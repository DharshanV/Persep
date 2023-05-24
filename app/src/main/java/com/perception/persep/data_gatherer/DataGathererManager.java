package com.perception.persep.data_gatherer;

import android.content.Context;

public class DataGathererManager
{
    private static final String TAG = "Data_Gatherer_Manager";

    private final RotationSensor m_RotationSensor;
    private final GPSSensor m_GPSSensor;
    private final AlbumManager m_AlbumManger;
    private final BearingSensor m_BearingSensor;

    public DataGathererManager(Context context)
    {
        m_RotationSensor = new RotationSensor(context);
        m_GPSSensor = new GPSSensor(context);
        m_AlbumManger = AlbumManager.getInstance();
        m_BearingSensor = new BearingSensor(context);
        PostManager.getInstance().createPostLoadingImage();
    }

    public void onStart()
    {
        m_GPSSensor.RegisterSensor();
        m_RotationSensor.RegisterSensor();
        m_BearingSensor.start();
    }

    public void onStop()
    {
        m_RotationSensor.UnregisterSensor();
        m_GPSSensor.UnregisterSensor();
        m_AlbumManger.onStop();
        m_BearingSensor.stop();
    }

    public void onResume()
    {
        m_AlbumManger.onResume();
    }

    public void onPause()
    {
        m_AlbumManger.onPause();
    }

    public void SetGyroscopeListener(RotationSensor.Listener listener)
    {
        m_RotationSensor.AddListener(listener);
    }

    public void AddBearingListener(BearingSensor.BearingSensorListener listener)
    {
        m_BearingSensor.addListener(listener);
    }

    public void AddGPSListener(GPSSensor.Listener listener)
    {
        m_GPSSensor.AddListener(listener);
    }
}