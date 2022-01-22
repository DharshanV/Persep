package com.example.persep.data_gatherer.gyroscope;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;

//https://stackoverflow.com/questions/27846604/how-to-get-smooth-orientation-data-in-android
//https://stackoverflow.com/questions/30279065/how-to-get-the-euler-angles-from-the-rotation-vector-sensor-type-rotation-vecto
public class Gyroscope
{
    private static final String TAG = "Gyroscope";

    public interface Listener
    {
        void onRotationDelta(float[] deltaRotationMatrix);
    }

    private Listener m_Listener;

    private final SensorManager m_SensorManager;
    private final Sensor m_Sensor;
    private final SensorEventListener m_SensorEventListener;

    private HandlerThread m_SensorThread;
    private Handler m_SensorHandler;

    private static final double NS2S = 1.0 / 1000000000.0;
    private final float[] m_DeltaRotation = new float[4];
    private final float[] m_DeltaRotationMatrix = new float[16];
    private float m_Timestamp;

    public Gyroscope(Context context)
    {
        m_SensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        m_Sensor = m_SensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        m_Timestamp = SystemClock.elapsedRealtimeNanos();

        m_SensorEventListener = new SensorEventListener()
        {
            @Override
            public void onSensorChanged(SensorEvent event)
            {
                if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
                {
                    Log.d(TAG, "Received unreliable accuracy.");
                    return;
                }

                if(m_Listener != null)
                {
                    if (m_Timestamp != 0)
                    {
                        final double dT = (event.timestamp - m_Timestamp) * NS2S;
                        double axisX = event.values[0];
                        double axisY = event.values[1];
                        double axisZ = event.values[2];

                        double omegaMagnitude = Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

                        if (omegaMagnitude > 0.0001)
                        {
                            axisX /= omegaMagnitude;
                            axisY /= omegaMagnitude;
                            axisZ /= omegaMagnitude;
                        }

                        double thetaOverTwo = omegaMagnitude * dT / 2.0f;
                        double sinThetaOverTwo = Math.sin(thetaOverTwo);
                        double cosThetaOverTwo = Math.cos(thetaOverTwo);
                        m_DeltaRotation[0] = (float) (sinThetaOverTwo * axisX);
                        m_DeltaRotation[1] = (float) (sinThetaOverTwo * axisY);
                        m_DeltaRotation[2] = (float) (sinThetaOverTwo * axisZ);
                        m_DeltaRotation[3] = (float) cosThetaOverTwo;
                    }
                    m_Timestamp = event.timestamp;
                    SensorManager.getRotationMatrixFromVector(m_DeltaRotationMatrix, m_DeltaRotation);
                    m_Listener.onRotationDelta(m_DeltaRotationMatrix);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) { }
        };
    }

    public void RegisterSensor()
    {
        m_SensorThread = new HandlerThread("GyroscopeThread", Thread.MAX_PRIORITY);
        m_SensorThread.start();
        m_SensorHandler = new Handler(m_SensorThread.getLooper());
        m_SensorManager.registerListener(m_SensorEventListener, m_Sensor, SensorManager.SENSOR_DELAY_FASTEST, m_SensorHandler);
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
