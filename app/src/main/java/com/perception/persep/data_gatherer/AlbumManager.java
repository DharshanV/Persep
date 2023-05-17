package com.perception.persep.data_gatherer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.perception.persep.album_data.Album;
import com.perception.persep.album_data.Post;
import com.perception.persep.constants.AlbumConstants;
import com.perception.persep.constants.PostConstants;
import com.perception.persep.constants.QueryConstants;
import com.perception.persep.firebase_helpers.FirebaseQueries;
import com.perception.persep.firebase_helpers.FirebaseUpdates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
Add Post
1. User adds a post and nearby album
2. User adds a post and no nearby album

Delete Post
1. User deletes a post and nearby album (i.e. local data exists) contains it
2. User deletes a post and no nearby album
 */

public class AlbumManager implements Runnable, GPSSensor.Listener
{
    private static final String TAG = "AlbumManager";

    private static AlbumManager m_Instance = null;

    private final Context m_Context;

    private final ReadWriteLock m_ReadWriteLock = new ReentrantReadWriteLock();
    private final Lock m_AlbumWriteLock = m_ReadWriteLock.writeLock();
    private final Lock m_AlbumReadLock = m_ReadWriteLock.readLock();
    private Map<String, Album> m_Albums;

    private double m_QueryLat = 0.0;
    private double m_QueryLon = 0.0;
    private boolean m_IsLocationInit = false;

    private boolean m_IsQueryInProgress = false;
    private final Handler m_QueryNearbyAlbumHandler;

    private AlbumManager(Context context)
    {
        m_Context = context;
        m_QueryNearbyAlbumHandler = new Handler();
    }

    public static synchronized AlbumManager getInstance(Context context)
    {
        if (m_Instance == null) {
            m_Instance = new AlbumManager(context.getApplicationContext());
        }
        return m_Instance;
    }

    public Collection<Album> getAlbums()
    {
        return m_Albums.values();
    }

    public void addPost(Uri postImageURI)
    {
        Album closestAlbum = getClosestLocalAlbum();
        if (closestAlbum == null) {
            closestAlbum = new Album(m_QueryLat, m_QueryLon);
        }

        Album finalClosestAlbum = closestAlbum;
        PostManager.getInstance(m_Context).addPost(postImageURI, closestAlbum.m_AlbumID, (success, message, postID) -> {
            if (success) {
                finalClosestAlbum.addPost(postID);
            }
        });
    }

    public void removeAlbum(String albumID)
    {
        Album album = m_Albums.get(albumID);
        if (album != null) {
            album.remove();
            m_Albums.remove(albumID);
        }
    }

    public void removePost(String postID)
    {
        PostManager.getInstance(m_Context).removePost(postID);

        boolean removedLocalAlbumPost = false;
        for (Map.Entry<String, Album> entry : m_Albums.entrySet())
        {
            Album album = entry.getValue();
            if (album.containsPost(postID)) {
                album.removePost(postID);
                if (album.m_Posts.isEmpty()) {
                    removeAlbum(entry.getKey());
                }
                removedLocalAlbumPost = true;
            }
        }

        if (!removedLocalAlbumPost) {
            // remove remote album post
            DocumentReference postRef = findPost(postID);
            DocumentSnapshot remoteAlbum = findPostAlbum(postID);
            List<DocumentReference> remoteAlbumPostRefs = (List<DocumentReference>) remoteAlbum.get(AlbumConstants.POSTS);
            if (remoteAlbumPostRefs != null && remoteAlbumPostRefs.contains(postRef)) {
                remoteAlbum.getReference().update(AlbumConstants.POSTS, FieldValue.arrayRemove(postRef));
            }
        }
    }

    @Override
    public void run()
    {
        if (m_IsLocationInit && !m_IsQueryInProgress)
        {
            m_IsQueryInProgress = true;
        }
        m_QueryNearbyAlbumHandler.postDelayed(this, QueryConstants.QUERY_UPDATE_LOOP_INTERVAL);
    }

    public void onPause()
    {
        m_QueryNearbyAlbumHandler.removeCallbacks(this);
    }

    public void onResume()
    {
        m_QueryNearbyAlbumHandler.post(this);
    }

    @Override
    public void onLocationChanged(double lat, double lon)
    {
        m_QueryLat = lat;
        m_QueryLon = lon;
        if (!m_IsLocationInit) {
            m_IsLocationInit = true;
            m_QueryNearbyAlbumHandler.removeCallbacks(this);
            m_QueryNearbyAlbumHandler.post(this);
        }
    }

    public void readLock() { m_AlbumReadLock.lock(); }

    public void readUnlock() { m_AlbumReadLock.unlock(); }

    public Album getClosestLocalAlbum()
    {
        if (!m_IsLocationInit) return null;

        double closestDist = Double.MAX_VALUE;
        Album closestAlbum = null;
        GeoLocation m_QueryLocation = new GeoLocation(m_QueryLat, m_QueryLon);
        for (Map.Entry<String, Album> entry : m_Albums.entrySet()) {
            Album album = entry.getValue();
            GeoLocation albumLocation = new GeoLocation(album.m_Lat, album.m_Lon);
            double distanceInM = GeoFireUtils.getDistanceBetween(m_QueryLocation, albumLocation);
            if (distanceInM < closestDist)
            {
                closestAlbum = album;
                closestDist = distanceInM;
            }
        }

        return closestAlbum;
    }
}
