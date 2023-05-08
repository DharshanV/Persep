package com.perception.persep.data_gatherer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class AccelerometerSensor implements SensorEventListener
{
    private static final String TAG = "RotationSensor";

    public interface Listener
    {
        void onAccelerationChanged(float[] acceleration);
    }

    private Listener m_Listener;

    private final SensorManager m_SensorManager;
    private final Sensor m_AccelerometerSensor;
    private HandlerThread m_SensorThread;
    private Handler m_SensorHandler;

    private final float[] m_Acceleration = new float[3];

    public AccelerometerSensor(Context context)
    {
        m_SensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        m_AccelerometerSensor = m_SensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    public void RegisterSensor()
    {
        if (m_AccelerometerSensor != null)
        {
            m_SensorThread = new HandlerThread("AccelerometerThread", Thread.MAX_PRIORITY);
            m_SensorThread.start();
            m_SensorHandler = new Handler(m_SensorThread.getLooper());
            m_SensorManager.registerListener(this, m_AccelerometerSensor, SensorManager.SENSOR_STATUS_ACCURACY_HIGH, m_SensorHandler);
        }
    }

    public void UnregisterSensor()
    {
        if (m_AccelerometerSensor != null)
        {
            try
            {
                if (m_SensorThread != null) {
                    m_SensorThread.quitSafely();
                    m_SensorThread.join();
                }
                m_SensorThread = null;
                m_SensorHandler = null;
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            m_SensorManager.unregisterListener(this, m_AccelerometerSensor);
        }
    }

    public void AddListener(Listener listener)
    {
        m_Listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (m_Listener != null && event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
        {
            m_Listener.onAccelerationChanged(event.values);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

}
