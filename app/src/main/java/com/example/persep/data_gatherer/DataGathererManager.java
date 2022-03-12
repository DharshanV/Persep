package com.example.persep.data_gatherer;

import android.content.Context;
import android.util.Log;

public class DataGathererManager
{
    private static final String TAG = "Data_Gatherer_Manager";

    private RotationSensor m_RotationSensor;
    private GPSSensor m_GPSSensor;

    public DataGathererManager(Context context)
    {
        m_RotationSensor = new RotationSensor(context);
        m_GPSSensor= new GPSSensor(context);
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
    }

    public void onResume()
    {
        Log.d(TAG, "Resumed");
        m_RotationSensor.RegisterSensor();
        m_GPSSensor.RegisterSensor();
    }

    public void AddGyroscopeListener(RotationSensor.Listener listener)
    {
        m_RotationSensor.AddListener(listener);
    }

    public void AddGPSListener(GPSSensor.Listener listener)
    {
        m_GPSSensor.AddListener(listener);
    }
}