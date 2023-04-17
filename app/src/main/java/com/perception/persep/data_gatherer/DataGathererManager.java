package com.perception.persep.data_gatherer;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;

public class DataGathererManager
{
    private static final String TAG = "Data_Gatherer_Manager";

    private final RotationSensor m_RotationSensor;
    private final GPSSensor m_GPSSensor;
    private final AlbumManager m_AlbumManger;

    public DataGathererManager(Context context)
    {
        m_RotationSensor = new RotationSensor(context);
        m_GPSSensor = new GPSSensor(context);
        m_AlbumManger = new AlbumManager(context);
    }

    public void onPause()
    {
        Log.d(TAG, "Paused");
        m_RotationSensor.UnregisterSensor();
        m_GPSSensor.UnregisterSensor();
        m_AlbumManger.onPause();
    }

    public void onResume()
    {
        Log.d(TAG, "Resumed");
        m_RotationSensor.RegisterSensor();
        m_GPSSensor.RegisterSensor();
        m_AlbumManger.onResume();
    }

    public void SetGyroscopeListener(RotationSensor.Listener listener)
    {
        m_RotationSensor.AddListener(listener);
    }

    public void AddGPSListener(GPSSensor.Listener listener)
    {
        m_GPSSensor.AddListener(listener);
    }

    public void SetAlbumListener(AlbumManager.Listener listener)
    {
        m_AlbumManger.SetListener(listener);
    }

    public AlbumManager getAlbumManger()
    {
        return m_AlbumManger;
    }

    public void AddPost(FirebaseUser user, Uri imageUri)
    {
        m_AlbumManger.AddPost(user, imageUri);
    }
}