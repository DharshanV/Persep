package com.perception.persep.data_gatherer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.perception.persep.album_data.Album;
import com.perception.persep.constants.QueryConstants;
import com.perception.persep.firebase_helpers.FirebaseQueries;

import java.util.ArrayList;
import java.util.List;

public class AlbumManager implements Runnable, FirebaseQueries.GeoqueryListener
{
    private static final String TAG = "AlbumManager";

    public interface Listener
    {
        void onAlbumUpdate(List<Album> albums);
    }

    private final Context m_Context;
    private Listener m_Listener = null;
    private final List<Album> m_Albums;
    private final FirebaseQueries m_FirebaseGeo;
    private boolean m_IsQueryInProgress = false;

    private final double m_QueryLat = -33.92447747349893;
    private final double m_QueryLon = 151.2338687296112;

    private final long UPDATE_LOOP_INTERVAL = 5000; //update and notify every 5 secs
    Handler m_Handler;

    AlbumManager(Context context)
    {
        m_Context = context;
        m_Handler = new Handler();
        m_FirebaseGeo = FirebaseQueries.getInstance();
        m_FirebaseGeo.m_Listener = this;
        m_Albums = new ArrayList<>();
    }

    @Override
    public void run()
    {
        m_Handler.postDelayed(this, UPDATE_LOOP_INTERVAL);

        if (!m_IsQueryInProgress && m_Listener != null)
        {
            List<String> ignoreAlbums = GetAlbumsToIgnore();
            m_FirebaseGeo.QueryNearbyAlbums(m_QueryLat, m_QueryLon, ignoreAlbums);
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

    @Override
    public void OnQueryResult(boolean successful, String message, List<Album> albums)
    {
        m_IsQueryInProgress = false;

        if (successful && !albums.isEmpty())
        {
            UpdateAlbums(albums);
        }
    }

    private void UpdateAlbums(List<Album> albums)
    {
        RemoveInvalidAlbums();
        m_Albums.addAll(albums);

        if (m_Listener != null)
        {
            m_Listener.onAlbumUpdate(m_Albums);
        }
    }

    private List<String> GetAlbumsToIgnore()
    {
        List<String> result = new ArrayList<>();
        for (Album album : m_Albums)
        {
            result.add(album.m_GeoHash);
        }
        return result;
    }

    private void RemoveInvalidAlbums()
    {
        GeoLocation queryLocation = new GeoLocation(m_QueryLat, m_QueryLon);
        for (int i = 0; i < m_Albums.size(); i++)
        {
            Album album = m_Albums.get(i);
            GeoLocation albumLocation = new GeoLocation(album.m_Lat, album.m_Lon);
            double distanceInM = GeoFireUtils.getDistanceBetween(albumLocation, queryLocation);

            if (distanceInM >= QueryConstants.MAX_QUERY_RADIUS)
            {
                Log.d(TAG, "Remove album with ID: " + album.m_AlbumID);
                m_Albums.remove(i);
            }
        }
    }
}
