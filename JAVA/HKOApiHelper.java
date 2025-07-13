package com.example.project.Activitis;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HKOApiHelper {

    private static final String TAG = "HKOApiHelper";
    private static OkHttpClient client;

    static {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    // ==================== MainActivity Related ====================

    // MainActivity Callback interface
    public interface MainActivityCallback {
        void onDataReceived(ArrayList<DistrictData> districts, String generalWeather, String hkoHumidity, int weatherIcon, String highLowTemp);
        void onError(String error);
    }

    // Simplified district data model
    public static class DistrictData {
        public String districtName;
        public double temperature;
        public double rainfallMax;
        public boolean hasTemperature;
        public boolean hasRainfall;

        public DistrictData(String districtName) {
            this.districtName = districtName;
            this.temperature = 0.0;
            this.rainfallMax = 0.0;
            this.hasTemperature = false;
            this.hasRainfall = false;
        }

        public String getDisplayName() {
            return districtName;
        }

        public String getFormattedRainfall() {
            if (hasRainfall) {
                return String.format("%.0f mm", rainfallMax);
            }
            return "-- mm";
        }

        public String getFormattedTemperature() {
            if (hasTemperature) {
                return String.format("%.0f°", temperature);
            }
            return "--°";
        }
    }

    // MainActivity main method
    public static void getMainActivityData(MainActivityCallback callback) {
        getRealTimeData(callback);
    }

    // Get real time data (rhrread) Real-time temperature, humidity, rainfall
    private static void getRealTimeData(MainActivityCallback callback) {
        String url = "https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=rhrread&lang=en";
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get rhrread data", e);
                runOnMainThread(() -> callback.onError("Unable to obtain real-time data: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnMainThread(() -> callback.onError("Real-time data API failed: " + response.code()));
                    return;
                }

                String rhrreadData = response.body().string();
                Log.d(TAG, "Got rhrread data, now getting general weather");
                getGeneralWeather(rhrreadData, callback);
            }
        });
    }

    // Get weather overview (flw) General weather conditions and forecasts
    private static void getGeneralWeather(String rhrreadData, MainActivityCallback callback) {
        String url = "https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=flw&lang=en";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get flw data", e);
                parseMainActivityData(rhrreadData, null, callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.w(TAG, "FLW API failed with code: " + response.code());
                    parseMainActivityData(rhrreadData, null, callback);
                    return;
                }

                String flwData = response.body().string();
                Log.d(TAG, "Got flw data, now parsing");
                parseMainActivityData(rhrreadData, flwData, callback);
            }
        });
    }

    // Parsing MainActivity data
    private static void parseMainActivityData(String rhrreadJson, String flwJson, MainActivityCallback callback) {
        try {
            Log.d(TAG, "Starting to parse MainActivity data");

            JSONObject rhrread = new JSONObject(rhrreadJson);
            JSONObject flw = flwJson != null ? new JSONObject(flwJson) : null;

            ArrayList<DistrictData> districts = initializeDistricts();
            Map<String, DistrictData> districtMap = createDistrictMap(districts);

            // Parsing temp data
            parseTemperatureData(rhrread, districtMap);

            // Parsing rainfall data
            parseRainfallData(rhrread, districtMap);

            // Calculate the highest and lowest temperatures
            double[] highLowTemp = calculateHighLowTemperature(districts);
            double highTemp = highLowTemp[0];
            double lowTemp = highLowTemp[1];

            // Get the humidity of the observatory
            final String hkoHumidity = getHKOHumidity(rhrread);

            // Get the weather icon code
            final int weatherIcon = getWeatherIcon(rhrread);

            // Get weather overview
            final String generalWeather;
            if (flw != null && flw.has("generalSituation")) {
                generalWeather = flw.getString("generalSituation");
                Log.d(TAG, "Got generalSituation: " + generalWeather.substring(0, Math.min(50, generalWeather.length())) + "...");
            } else {
                generalWeather = "Loading...";
                Log.w(TAG, "No generalSituation found");
            }

            // Create callback data including high and low temp
            final String highLowTempString = String.format("L:%.0f° H:%.0f°", lowTemp, highTemp);

            Log.d(TAG, "Successfully parsed all data. High: " + highTemp + "°C, Low: " + lowTemp + "°C");
            runOnMainThread(() -> callback.onDataReceived(districts, generalWeather, hkoHumidity, weatherIcon, highLowTempString));

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing MainActivity data", e);
            runOnMainThread(() -> callback.onError("Failed to parse data: " + e.getMessage()));
        }
    }

    // Parsing temp data
    private static void parseTemperatureData(JSONObject rhrread, Map<String, DistrictData> districtMap) throws JSONException {
        if (!rhrread.has("temperature")) {
            Log.w(TAG, "No temperature data found");
            return;
        }

        JSONObject tempObj = rhrread.getJSONObject("temperature");
        if (!tempObj.has("data")) {
            Log.w(TAG, "No temperature data array found");
            return;
        }

        JSONArray tempArray = tempObj.getJSONArray("data");
        Log.d(TAG, "Found " + tempArray.length() + " temperature records");

        for (int i = 0; i < tempArray.length(); i++) {
            JSONObject tempItem = tempArray.getJSONObject(i);
            String place = tempItem.optString("place", "");
            double temperature = tempItem.optDouble("value", 0.0);

            // Try direct regional correspondence first
            DistrictData district = findDistrictByPlace(place, districtMap);

            // If cant find the direct match, use the weather station to match
            if (district == null) {
                district = findDistrictByStation(place, districtMap);
            }

            if (district != null) {
                district.temperature = temperature;
                district.hasTemperature = true;
                Log.d(TAG, "Temp: " + place + " → " + district.districtName + " = " + temperature + "°C");
            } else {
                Log.d(TAG, "Cannot map temperature place: " + place);
            }
        }
    }

    // Analyzing rainfall data
    private static void parseRainfallData(JSONObject rhrread, Map<String, DistrictData> districtMap) throws JSONException {
        if (!rhrread.has("rainfall")) {
            Log.w(TAG, "No rainfall data found");
            return;
        }

        JSONObject rainfallObj = rhrread.getJSONObject("rainfall");
        if (!rainfallObj.has("data")) {
            Log.w(TAG, "No rainfall data array found");
            return;
        }

        JSONArray rainfallArray = rainfallObj.getJSONArray("data");
        Log.d(TAG, "Found " + rainfallArray.length() + " rainfall records");

        for (int i = 0; i < rainfallArray.length(); i++) {
            JSONObject rainfallItem = rainfallArray.getJSONObject(i);
            String place = rainfallItem.optString("place", "");
            double maxRainfall = rainfallItem.optDouble("max", 0.0);

            DistrictData district = findDistrictByPlace(place, districtMap);
            if (district != null) {
                district.rainfallMax = maxRainfall;
                district.hasRainfall = true;
                Log.d(TAG, "雨量: " + place + " = " + district.rainfallMax + "mm");
            } else {
                Log.d(TAG, "Cannot map rainfall place: " + place);
            }
        }
    }

    // Get the humidity of the observatory
    private static String getHKOHumidity(JSONObject rhrread) {
        try {
            if (rhrread.has("humidity")) {
                JSONObject humidityObj = rhrread.getJSONObject("humidity");
                if (humidityObj.has("data")) {
                    JSONArray humidityArray = humidityObj.getJSONArray("data");
                    for (int i = 0; i < humidityArray.length(); i++) {
                        JSONObject humidityItem = humidityArray.getJSONObject(i);
                        String place = humidityItem.optString("place", "");
                        if ("Hong Kong Observatory".equals(place)) {
                            double humidity = humidityItem.optDouble("value", 0.0);
                            Log.d(TAG, "HKO humidity: " + humidity + "%");
                            return String.format("%.0f%%", humidity);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Failed to obtain the observatory humidity", e);
        }
        Log.w(TAG, "No HKO humidity found, using default");
        return "--%";
    }

    // Get the weather icon code
    private static int getWeatherIcon(JSONObject rhrread) {
        try {
            if (rhrread.has("icon")) {
                JSONArray iconArray = rhrread.getJSONArray("icon");
                if (iconArray.length() > 0) {
                    int iconCode = iconArray.optInt(0, 50);
                    Log.d(TAG, "Weather icon code from rhrread: " + iconCode);
                    return iconCode;
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Failed to get weather icon", e);
        }

        Log.w(TAG, "No weather icon found, using default (50)");
        return 50;
    }

    // Calculate the highest and lowest temperatures
    private static double[] calculateHighLowTemperature(ArrayList<DistrictData> districts) {
        double highTemp = Double.MIN_VALUE;
        double lowTemp = Double.MAX_VALUE;
        boolean hasValidTemp = false;

        for (DistrictData district : districts) {
            if (district.hasTemperature) {
                highTemp = Math.max(highTemp, district.temperature);
                lowTemp = Math.min(lowTemp, district.temperature);
                hasValidTemp = true;
                Log.d(TAG, district.districtName + ": " + district.temperature + "°C");
            }
        }

        if (!hasValidTemp) {
            Log.w(TAG, "No valid temperature data found, using default values");
            return new double[]{0.0, 0.0};
        }

        Log.d(TAG, "Temperature range: " + lowTemp + "°C to " + highTemp + "°C");
        return new double[]{highTemp, lowTemp};
    }

    // Find the corresponding area according to the weather station name
    private static DistrictData findDistrictByStation(String stationName, Map<String, DistrictData> districtMap) {
        Map<String, String> stationToDistrict = new HashMap<>();

        // Hong Kong Island
        stationToDistrict.put("Hong Kong Observatory", "Central & Western District");
        stationToDistrict.put("Hong Kong Park", "Central & Western District");
        stationToDistrict.put("Happy Valley", "Wan Chai");
        stationToDistrict.put("Wong Chuk Hang", "Southern District");
        stationToDistrict.put("Stanley", "Southern District");

        // Kowloon
        stationToDistrict.put("King's Park", "Yau Tsim Mong");
        stationToDistrict.put("Sham Shui Po", "Sham Shui Po");
        stationToDistrict.put("Kowloon City", "Kowloon City");
        stationToDistrict.put("Kai Tak Runway Park", "Kowloon City");
        stationToDistrict.put("Wong Tai Sin", "Wong Tai Sin");
        stationToDistrict.put("Kwun Tong", "Kwun Tong");
        stationToDistrict.put("Tseung Kwan O", "Sai Kung");

        // New Territories
        stationToDistrict.put("Tsuen Wan Ho Koon", "Tsuen Wan");
        stationToDistrict.put("Tsuen Wan Shing Mun Valley", "Tsuen Wan");
        stationToDistrict.put("Tuen Mun", "Tuen Mun");
        stationToDistrict.put("Yuen Long Park", "Yuen Long");
        stationToDistrict.put("Lau Fau Shan", "Yuen Long");
        stationToDistrict.put("Ta Kwu Ling", "North District");
        stationToDistrict.put("Tai Po", "Tai Po");
        stationToDistrict.put("Tai Mei Tuk", "Tai Po");
        stationToDistrict.put("Sha Tin", "Sha Tin");
        stationToDistrict.put("Sai Kung", "Sai Kung");
        stationToDistrict.put("Tsing Yi", "Kwai Tsing");
        stationToDistrict.put("Shek Kong", "Yuen Long");
        stationToDistrict.put("Chek Lap Kok", "Islands District");
        stationToDistrict.put("Cheung Chau", "Islands District");

        String districtName = stationToDistrict.get(stationName);
        if (districtName != null) {
            return districtMap.get(districtName);
        }

        return null;
    }

    // ==================== SecondActivity Related====================

    public static class SevenDayForecast {
        public String date;
        public String dayOfWeek;
        public String weather;
        public int maxTemp;
        public int minTemp;
        public String windInfo;
        public int maxHumidity;
        public int minHumidity;
        public int iconCode;
        public String rainProbability;

        public SevenDayForecast() {}
    }

    public interface SevenDayCallback {
        void onSevenDayDataReceived(ArrayList<SevenDayForecast> forecasts);
        void onError(String error);
    }

    // 7-day detailed forecasts
    public static void getSevenDayForecast(SevenDayCallback callback) {
        String forecastUrl = "https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=fnd&lang=en";

        Request request = new Request.Builder()
                .url(forecastUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch seven day forecast", e);
                runOnMainThread(() -> callback.onError("Unable to obtain seven-day forecast data: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnMainThread(() -> callback.onError("API request failed: " + response.code()));
                    return;
                }

                String responseBody = response.body().string();
                parseSevenDayForecast(responseBody, callback);
            }
        });
    }

    private static void parseSevenDayForecast(String jsonData, SevenDayCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            ArrayList<SevenDayForecast> forecasts = new ArrayList<>();

            if (jsonObject.has("weatherForecast")) {
                JSONArray forecastArray = jsonObject.getJSONArray("weatherForecast");

                for (int i = 0; i < Math.min(7, forecastArray.length()); i++) {
                    JSONObject dayForecast = forecastArray.getJSONObject(i);

                    SevenDayForecast forecast = new SevenDayForecast();

                    forecast.date = dayForecast.optString("forecastDate", "");
                    forecast.dayOfWeek = dayForecast.optString("week", "");
                    forecast.weather = dayForecast.optString("forecastWeather", "");

                    if (dayForecast.has("forecastMaxtemp")) {
                        forecast.maxTemp = dayForecast.getJSONObject("forecastMaxtemp").optInt("value", 0);
                    }
                    if (dayForecast.has("forecastMintemp")) {
                        forecast.minTemp = dayForecast.getJSONObject("forecastMintemp").optInt("value", 0);
                    }

                    forecast.windInfo = dayForecast.optString("forecastWind", "");

                    if (dayForecast.has("forecastMaxrh")) {
                        forecast.maxHumidity = dayForecast.getJSONObject("forecastMaxrh").optInt("value", 0);
                    }
                    if (dayForecast.has("forecastMinrh")) {
                        forecast.minHumidity = dayForecast.getJSONObject("forecastMinrh").optInt("value", 0);
                    }

                    forecast.iconCode = dayForecast.optInt("ForecastIcon", 50);
                    forecast.rainProbability = dayForecast.optString("PSR", "高");

                    forecasts.add(forecast);
                }
            }

            runOnMainThread(() -> callback.onSevenDayDataReceived(forecasts));

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing seven day forecast", e);
            runOnMainThread(() -> callback.onError("Failed to parse the seven day forecast data: " + e.getMessage()));
        }
    }

    // ==================== GeneralSituation Related ====================

    public interface GeneralSituationCallback {
        void onGeneralSituationReceived(String generalSituation);
        void onError(String error);
    }

    public static void getGeneralSituation(GeneralSituationCallback callback) {
        String url = "https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=flw&lang=en";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get general situation", e);
                runOnMainThread(() -> callback.onError("Failed to get general situation: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnMainThread(() -> callback.onError("天氣概況API失敗: " + response.code()));
                    return;
                }

                String responseBody = response.body().string();
                parseGeneralSituation(responseBody, callback);
            }
        });
    }

    private static void parseGeneralSituation(String jsonData, GeneralSituationCallback callback) {
        try {
            JSONObject flwData = new JSONObject(jsonData);
            String generalSituation = "";

            if (flwData.has("generalSituation")) {
                generalSituation = flwData.getString("generalSituation");
                Log.d(TAG, "Got generalSituation from API");
            } else {
                Log.w(TAG, "No generalSituation found in API response");
                generalSituation = "No weather information available";
            }

            final String finalGeneralSituation = generalSituation;
            runOnMainThread(() -> callback.onGeneralSituationReceived(finalGeneralSituation));

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing general situation", e);
            runOnMainThread(() -> callback.onError("Failed to parse weather overview: " + e.getMessage()));
        }
    }

    // ==================== Tools ====================

    private static ArrayList<DistrictData> initializeDistricts() {
        ArrayList<DistrictData> districts = new ArrayList<>();

        districts.add(new DistrictData("Central & Western District"));
        districts.add(new DistrictData("Wan Chai"));
        districts.add(new DistrictData( "Eastern District"));
        districts.add(new DistrictData("Southern District"));
        districts.add(new DistrictData("Yau Tsim Mong"));
        districts.add(new DistrictData("Sham Shui Po"));
        districts.add(new DistrictData("Kowloon City"));
        districts.add(new DistrictData("Wong Tai Sin"));
        districts.add(new DistrictData("Kwun Tong"));
        districts.add(new DistrictData("Tsuen Wan"));
        districts.add(new DistrictData("Tuen Mun"));
        districts.add(new DistrictData("Yuen Long"));
        districts.add(new DistrictData("North District"));
        districts.add(new DistrictData("Tai Po"));
        districts.add(new DistrictData("Sha Tin"));
        districts.add(new DistrictData("Sai Kung"));
        districts.add(new DistrictData("Kwai Tsing"));
        districts.add(new DistrictData("Islands District"));

        return districts;
    }

    private static Map<String, DistrictData> createDistrictMap(ArrayList<DistrictData> districts) {
        Map<String, DistrictData> map = new HashMap<>();
        for (DistrictData district : districts) {
            map.put(district.districtName, district);
        }
        return map;
    }

    private static DistrictData findDistrictByPlace(String place, Map<String, DistrictData> districtMap) {
        DistrictData district = districtMap.get(place);
        if (district != null) {
            return district;
        }
        return null;
    }

    public static String getHKOOfficialIconUrl(int iconCode) {
        if (iconCode > 0) {
            return "https://www.weather.gov.hk/images/HKOWxIconOutline/pic" + iconCode + ".png";
        }
        return "https://www.hko.gov.hk/images/wxicon/pic50.png";
    }

    private static void runOnMainThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}