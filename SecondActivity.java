package com.example.project.Activitis;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.project.Activitis.SecondDomain;
import com.example.project.Activitis.HKOApiHelper;
import com.example.project.R;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SecondActivity extends AppCompatActivity {
    private static final String TAG = "SecondActivity";
    private TextView tmrTempTextView, tmrWeatherTextView, tmrRainfallPercentTextView, tmrWindSpeedTextView, tmrHumidityTextView;
    private ImageView tmrWeatherIcon;
    private ListView next7DayForecastList;
    private SecondAdapter adapter;

    // Storing weather profiles
    private String generalWeatherCondition = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        initViews();
        loadWeatherData();
        setupBackButton();
    }

    private void initViews() {
        tmrTempTextView = findViewById(R.id.tmrTemp);
        tmrWeatherTextView = findViewById(R.id.tmrWeather);
        tmrRainfallPercentTextView = findViewById(R.id.tmrRainfallPercent);
        tmrWindSpeedTextView = findViewById(R.id.nowTemp); // Wind speed
        tmrHumidityTextView = findViewById(R.id.textViewHuman); // Humidity
        tmrWeatherIcon = findViewById(R.id.tmrWeatherIcon);
        next7DayForecastList = findViewById(R.id.next7DayForecastList);

        // tmrWeatherTextView , Now only tomorrow's weather is displayed, no click event is required
        // If want to display the generalSituation, click on the entire Tomorrow card.
        if (tmrWeatherTextView != null) {
            tmrWeatherTextView.setOnClickListener(v -> showFullWeatherDescription());
            tmrWeatherTextView.setMaxLines(2);
            tmrWeatherTextView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        }
    }

    private void showFullWeatherDescription() {
        if (generalWeatherCondition.isEmpty()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Weather Situation");
        builder.setMessage(generalWeatherCondition);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void loadWeatherData() {
        // Load generalSituation first
        loadGeneralWeatherSituation();

        // Then load the seven-day forecast
        HKOApiHelper.getSevenDayForecast(new HKOApiHelper.SevenDayCallback() {
            @Override
            public void onSevenDayDataReceived(ArrayList<HKOApiHelper.SevenDayForecast> forecasts) {
//                if (forecasts == null || forecasts.isEmpty()) {
//                    runOnUiThread(() -> setTomorrowWeather(forecasts.get(0)));
//                    return;
//                }

                // Index 0 for tmr
                if (forecasts.size() > 0) {
                    runOnUiThread(() -> setTomorrowWeather(forecasts.get(0)));
                }

                // Seven-day list (starting from index 1 - starting from the day after tomorrow)
                if (forecasts.size() > 1) {
                    runOnUiThread(() -> updateSevenDayList(forecasts, 1));
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load seven day forecast: " + error);
                runOnUiThread(SecondActivity.this::loadDefaultData);
            }
        });
    }

    // Load weather overview (Use HKOApiHelper for unified management)
    private void loadGeneralWeatherSituation() {
        HKOApiHelper.getGeneralSituation(new HKOApiHelper.GeneralSituationCallback() {
            @Override
            public void onGeneralSituationReceived(String generalSituation) {
                generalWeatherCondition = generalSituation;
                Log.d(TAG, "Loaded general weather situation via HKOApiHelper");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load general weather situation: " + error);
                generalWeatherCondition = "Failed to load weather overview";
            }
        });
    }

    private void setupBackButton() {
        ConstraintLayout backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }
    }

    private void setTomorrowWeather(HKOApiHelper.SevenDayForecast forecast) {
        runOnUiThread(() -> {
            // Display temperature range (min.-max.)
            if (tmrTempTextView != null) {
                String tempRange = forecast.minTemp + "-" + forecast.maxTemp + "°";
                tmrTempTextView.setText(tempRange);
                tmrTempTextView.setVisibility(View.VISIBLE);
            }

            if (tmrWeatherTextView != null) {
                tmrWeatherTextView.setText(forecast.weather); // Tomorrow  weather description
                tmrWeatherTextView.setMaxLines(2);
                tmrWeatherTextView.setEllipsize(android.text.TextUtils.TruncateAt.END);
                tmrWeatherTextView.setVisibility(View.VISIBLE);
            }

            if (tmrRainfallPercentTextView != null) {
                tmrRainfallPercentTextView.setText(forecast.rainProbability);
                tmrRainfallPercentTextView.setVisibility(View.VISIBLE);
            }

            if (tmrWindSpeedTextView != null) {
                // Remove the last . of wind speed information
                String windInfo = forecast.windInfo;
                if (windInfo != null && windInfo.endsWith(".")) {
                    windInfo = windInfo.substring(0, windInfo.length() - 1);
                }
                tmrWindSpeedTextView.setText(windInfo);
                tmrWindSpeedTextView.setVisibility(View.VISIBLE);
            }

            // Display humidity range (minimum - maximum)
            if (tmrHumidityTextView != null) {
                String humidityRange = forecast.minHumidity + "-" + forecast.maxHumidity + "%";
                tmrHumidityTextView.setText(humidityRange);
                tmrHumidityTextView.setVisibility(View.VISIBLE);
            }

            // Update tomorrow weather icon
            if (tmrWeatherIcon != null) {
                String iconUrl = HKOApiHelper.getHKOOfficialIconUrl(forecast.iconCode);
                Log.d(TAG, "Loading tomorrow weather icon: " + iconUrl);

                Picasso.get()
                        .load(iconUrl)
                        .resize(80, 80)
                        .centerInside()
                        .into(tmrWeatherIcon);

                tmrWeatherIcon.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateSevenDayList(ArrayList<HKOApiHelper.SevenDayForecast> forecasts, int startIndex) {
        ArrayList<SecondDomain> items = new ArrayList<>();

        // Starting from startIndex, display up to 7 items
        int maxItems = Math.min(7, forecasts.size() - startIndex);

        for (int i = startIndex; i < startIndex + maxItems && i < forecasts.size(); i++) {
            HKOApiHelper.SevenDayForecast forecast = forecasts.get(i);

            // Convert the date format returned by the API
            String dayName = convertDateToDisplay(forecast.date);
            String iconUrl = HKOApiHelper.getHKOOfficialIconUrl(forecast.iconCode);

            items.add(new SecondDomain(dayName, forecast.minTemp, iconUrl, forecast.weather, forecast.maxTemp));
        }

        runOnUiThread(() -> {
            if (items.size() > 0) {
                adapter = new SecondAdapter(this, items);
                next7DayForecastList.setAdapter(adapter);
            } else {
                loadDefaultData();
            }
        });
    }

    //Convert data format
    private String convertDateToDisplay(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "N/A";
        }


        try {
            if (dateStr.length() == 8) {
                String year = dateStr.substring(0, 4);
                String month = dateStr.substring(4, 6);
                String day = dateStr.substring(6, 8);

                // Remove zeros and format as "dd/M"
                int dayInt = Integer.parseInt(day);
                int monthInt = Integer.parseInt(month);

                String result = dayInt + "/" + monthInt;
                Log.d(TAG, "Converted date: " + dateStr + " -> " + result);
                return result;
            } else {
                Log.w(TAG, "Unexpected date format: " + dateStr);
                return "N/A";
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse date: " + dateStr, e);
            return "N/A";
        }
    }

    private void loadDefaultData() {
        runOnUiThread(() -> {
            if (tmrTempTextView != null) {
                tmrTempTextView.setText("--°");
                tmrTempTextView.setVisibility(View.VISIBLE);
            }

            if (tmrWeatherTextView != null) {
                tmrWeatherTextView.setText("Loading...");
                tmrWeatherTextView.setVisibility(View.VISIBLE);
            }

            if (tmrRainfallPercentTextView != null) {
                tmrRainfallPercentTextView.setText("--");
                tmrRainfallPercentTextView.setVisibility(View.VISIBLE);
            }

            if (tmrWindSpeedTextView != null) {
                tmrWindSpeedTextView.setText("--");
                tmrWindSpeedTextView.setVisibility(View.VISIBLE);
            }

            if (tmrHumidityTextView != null) {
                tmrHumidityTextView.setText("--%");
                tmrHumidityTextView.setVisibility(View.VISIBLE);
            }

            if (next7DayForecastList != null) {
                next7DayForecastList.setAdapter(null);
            }
        });
    }
}