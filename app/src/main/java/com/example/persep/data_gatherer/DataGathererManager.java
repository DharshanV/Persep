package com.example.persep.data_gatherer;

import android.content.Context;
import android.util.Log;

import com.example.persep.data_gatherer.gyroscope.Gyroscope;

public class DataGathererManager
{
    private static final String TAG = "Data_Gatherer_Manager";

    private Gyroscope m_Gyroscope;

    public DataGathererManager(Context context)
    {
        m_Gyroscope = new Gyroscope(context);
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
        m_Gyroscope.UnregisterSensor();
    }

    public void onResume()
    {
        Log.d(TAG, "Resumed");
        m_Gyroscope.RegisterSensor();
    }

    public void AddGyroscopeListener(Gyroscope.Listener listener)
    {
        m_Gyroscope.AddListener(listener);
    }
}