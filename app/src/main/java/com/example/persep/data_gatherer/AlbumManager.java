package com.example.persep.data_gatherer;

import android.content.Context;
import android.os.Handler;

import com.example.persep.album_data.Album;

public class AlbumManager implements Runnable
{
    public interface Listener
    {
        void onAlbumUpdate();
    }

    private Listener m_Listener;
    private Album[] m_Albums;

    private final long UPDATE_LOOP_INTERVAL = 5000; //update and notify every 5 secs
    Handler handler;

    AlbumManager(Context context)
    {
        handler = new Handler();
    }

    @Override
    public void run()
    {
        handler.postDelayed(this, UPDATE_LOOP_INTERVAL);
        if (m_Listener != null)
        {
            m_Listener.onAlbumUpdate();
        }
    }

    public void onPause()
    {
        handler.removeCallbacks(this);
    }

    public void onResume()
    {
        handler.postDelayed(this, UPDATE_LOOP_INTERVAL);
    }

    public void AddListener(Listener listener)
    {
        m_Listener = listener;
    }
}
