package com.perception.persep.math_helpers;

import com.firebase.geofire.GeoLocation;
import com.google.firebase.firestore.GeoPoint;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class GeoLocationHelper
{
    private static final double PI = 3.14159265358979323851;
    private static final double EARTH_RADIUS = 6371e3;

    public static double Distance(double lat1, double lon1, double lat2, double lon2)
    {
        double lat1Radian = TO_RADIANS(lat1);
        double lat2Radian = TO_RADIANS(lat2);
        double latDelta = TO_RADIANS(lat2 - lat1) / 2;
        double lonDelta = TO_RADIANS(lon2 - lon1) / 2;

        double a = Math.sin(latDelta) * Math.sin(latDelta) + Math.cos(lat1Radian) * Math.cos(lat2Radian) * Math.sin(lonDelta) * Math.sin(lonDelta);
        return EARTH_RADIUS * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 2;
    }

    public static double Heading(double lat1, double lon1, double lat2, double lon2)
    {
        double y = Math.sin(lon2 - lon1) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);
        return Math.atan2(y, x);
    }

    public static Vector3D ConvertToCartesian(double lat, double lon)
    {
        lat = TO_RADIANS(lat);
        lon = TO_RADIANS(lon);
        double x = EARTH_RADIUS * Math.cos(lat) * Math.cos(lon);
        double y = EARTH_RADIUS * Math.cos(lat) * Math.sin(lon);
        double z = EARTH_RADIUS * Math.sin(lat);
        return new Vector3D(x, y, z);
    }

    private static double TO_RADIANS(double x)
    {
        return x * PI / 180;
    }

    private static double TO_DEGREES(double x)
    {
        return x * 180 / PI;
    }
}
