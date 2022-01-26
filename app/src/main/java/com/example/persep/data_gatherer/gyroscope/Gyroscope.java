package com.example.persep.data_gatherer.gyroscope;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class Gyroscope
{
    private static final String TAG = "Gyroscope";

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

    private static final float LOW_PASS_FILTER_SENSITIVITY = 1/10F;
    private float[] m_RotationValue;
    private final float[] m_RotationMatrix = new float[16];

    //Exponential Moving Average
    private static final int EMA_LOOK_BACK = 20;
    private static final float EMA_WEIGHT_MULTIPLIER = 2.0f/(EMA_LOOK_BACK + 1);
    private float[] m_AvgRotationVector = new float[4];
    private float[] m_PrevAvgRotationVector = new float[4];
    private boolean m_FirstData = true;

    public Gyroscope(Context context)
    {
        m_SensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        m_RotationSensor = m_SensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        m_SensorEventListener = new SensorEventListener()
        {
            @Override
            public void onSensorChanged(SensorEvent event)
            {
                if(m_Listener != null && event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
                {
                    //Log.d(TAG, String.format("<%.2f, %.2f, %.2f, %.2f>", m_RotationValue[0], m_RotationValue[1], m_RotationValue[2], m_RotationValue[3]));
                    m_RotationValue = LowPassFilter(event.values.clone(), m_RotationValue);
                    UpdateAverage(m_RotationValue);
                    SensorManager.getRotationMatrixFromVector(m_RotationMatrix, m_AvgRotationVector);
                    m_Listener.onRotationDelta(m_RotationMatrix);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) { }
        };
    }

    private float[] LowPassFilter(float[] input, float[] output)
    {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++)
        {
            output[i] = output[i] + LOW_PASS_FILTER_SENSITIVITY * (input[i] - output[i]);
        }
        return output;
    }

    private void UpdateAverage(float[] newData)
    {
        if(m_FirstData)
        {
            m_AvgRotationVector = newData.clone();
            m_PrevAvgRotationVector = newData.clone();
            m_FirstData = false;
            return;
        }
        float[] temp = new float[4];
        for(int i=0;i<4;i++)
        {
            temp[i] = ((newData[i] - m_PrevAvgRotationVector[i]) * EMA_WEIGHT_MULTIPLIER) + m_PrevAvgRotationVector[i];
        }
        m_AvgRotationVector = temp;
        m_PrevAvgRotationVector = m_AvgRotationVector;
    }

    public void RegisterSensor()
    {
        if(m_RotationSensor != null)
        {
            m_SensorThread = new HandlerThread("GyroscopeThread", Thread.MAX_PRIORITY);
            m_SensorThread.start();
            m_SensorHandler = new Handler(m_SensorThread.getLooper());
            m_SensorManager.registerListener(m_SensorEventListener, m_RotationSensor, SensorManager.SENSOR_DELAY_FASTEST, m_SensorHandler);
        }
    }

    public void UnregisterSensor()
    {
        try
        {
            m_SensorThread.quitSafely();
            m_SensorThread.join();
            m_SensorThread = null;
            m_SensorHandler = null;
        } catch (InterruptedException e) {  e.printStackTrace(); }
        m_SensorManager.unregisterListener(m_SensorEventListener);
    }

    public void AddListener(Listener listener)
    {
        m_Listener = listener;
    }
}
