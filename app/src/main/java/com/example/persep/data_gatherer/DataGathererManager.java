package com.example.persep.data_gatherer;

import android.content.Context;
import android.util.Log;

public class DataGathererManager
{
    private static final String TAG = "Data_Gatherer_Manager";

    private RotationSensor m_RotationSensor;

    public DataGathererManager(Context context)
    {
        m_RotationSensor = new RotationSensor(context);
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
    }

    public void onResume()
    {
        Log.d(TAG, "Resumed");
        m_RotationSensor.RegisterSensor();
    }

    public void AddGyroscopeListener(RotationSensor.Listener listener)
    {
        m_RotationSensor.AddListener(listener);
    }
}