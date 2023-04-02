package com.perception.persep.data_gatherer;

import android.content.Context;
import android.util.Log;

public class DataGathererManager
{
    private static final String TAG = "Data_Gatherer_Manager";

    public RotationSensor m_RotationSensor;
    public GPSSensor m_GPSSensor;
    public AlbumManager m_AlbumManger;

    public DataGathererManager(Context context)
    {
        m_RotationSensor = new RotationSensor(context);
        m_GPSSensor = new GPSSensor(context);
        m_AlbumManger = new AlbumManager(context);
    }

    public void onStart()
    {
    }

    public void onStop()
    {
    }

    public void onPause()
    {
        Log.d(TAG, "Paused");
        m_RotationSensor.UnregisterSensor();
        m_GPSSensor.UnregisterSensor();
        m_AlbumManger.onPause();
    }

    public void onResume()
    {
        Log.d(TAG, "Resumed");
        m_RotationSensor.RegisterSensor();
        m_GPSSensor.RegisterSensor();
        m_AlbumManger.onResume();
    }

    public void AddGyroscopeListener(RotationSensor.Listener listener)
    {
        m_RotationSensor.AddListener(listener);
    }

    public void AddGPSListener(GPSSensor.Listener listener)
    {
        m_GPSSensor.AddListener(listener);
    }

    public void AddAlbumListener(AlbumManager.Listener listener)
    {
        m_AlbumManger.AddListener(listener);
    }
}