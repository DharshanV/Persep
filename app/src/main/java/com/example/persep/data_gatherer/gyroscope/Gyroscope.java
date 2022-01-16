package com.example.persep.data_gatherer.gyroscope;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Gyroscope
{
    public interface Listener
    {
        void onRotation(float rx, float ry, float rz, int accuracy);
    }

    private Listener m_Listener;

    private final SensorManager m_SensorManager;
    private final Sensor m_Sensor;
    private final SensorEventListener m_SensorEventListener;

    public Gyroscope(Context context)
    {
        m_SensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        m_Sensor = m_SensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        m_SensorEventListener = new SensorEventListener()
        {
            @Override
            public void onSensorChanged(SensorEvent event)
            {
                if(m_Listener != null)
                {
                    float[] rotation = event.values;
                    m_Listener.onRotation(rotation[0], rotation[1], rotation[2], event.accuracy);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) { }
        };
    }

    public void RegisterSensor()
    {
        m_SensorManager.registerListener(m_SensorEventListener, m_Sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void UnregisterSensor()
    {
        m_SensorManager.unregisterListener(m_SensorEventListener);
    }

    public void AddListener(Listener listener)
    {
        m_Listener = listener;
    }
}
