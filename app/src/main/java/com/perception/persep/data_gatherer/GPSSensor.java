package com.perception.persep.data_gatherer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.LinkedList;

public class GPSSensor
{
    private static final String TAG = "GPSSensor";

    public interface Listener
    {
        void onLocationChanged(double lat, double lon);
    }

    private final ArrayList<Listener> m_Listeners = new ArrayList<>();

    private FusedLocationProviderClient m_FusedLocationProvider = null;
    private LocationRequest m_LocationRequest = null;
    private LocationCallback m_LocationCB = null;

    private final static int MAX_BOUND_COUNT = 10;
    private final static int MIN_NUM_BOUND_TO_CB = 4;
    private final LinkedList<LatLng> m_Bounds = new LinkedList<>();

    private HandlerThread m_SensorThread;

    // The minimum distance between updates in meters
    private static final long MIN_DISTANCE_BW_UPDATES = 0;

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000;

    // Kalman Filter parameters
    private static final double DEG_TO_METER = 111225.0;
    private static final double METER_TO_DEG = 1.0 / DEG_TO_METER;

    private static final double TIME_STEP = 1.0;
    private static final double COORDINATE_NOISE = 4.0 * METER_TO_DEG;
    private KalmanFilterTracker1D m_LatTracker, m_LonTracker;

    public GPSSensor(Context context)
    {
        //get google play fused location service
        m_FusedLocationProvider = LocationServices.getFusedLocationProviderClient(context);

        m_LocationRequest = LocationRequest.create();
        m_LocationRequest.setInterval(MIN_TIME_BW_UPDATES);
        m_LocationRequest.setFastestInterval(MIN_TIME_BW_UPDATES / 2);
        m_LocationRequest.setSmallestDisplacement(MIN_DISTANCE_BW_UPDATES);
        m_LocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        m_LocationCB = new LocationCallback()
        {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult)
            {
                if (m_Listeners.isEmpty()) return;

                Location location = locationResult.getLastLocation();

                double lat = PredictLatitude(location);
                double lon = PredictLongitude(location);

                UpdateLocation(lat, lon);
            }
        };
    }

    private double PredictLatitude(Location location)
    {
        double position = location.getLatitude();
        double noise = location.getAccuracy() * METER_TO_DEG;

        if (m_LatTracker == null) {
            m_LatTracker = new KalmanFilterTracker1D(TIME_STEP, COORDINATE_NOISE);
            m_LatTracker.setState(position, 0.0, noise);
        }

        m_LatTracker.predict(0.0);
        double predictedLat = m_LatTracker.getPosition();
        m_LatTracker.update(position, noise);
        return predictedLat;
    }

    private double PredictLongitude(Location location)
    {
        double position = location.getLongitude();
        double noise = location.getAccuracy() * Math.cos(Math.toRadians(location.getLatitude())) * METER_TO_DEG ;

        if (m_LonTracker == null) {
            m_LonTracker = new KalmanFilterTracker1D(TIME_STEP, COORDINATE_NOISE);
            m_LonTracker.setState(position, 0.0, noise);
        }

        m_LonTracker.predict(0.0);
        double predictedLon = m_LonTracker.getPosition();
        m_LonTracker.update(position, noise);
        return predictedLon;
    }

    @SuppressLint("MissingPermission")
    public void RegisterSensor()
    {
        m_SensorThread = new HandlerThread("GPSThread", Thread.MAX_PRIORITY);
        m_SensorThread.start();
        m_FusedLocationProvider.requestLocationUpdates(m_LocationRequest, m_LocationCB, m_SensorThread.getLooper());
    }

    public void UnregisterSensor()
    {
        try
        {
            if (m_SensorThread != null)
            {
                m_SensorThread.quitSafely();
                m_SensorThread.join();
            }
            m_SensorThread = null;
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        m_FusedLocationProvider.removeLocationUpdates(m_LocationCB);
    }

    @SuppressLint("MissingPermission")
    public void AddListener(GPSSensor.Listener listener)
    {
        m_Listeners.add(listener);
    }

    private void UpdateLocation(double lat, double lon)
    {
        AddLocationToBound(lat, lon);
        if (m_Bounds.size() >= MIN_NUM_BOUND_TO_CB) {
            LatLng center = GetCenter();
            for (Listener listener : m_Listeners)
            {
                listener.onLocationChanged(center.latitude, center.longitude);
            }
        }
    }

    private void AddLocationToBound(double lat, double lon)
    {
        if (m_Bounds.size() == MAX_BOUND_COUNT)
        {
            m_Bounds.removeFirst();
            m_Bounds.addLast(new LatLng(lat, lon));
            return;
        }
        m_Bounds.addLast(new LatLng(lat, lon));
    }

    private LatLng GetCenter()
    {
        LatLngBounds.Builder builder = LatLngBounds.builder();
        for (LatLng location : m_Bounds)
        {
            builder.include(location);
        }
        return builder.build().getCenter();
    }
}
