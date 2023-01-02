package com.perception.persep.data_gatherer;

import android.content.Context;
import android.os.Handler;

import com.perception.persep.album_data.Album;
import com.perception.persep.album_data.Post;
import com.perception.persep.firebase_helpers.FirebaseGeoqueries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AlbumManager implements Runnable, FirebaseGeoqueries.GeoqueryListener
{
    public interface Listener
    {
        void onAlbumUpdate();
    }

    private Context m_Context;
    private Listener m_Listener;
    private List<Album> m_Albums;
    private FirebaseGeoqueries m_FirebaseGeo = null;
    private boolean m_IsQueryInProgress = false;

    private final long UPDATE_LOOP_INTERVAL = 5000; //update and notify every 5 secs
    Handler m_Handler;

    AlbumManager(Context context)
    {
        m_Context = context;
        m_Handler = new Handler();
        m_FirebaseGeo = FirebaseGeoqueries.getInstance();
        m_FirebaseGeo.m_Listener = this;
    }

    @Override
    public void run()
    {
        m_Handler.postDelayed(this, UPDATE_LOOP_INTERVAL);

        if (!m_IsQueryInProgress)
        {
            List<String> ignoreAlbums = GetAlbumsToIgnore();
            m_FirebaseGeo.QueryNearbyAlbums(-33.92447747349893, 151.2338687296112, ignoreAlbums);
            m_IsQueryInProgress = true;
        }
    }

    public void onPause()
    {
        m_Handler.removeCallbacks(this);
    }

    public void onResume()
    {
        m_Handler.postDelayed(this, UPDATE_LOOP_INTERVAL);
    }

    public void AddListener(Listener listener)
    {
        m_Listener = listener;
    }

    public void TestAddAlbum()
    {
        m_FirebaseGeo.AddPost(new Post(UUID.randomUUID().toString()), -33.92447747349893, 151.2338687296112);
    }

    @Override
    public void OnQueryResult(boolean successful, String message, List<Album> albums)
    {
        UpdateAlbums(albums);
        m_IsQueryInProgress = false;
    }

    private void UpdateAlbums(List<Album> albums)
    {
        if (m_Listener != null)
        {
            m_Listener.onAlbumUpdate();
        }
    }

    private List<String> GetAlbumsToIgnore()
    {
        List<String> result = new ArrayList<>();
        result.add("TEMP");
        return result;
    }
}
