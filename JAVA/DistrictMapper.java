package com.example.project.Activitis;

import android.util.Log;

public class DistrictMapper {

    private static final String TAG = "DistrictMapper";

    // District data structure
    public static class DistrictLocation {
        public double latitude;
        public double longitude;
//        public String districtCode;
        public String districtName;


        public DistrictLocation(double lat, double lon, String name) {
            this.latitude = lat;
            this.longitude = lon;
            this.districtName = name;
        }
    }

    // Coordinates of the 18 districts of Hong Kong
    private static final DistrictLocation[] DISTRICT_LOCATIONS = {
            // Hong Kong Island
            new DistrictLocation(22.2866, 114.1547, "Central & Western District"),
            new DistrictLocation(22.2783, 114.1747, "Wan Chai"),
            new DistrictLocation(22.2866, 114.2147, "Eastern District"),
            new DistrictLocation(22.2483, 114.1747, "Southern District"),

            // Kowloon
            new DistrictLocation(22.3066, 114.1697, "Yau Tsim Mong"),
            new DistrictLocation(22.3316, 114.1597, "Sham Shui Po"),
            new DistrictLocation(22.3166, 114.1897, "Kowloon City"),
            new DistrictLocation(22.3366, 114.1997, "Wong Tai Sin"),
            new DistrictLocation(22.3116, 114.2247, "Kwun Tong"),

            // New Territories
            new DistrictLocation(22.3716, 114.1147, "Tsuen Wan"),
            new DistrictLocation(22.3916, 113.9747, "Tuen Mun"),
            new DistrictLocation(22.4466, 114.0347, "Yuen Long"),
            new DistrictLocation(22.4966, 114.1297, "North District"),
            new DistrictLocation(22.4516, 114.1647, "Tai Po"),
            new DistrictLocation(22.3816, 114.1947, "Sha Tin"),
            new DistrictLocation(22.3816, 114.2647, "Sai Kung"),
            new DistrictLocation(22.3566, 114.1347, "Kwai Tsing"),
            new DistrictLocation(22.2666, 113.9447, "Islands District")
    };

    /**
     * Find the nearest 18 districts of Hong Kong based on longitude and latitude
     * @param latitude
     * @param longitude
     * @return the nearest district data, if not found, return null
     */
    public static DistrictLocation findNearestDistrict(double latitude, double longitude) {
        Log.d(TAG, "Finding nearest district for: " + latitude + ", " + longitude);

        double minDistance = Double.MAX_VALUE;
        DistrictLocation nearestDistrict = null;

        for (DistrictLocation district : DISTRICT_LOCATIONS) {
            // Calculate distance (use Haversine formula to calculate the distance between two points on the earth surface)
            double distance = calculateDistance(latitude, longitude,
                    district.latitude, district.longitude);

            Log.d(TAG, "Distance to " + district.districtName + ": " + distance + " km");

            if (distance < minDistance) {
                minDistance = distance;
                nearestDistrict = district;
            }
        }

        if (nearestDistrict != null) {
            Log.d(TAG, "Nearest district: " + nearestDistrict.districtName +
                    " (Distance: " + minDistance + " km)");
        }

        return nearestDistrict;
    }

    /**
     * Calculate the distance between two points (in kilometers) using the Haversine formula
     */
    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS = 6371.0; // Earth radius (km)

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

/**
 * Get district information based on district code
 */
// public static DistrictLocation getDistrictByCode(String districtCode) {
// for (DistrictLocation district : DISTRICT_LOCATIONS) {
// if (district.districtCode.equals(districtCode)) {
// return district;
// }
// }
// return null;
// }

/**
 * Get a list of all districts
 */
// public static DistrictLocation[] getAllDistricts() {
// return DISTRICT_LOCATIONS.clone();
// }
}