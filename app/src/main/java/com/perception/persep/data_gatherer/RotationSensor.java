package com.perception.persep.data_gatherer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.apache.commons.numbers.quaternion.Quaternion;
import org.apache.commons.numbers.quaternion.Slerp;

import java.util.Arrays;

public class RotationSensor
{
    private static final String TAG = "RotationSensor";

    public interface Listener
    {
        void onRotationDelta(float[] deltaRotationMatrix);
    }

    private Listener m_Listener;

    private final SensorManager m_SensorManager;
    private final Sensor m_RotationSensor;
    private final SensorEventListener m_SensorEventListener;

    private HandlerThread m_SensorThread;
    private Handler m_SensorHandler;

    private final float[] m_RotationMatrix = new float[16];

    public RotationSensor(Context context)
    {
        m_SensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        m_RotationSensor = m_SensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        m_SensorEventListener = new SensorEventListener()
        {
            @Override
            public void onSensorChanged(SensorEvent event)
            {
                if (m_Listener != null && event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
                {
                    SensorManager.getRotationMatrixFromVector(m_RotationMatrix, event.values);
                    m_Listener.onRotationDelta(m_RotationMatrix);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy)
            {
            }
        };
    }

    public void RegisterSensor()
    {
        if (m_RotationSensor != null)
        {
            m_SensorThread = new HandlerThread("GyroscopeThread", Thread.MAX_PRIORITY);
            m_SensorThread.start();
            m_SensorHandler = new Handler(m_SensorThread.getLooper());
            m_SensorManager.registerListener(m_SensorEventListener, m_RotationSensor, SensorManager.SENSOR_DELAY_GAME, m_SensorHandler);
        }
    }

    public void UnregisterSensor()
    {
        if (m_RotationSensor != null)
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
            m_SensorManager.unregisterListener(m_SensorEventListener);
        }
    }

    public void AddListener(Listener listener)
    {
        m_Listener = listener;
    }
}
