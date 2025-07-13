package com.example.project.Activitis;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.project.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TextView selectDistrictTextView;
    private TextView temperatureTextView;
    private TextView humidityTextView;
    private TextView locationTextView;
    private TextView weatherConditionTextView;
    private TextView rainfallTextView;
    private TextView dateTimeTextView;
    private TextView highLowTempTextView;
    private ImageView weatherIconImageView;

    // Data
    private ArrayList<HKOApiHelper.DistrictData> districtList;
    private HKOApiHelper.DistrictData selectedDistrict;
    private String generalWeatherCondition = "Loading...";
    private String hkoHumidity = "--%";
    private int currentWeatherIcon = 50;
    private String highLowTempString = "H:-- L:--"; // High and low temp
    private Dialog dialog;

    // Time update
    private Handler timeHandler;
    private Runnable timeUpdateRunnable;

    // GPS
    private WeatherLocationManager locationManager;
    private boolean isFirstLoad = true; // Tracking it is first loaded
    private boolean weatherDataLoaded = false; // Tracks whether weather data has been loaded

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupTimeUpdater();
        initLocationServices(); // Initialize GPS
        loadWeatherData(); // Initialize weather data
        setupClickListeners();
    }

    private void initViews() {
        selectDistrictTextView = findViewById(R.id.selectDistrict);
        temperatureTextView = findViewById(R.id.nowTemp);
        humidityTextView = findViewById(R.id.humidity);
        locationTextView = findViewById(R.id.locationTextView);
        weatherConditionTextView = findViewById(R.id.nowLocationWeatherTextView);
        rainfallTextView = findViewById(R.id.rainfallmax);
        dateTimeTextView = findViewById(R.id.dateAndTime);
        weatherIconImageView = findViewById(R.id.imageView);
        highLowTempTextView = findViewById(R.id.nowHighTempAndLowTemp);

        districtList = new ArrayList<>();

        if (weatherConditionTextView != null) {
            weatherConditionTextView.setOnClickListener(v -> showFullWeatherDescription());
            weatherConditionTextView.setMaxLines(2);
            weatherConditionTextView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        }
    }

    private void showFullWeatherDescription() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Weather Situation");
        builder.setMessage(generalWeatherCondition);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void setupTimeUpdater() {
        timeHandler = new Handler();
        timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateCurrentTime();
                timeHandler.postDelayed(this, 60000);
            }
        };

        updateCurrentTime();
        timeHandler.postDelayed(timeUpdateRunnable, 60000);
    }

    private void updateCurrentTime() {
        java.util.Calendar now = java.util.Calendar.getInstance();

        String dayOfWeek = getDayOfWeekShort(now.get(java.util.Calendar.DAY_OF_WEEK));
        int hour = now.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = now.get(java.util.Calendar.MINUTE);
        int day = now.get(java.util.Calendar.DAY_OF_MONTH);
        int month = now.get(java.util.Calendar.MONTH) + 1;

        String ampm = hour >= 12 ? "PM" : "AM";
        if (hour > 12) hour -= 12;
        if (hour == 0) hour = 12;

        String timeText = String.format("%s %s %d | %d:%02d%s",
                dayOfWeek, getMonthName(month), day, hour, minute, ampm);

        if (dateTimeTextView != null) {
            dateTimeTextView.setText(timeText);
        }
    }

    private void loadWeatherData() {
        if (selectDistrictTextView != null) {
            selectDistrictTextView.setText("》Loading...");
        }

        HKOApiHelper.getMainActivityData(new HKOApiHelper.MainActivityCallback() {
            @Override
            public void onDataReceived(ArrayList<HKOApiHelper.DistrictData> districts, String generalWeather, String humidity, int weatherIcon, String highLowTemp) {
                districtList.clear();
                districtList.addAll(districts);

                generalWeatherCondition = generalWeather;
                hkoHumidity = humidity;
                currentWeatherIcon = weatherIcon;
                highLowTempString = highLowTemp;
                weatherDataLoaded = true; // Mark weather data loaded

                if (selectDistrictTextView != null) {
                    selectDistrictTextView.setText("Select District");
                }

                updateGeneralInfo();
                updateWeatherIcon();


                // If it is the first time loading and GPS is ready, automatically start positioning
                if (isFirstLoad && locationManager != null && locationManager.isPermissionGranted()) {
                    startAutoGPS();
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (selectDistrictTextView != null) {
                        selectDistrictTextView.setText("Loading failed");
                    }
                    Toast.makeText(MainActivity.this,
                            "Loading failed: " + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // Add automatic GPS method
    private void startAutoGPS() {
        if (isFirstLoad && weatherDataLoaded) {
            Log.d("MainActivity", "Starting auto GPS location");
            Toast.makeText(this, "Starting auto GPS location...", Toast.LENGTH_SHORT).show();

            // Start positioning after a delay of 500ms so can see the prompt
            new Handler().postDelayed(() -> {
                if (locationManager != null) {
                    locationManager.getCurrentLocation();
                }
            }, 500);

            isFirstLoad = false; // Mark first load completed
        }
    }

    private void updateGeneralInfo() {
        if (weatherConditionTextView != null) {
            weatherConditionTextView.setText(generalWeatherCondition);
            weatherConditionTextView.setMaxLines(2);
            weatherConditionTextView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        }

        // Update humidity
        if (humidityTextView != null) {
            humidityTextView.setText(hkoHumidity);
        }

        // Update high and low temperatures
        if (highLowTempTextView != null) {
            highLowTempTextView.setText(highLowTempString);
        }
    }

    private void updateWeatherIcon() {
        if (weatherIconImageView == null) return;

        String iconUrl = HKOApiHelper.getHKOOfficialIconUrl(currentWeatherIcon);

        Picasso.get()
                .load(iconUrl)
                .resize(150, 150)
                .centerInside()
                .into(weatherIconImageView);
    }

    private void updateSelectedDistrictDisplay() {
        if (selectedDistrict == null) return;

        runOnUiThread(() -> {
            if (selectDistrictTextView != null) {
                selectDistrictTextView.setText(selectedDistrict.districtName);
            }
            if (locationTextView != null) {
                locationTextView.setText(selectedDistrict.districtName);
            }
            if (temperatureTextView != null) {
                temperatureTextView.setText(selectedDistrict.getFormattedTemperature());
            }
            if (rainfallTextView != null) {
                rainfallTextView.setText(selectedDistrict.getFormattedRainfall());
            }
        });
    }

    private void setupClickListeners() {
        // Select district
        if (selectDistrictTextView != null) {
            selectDistrictTextView.setOnClickListener(v -> showDistrictSelectionDialog());
        }

        // Next 7 Day butoon
        TextView next7dayBtn = findViewById(R.id.nextBtn);
        if (next7dayBtn != null) {
            next7dayBtn.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                startActivity(intent);
            });
        }

        // Fun Text button
        Button funTextButton = findViewById(R.id.funTextButton);
        if (funTextButton != null) {
            funTextButton.setOnClickListener(v -> showFunTextDialog());
        }

        //  Manual search button
        Button locateButton = findViewById(R.id.locateButton);
        if (locateButton != null) {
            locateButton.setOnClickListener(v -> {
                Toast.makeText(this, "Manual locating in progress...", Toast.LENGTH_SHORT).show();
                if (locationManager != null) {
                    locationManager.getCurrentLocation();
                }
            });
        }
    }

    private void showDistrictSelectionDialog() {
        dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.dialog_searchable_spinner);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(850, 1050);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

        EditText editText = dialog.findViewById(R.id.editTxt);
        ListView listView = dialog.findViewById(R.id.listView);

        ArrayList<String> displayList = new ArrayList<>();
        for (HKOApiHelper.DistrictData district : districtList) {
            displayList.add(district.getDisplayName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_list_item_1, displayList);

        if (listView != null) {
            listView.setAdapter(adapter);
        }

        if (editText != null) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {}

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    adapter.getFilter().filter(s);
                }
            });
        }

        if (listView != null) {
            listView.setOnItemClickListener((parent, view, position, id) -> {
                String selectedText = adapter.getItem(position);

                for (HKOApiHelper.DistrictData district : districtList) {
                    if (selectedText != null && selectedText.startsWith(district.getDisplayName())) {
                        selectedDistrict = district;
                        updateSelectedDistrictDisplay();
                        break;
                    }
                }
                dialog.dismiss();
            });
        }
    }

    private void showFunTextDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        ImageButton closeButton = dialogView.findViewById(R.id.close_button);
        Button shareButton = dialogView.findViewById(R.id.share_button);
        TextView dialogText = dialogView.findViewById(R.id.dialog_text);

        String funText = generateFunText();
        if (dialogText != null) {
            dialogText.setText(funText);
        }

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dialog.dismiss());
        }

        if (shareButton != null) {
            shareButton.setOnClickListener(v -> {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, funText);
                shareIntent.setType("text/plain");
                startActivity(Intent.createChooser(shareIntent, "Share Weather Tips"));
            });
        }

        dialog.show();
    }

    private String generateFunText() {
        if (selectedDistrict == null) {
            return "今日天氣唔錯，記得出街！ 😊";
        }

        String districtName = selectedDistrict.districtName;

        if (selectedDistrict.hasRainfall && selectedDistrict.rainfallMax > 5.0) {
            return String.format("%s過去有%.1fmm雨！出街記得帶遮啊！ ☔",
                    districtName, selectedDistrict.rainfallMax);
        } else if (selectedDistrict.hasRainfall && selectedDistrict.rainfallMax > 0.1) {
            return String.format("%s有少少雨(%.1fmm)，小心啲！ 🌦️",
                    districtName, selectedDistrict.rainfallMax);
        }

        if (selectedDistrict.hasTemperature) {
            double temp = selectedDistrict.temperature;
            if (temp > 30) {
                return String.format("%s今日好熱呀！%.0f度，記得多飲水！🌞💦", districtName, temp);
            } else if (temp < 15) {
                return String.format("%s今日凍過雪櫃！%.0f度，記得著多件衫！🧥❄️", districtName, temp);
            } else {
                return String.format("%s今日天氣幾好，%.0f度，啱啱好！😊🌤️", districtName, temp);
            }
        }

        return String.format("%s今日天氣唔錯！ 😊", districtName);
    }

    private String getDayOfWeekShort(int dayOfWeek) {
        switch (dayOfWeek) {
            case java.util.Calendar.SUNDAY: return "SUN";
            case java.util.Calendar.MONDAY: return "MON";
            case java.util.Calendar.TUESDAY: return "TUE";
            case java.util.Calendar.WEDNESDAY: return "WED";
            case java.util.Calendar.THURSDAY: return "THU";
            case java.util.Calendar.FRIDAY: return "FRI";
            case java.util.Calendar.SATURDAY: return "SAT";
            default: return "SAT";
        }
    }

    private String getMonthName(int month) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return months[month - 1];
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (timeHandler != null && timeUpdateRunnable != null) {
            timeHandler.removeCallbacks(timeUpdateRunnable);
        }

        if (locationManager != null) {
            locationManager.stopLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (locationManager != null) {
            locationManager.handlePermissionResult(requestCode, permissions, grantResults);

            // If permission is granted and it is the first time loading, start automatic GPS
            if (locationManager.isPermissionGranted() && isFirstLoad && weatherDataLoaded) {
                startAutoGPS();
            }
        }
    }

    // GPS Function
    private void initLocationServices() {
        Log.d("MainActivity", "Initializing location services");
        locationManager = new WeatherLocationManager(this, this);
        locationManager.setLocationCallback(new WeatherLocationManager.LocationCallback() {
            @Override
            public void onLocationFound(double latitude, double longitude) {
                Log.d("MainActivity", "Location found: " + latitude + ", " + longitude);
                handleLocationResult(latitude, longitude);
            }

            @Override
            public void onLocationError(String error) {
                Log.e("MainActivity", "Location error: " + error);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionRequired() {
                Log.d("MainActivity", "Permission required, requesting...");
                locationManager.requestLocationPermission();
            }
        });

        // Log if permission has been granted
        if (locationManager.isPermissionGranted()) {
            Log.d("MainActivity", "GPS permission already granted");
        }
    }

    private void handleLocationResult(double latitude, double longitude) {
        DistrictMapper.DistrictLocation nearestDistrict =
                DistrictMapper.findNearestDistrict(latitude, longitude);

        if (nearestDistrict != null) {
            selectDistrictByName(nearestDistrict.districtName);

            // Display a toast message containing latitude and longitude
            String toastMessage = String.format("Successfully located to %s (%.4f, %.4f)",
                    nearestDistrict.districtName, latitude, longitude);
            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();

            Log.d("MainActivity", "Successfully located to: " + nearestDistrict.districtName +
                    " at coordinates: " + latitude + ", " + longitude);
        } else {
            Toast.makeText(this,
                    String.format("Locating success (%.4f, %.4f) But the district cannot be determined", latitude, longitude),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void selectDistrictByName(String districtName) {
        for (HKOApiHelper.DistrictData district : districtList) {
            if (district.districtName.equals(districtName)) {
                selectedDistrict = district;
                updateSelectedDistrictDisplay();
                break;
            }
        }
    }
}