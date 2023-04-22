package com.perception.persep.data_gatherer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.HandlerThread;
import android.os.SystemClock;

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

    private final static int MAX_BOUND_COUNT = 20;
    private final static int MIN_NUM_BOUND_TO_CB = 4;
    private final static double MIN_ACCURACY_TO_CB = 4.0; // meters
    private final static long MIN_SECS_TO_CB = 5000; // 5 seconds
    private final LinkedList<LatLng> m_Bounds = new LinkedList<>();
    private Location m_BestLocation = null;
    private long m_GPSensorStartTime = 0;

    private HandlerThread m_SensorThread;

    // The minimum distance between updates in meters
    private static final long MIN_DISTANCE_BW_UPDATES = 0;

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000;

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
                if (!m_Listeners.isEmpty())
                {
                    UpdateLocation(locationResult.getLastLocation());
                }
            }
        };
    }

    @SuppressLint("MissingPermission")
    public void RegisterSensor()
    {
        m_GPSensorStartTime = SystemClock.elapsedRealtime();
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

        if (m_FusedLocationProvider != null)
        {
            m_FusedLocationProvider.getLastLocation().addOnCompleteListener(task -> {
                if (!m_Listeners.isEmpty())
                {
                    UpdateLocation(task.getResult());
                }
            });
        }
    }

    private void UpdateLocation(Location location)
    {
        if (m_BestLocation == null || location.getAccuracy() < m_BestLocation.getAccuracy())
        {
            m_BestLocation = location;
            AddLocationToBound(m_BestLocation);
            double elapsedTime = SystemClock.elapsedRealtime() - m_GPSensorStartTime;
            if (m_BestLocation.getAccuracy() <= MIN_ACCURACY_TO_CB || m_Bounds.size() >= MIN_NUM_BOUND_TO_CB || elapsedTime > MIN_SECS_TO_CB)
            {
                LatLng center = GetCenter();
                for (Listener listener : m_Listeners)
                {
                    listener.onLocationChanged(center.latitude, center.longitude);
                }
            }
        }
    }

    private void AddLocationToBound(Location location)
    {
        if (m_Bounds.size() == MAX_BOUND_COUNT)
        {
            m_Bounds.removeFirst();
            m_Bounds.addLast(new LatLng(location.getLatitude(), location.getLongitude()));
            return;
        }
        m_Bounds.addLast(new LatLng(location.getLatitude(), location.getLongitude()));
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
