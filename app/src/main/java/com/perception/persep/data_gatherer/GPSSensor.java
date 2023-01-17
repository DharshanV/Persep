package com.perception.persep.data_gatherer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class GPSSensor
{
    private static final String TAG = "GPSSensor";

    public interface Listener
    {
        void onLocationChanged(double lat, double lon);
    }

    private Listener m_Listener;

    boolean m_IsLocationServiceEnabled = false;
    private FusedLocationProviderClient m_FusedLocationProvider = null;
    private LocationRequest m_LocationRequest = null;
    private LocationCallback m_LocationCB = null;
    private Location m_BestLocation = null;

    private HandlerThread m_SensorThread;

    // The minimum distance between updates in meters
    private static final long MIN_DISTANCE_BW_UPDATES = 4;

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 2500;

    public GPSSensor(Context context)
    {
        boolean isPermissionEnabled = true;
        isPermissionEnabled &= ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        isPermissionEnabled &= ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if(isPermissionEnabled)
        {
            //get google play fused location service
            m_FusedLocationProvider = LocationServices.getFusedLocationProviderClient(context);

            if(m_FusedLocationProvider != null)
            {
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
                        if(m_Listener != null)
                        {
                            Location location = locationResult.getLastLocation();
                            UpdateBestLocation(location);
                            m_Listener.onLocationChanged(m_BestLocation.getLatitude(), m_BestLocation.getLongitude());
                        }
                    }
                };

                m_IsLocationServiceEnabled = true;
            }
        }
        else
        {
            Log.e(TAG, "Permission not enabled for fine and coarse location");
        }
    }

    @SuppressLint("MissingPermission")
    public void RegisterSensor()
    {
        if (!m_IsLocationServiceEnabled)
        {
            return;
        }

        m_SensorThread = new HandlerThread("GPSThread", Thread.MAX_PRIORITY);
        m_SensorThread.start();
        m_FusedLocationProvider.requestLocationUpdates(m_LocationRequest, m_LocationCB, m_SensorThread.getLooper());
    }

    public void UnregisterSensor()
    {
        if(!m_IsLocationServiceEnabled)
        {
            return;
        }

        try
        {
            m_SensorThread.quitSafely();
            m_SensorThread.join();
            m_SensorThread = null;
        } catch (InterruptedException e) {  e.printStackTrace(); }
        m_FusedLocationProvider.removeLocationUpdates(m_LocationCB);
    }

    @SuppressLint("MissingPermission")
    public void AddListener(GPSSensor.Listener listener)
    {
        m_Listener = listener;

        if(m_FusedLocationProvider != null)
        {
            m_FusedLocationProvider.getLastLocation().addOnCompleteListener(task -> {
                Location location = task.getResult();
                if(location != null)
                {
                    UpdateBestLocation(location);
                    m_Listener.onLocationChanged(m_BestLocation.getLatitude(), m_BestLocation.getLongitude());
                }
            });
        }
    }

    private void UpdateBestLocation(Location location)
    {
        if (m_BestLocation == null || location.getAccuracy() < m_BestLocation.getAccuracy())
        {
            m_BestLocation = location;
        }
    }

}
