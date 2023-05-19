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
    private final PostManager m_PostManager;

    public DataGathererManager(Context context)
    {
        m_RotationSensor = new RotationSensor(context);
        m_GPSSensor = new GPSSensor(context);
        m_AlbumManger = AlbumManager.getInstance(context);
        m_PostManager = PostManager.getInstance(context);
    }

    public void onStart()
    {
        m_GPSSensor.RegisterSensor();
        m_RotationSensor.RegisterSensor();
    }

    public void onStop()
    {
        m_RotationSensor.UnregisterSensor();
        m_GPSSensor.UnregisterSensor();
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

    public void AddGPSListener(GPSSensor.Listener listener)
    {
        m_GPSSensor.AddListener(listener);
    }

    public AlbumManager getAlbumManger()
    {
        return m_AlbumManger;
    }

    public void AddPost(FirebaseUser user, Uri imageUri)
    {
        m_AlbumManger.tryAddPost(imageUri);
    }
}