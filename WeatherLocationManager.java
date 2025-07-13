package com.example.project.Activitis;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class WeatherLocationManager {

    private static final String TAG = "WeatherLocationManager";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private Context context;
    private Activity activity;
    private LocationManager locationManager;
    private LocationCallback callback;
    private boolean isLocationPermissionGranted = false;

    public interface LocationCallback {
        void onLocationFound(double latitude, double longitude);
        void onLocationError(String error);
        void onPermissionRequired();
    }

    public WeatherLocationManager(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        checkLocationPermission();
    }

    public void setLocationCallback(LocationCallback callback) {
        this.callback = callback;
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            isLocationPermissionGranted = false;
            Log.d(TAG, "Location permission not granted");
        } else {
            isLocationPermissionGranted = true;
            Log.d(TAG, "Location permission already granted");
        }
    }

    public void requestLocationPermission() {
        Log.d(TAG, "Requesting location permission");
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    public void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isLocationPermissionGranted = true;
                Toast.makeText(context, "GPS permission allowed", Toast.LENGTH_SHORT).show();
            } else {
                isLocationPermissionGranted = false;
                Toast.makeText(context, "GPS permission is required for positioning", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void getCurrentLocation() {
        Log.d(TAG, "getCurrentLocation called, permission granted: " + isLocationPermissionGranted);

        if (!isLocationPermissionGranted) {
            Log.d(TAG, "Permission not granted, requesting...");
            if (callback != null) {
                callback.onPermissionRequired();
            }
            return;
        }

        if (locationManager == null) {
            Log.e(TAG, "LocationManager is null");
            if (callback != null) {
                callback.onLocationError("Unable to obtain location service");
            }
            return;
        }

        try {
            // Do not display the "locating" toast to reduce distractions
            Log.d(TAG, "Starting location request");

            // Check if GPS is available
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Log.d(TAG, "GPS enabled: " + gpsEnabled + ", Network enabled: " + networkEnabled);

            if (gpsEnabled) {
                Log.d(TAG, "Using GPS provider");
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
            }
            else if (networkEnabled) {
                Log.d(TAG, "Using Network provider");
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);
            }
            else {
                Log.w(TAG, "No location providers available");
                if (callback != null) {
                    callback.onLocationError("Please turn on GPS or network positioning");
                }
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error", e);
            if (callback != null) {
                callback.onLocationError("Location permission error");
            }
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            Log.d(TAG, "Got location: " + latitude + ", " + longitude);

            if (callback != null) {
                callback.onLocationFound(latitude, longitude);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };

    public void stopLocationUpdates() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                Log.e(TAG, "Error removing location updates", e);
            }
        }
    }

    public boolean isPermissionGranted() {
        return isLocationPermissionGranted;
    }
}
