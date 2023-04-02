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

    private static final float LOW_PASS_FILTER_SENSITIVITY = 1 / 10F;
    private final float[] m_RotationValue = null;
    private final float[] m_RotationMatrix = new float[16];

    //Exponential Moving Average
    private static final int EMA_LOOK_BACK = 20;
    private static final float EMA_WEIGHT_MULTIPLIER = 2.0f / (EMA_LOOK_BACK + 1);
    private float[] m_PrevRotationVector = new float[4];
    private boolean m_FirstData = true;

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
                    //Log.d(TAG, String.format("<%.2f, %.2f, %.2f, %.2f>", m_RotationValue[0], m_RotationValue[1], m_RotationValue[2], m_RotationValue[3]));
//                    m_RotationValue = LowPassFilter(event.values.clone(), m_RotationValue);
//                    UpdateAverage(m_RotationValue);
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

    private float[] GetSerpedRotationVector(float[] rotationVector, double t)
    {
        if (m_FirstData)
        {
            m_PrevRotationVector = rotationVector.clone();
            m_FirstData = false;
            return rotationVector;
        }

        float q0 = rotationVector[3];
        float q1 = rotationVector[0];
        float q2 = rotationVector[1];
        float q3 = rotationVector[2];

        Quaternion quaternion1 = Quaternion.of(m_PrevRotationVector[3], m_PrevRotationVector[0], m_PrevRotationVector[1], m_PrevRotationVector[2]);
        Quaternion quaternion2 = Quaternion.of(q0, q1, q2, q3);
        Slerp slerp = new Slerp(quaternion1, quaternion2);
        Quaternion result = slerp.apply(t);

        Log.d(TAG, Arrays.toString(m_PrevRotationVector));

        m_PrevRotationVector = rotationVector.clone();
        rotationVector[0] = (float) result.getX();
        rotationVector[1] = (float) result.getY();
        rotationVector[2] = (float) result.getZ();
        rotationVector[3] = (float) result.getW();

        Log.d(TAG, Arrays.toString(rotationVector));
        return rotationVector;
    }

    public void RegisterSensor()
    {
        if (m_RotationSensor != null)
        {
            m_SensorThread = new HandlerThread("GyroscopeThread", Thread.MAX_PRIORITY);
            m_SensorThread.start();
            m_SensorHandler = new Handler(m_SensorThread.getLooper());
            m_SensorManager.registerListener(m_SensorEventListener, m_RotationSensor, SensorManager.SENSOR_DELAY_FASTEST, m_SensorHandler);
        }
    }

    public void UnregisterSensor()
    {
        if (m_RotationSensor != null)
        {
            try
            {
                m_SensorThread.quitSafely();
                m_SensorThread.join();
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
