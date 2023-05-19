package com.perception.persep.data_gatherer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;

import mad.location.manager.lib.Interfaces.ILogger;
import mad.location.manager.lib.Interfaces.LocationServiceInterface;
import mad.location.manager.lib.Services.KalmanLocationService;
import mad.location.manager.lib.Services.ServicesHelper;
import mad.location.manager.lib.Services.Settings;

public class GPSSensor implements LocationServiceInterface, ILogger
{
    private static final String TAG = "GPSSensor";

    @Override
    public void log2file(String format, Object... args)
    {
//        Log.d("Debug", String.format(format, args));
    }

    public interface Listener
    {
        void onLocationChanged(double lat, double lon);
    }

    private final ArrayList<Listener> m_Listeners = new ArrayList<>();
    private Context m_Context;

    public static double ACCELEROMETER_DEFAULT_DEVIATION = 0.1;     //0.1
    public static final int SENSOR_POSITION_MIN_TIME = 0;         //500
    public static final int GPS_MIN_TIME = 1_000;                    //2000
    public static final int GPS_MIN_DISTANCE = 0;                   //0
    public static final double SENSOR_DEFAULT_FREQ_HZ = 10.0;       //10.0
    public static final int GEOHASH_DEFAULT_PREC = 6;               //6
    public static final int GEOHASH_DEFAULT_MIN_POINT_COUNT = 2;    //2
    public static final double DEFAULT_VEL_FACTOR = 1.0;
    public static final double DEFAULT_POS_FACTOR = 1.0;

    private double accumLat = 0;
    private double accumLon = 0;
    private int count = 0;

    public GPSSensor(Context context)
    {
        m_Context = context;
        ServicesHelper.addLocationServiceInterface(this);
    }

    @Override
    public void locationChanged(Location location)
    {
//        accumLat += location.getLatitude();
//        accumLon += location.getLongitude();
//        count++;
//        double avgLat = accumLat / count;
//        double avgLon = accumLon / count;
//        Log.d("Debug", avgLat + ", " + avgLon);
        for (Listener listener : m_Listeners) {
            listener.onLocationChanged(location.getLatitude(), location.getLongitude());
        }
    }

    @SuppressLint("MissingPermission")
    public void RegisterSensor()
    {
        ServicesHelper.getLocationService(m_Context, value -> {
            if (value.IsRunning())
            {
                return;
            }
            value.stop();
            Settings settings = new Settings(
                    ACCELEROMETER_DEFAULT_DEVIATION,
                    GPS_MIN_DISTANCE,
                    GPS_MIN_TIME,
                    SENSOR_POSITION_MIN_TIME,
                    GEOHASH_DEFAULT_PREC,
                    GEOHASH_DEFAULT_MIN_POINT_COUNT,
                    SENSOR_DEFAULT_FREQ_HZ,
                    this,   // logger
                    true,   // filterMockGpsCoordinates
                    false,  // onlyGpsSensor
                    true,   // useGpsSpeed
                    DEFAULT_VEL_FACTOR,
                    DEFAULT_POS_FACTOR,
                    Settings.LocationProvider.FUSED
            );
            value.reset(settings);
            value.start();
        });
    }

    public void UnregisterSensor()
    {
        ServicesHelper.getLocationService(m_Context, KalmanLocationService::stop);
    }

    @SuppressLint("MissingPermission")
    public void AddListener(GPSSensor.Listener listener)
    {
        m_Listeners.add(listener);
    }
}
