package com.example.persep.data_gatherer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class GPSSensor implements LocationListener
{
    private static final String TAG = "GPSSensor";

    public interface Listener
    {
        void onLocationChanged(double lat, double lon);
    }

    private Listener m_Listener;

    boolean m_IsLocationServiceEnabled = false;
    boolean m_IsGPSEnabled = false;
    boolean m_IsNetworkEnabled = false;

    LocationManager m_LocationManager;

    private HandlerThread m_SensorThread;

    // The minimum distance between updates in meters
    private static final long MIN_DISTANCE_BW_UPDATES = 0;

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000;

    public GPSSensor(Context context)
    {
        boolean isPermissionEnabled = true;
        isPermissionEnabled &= ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        isPermissionEnabled &= ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if(isPermissionEnabled)
        {
            m_LocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            m_IsGPSEnabled = m_LocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            m_IsNetworkEnabled = m_LocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!m_IsGPSEnabled && !m_IsNetworkEnabled)
            {
                Log.e(TAG, "No network or GPS provider is enabled to receive location updates.");
            } else
            {
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

        if (m_IsGPSEnabled)
        {
            m_LocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_BW_UPDATES, this, m_SensorThread.getLooper());
        }
        else if(m_IsNetworkEnabled)
        {
            m_LocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_BW_UPDATES, this, m_SensorThread.getLooper());
        }
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
        m_LocationManager.removeUpdates(this);
    }

    public void AddListener(GPSSensor.Listener listener)
    {
        m_Listener = listener;
    }

    @Override
    public void onLocationChanged(@NonNull Location location)
    {
        if(m_Listener != null)
        {
            m_Listener.onLocationChanged(location.getLatitude(), location.getLongitude());
        }
    }

}
