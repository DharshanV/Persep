package com.perception.persep.data_gatherer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AlbumManager implements Runnable, GPSSensor.Listener, BearingSensor.BearingSensorListener
{
    private static final String TAG = "AlbumManager";

    private static AlbumManager m_Instance = null;

    private final Map<String, Album> m_Albums = new HashMap<>();

    private double m_QueryLat = 0.0;
    private double m_QueryLon = 0.0;
    private boolean m_IsLocationInit = false;
    private int m_Azimuth = 0;

    private boolean m_IsQueryInProgress = false;
    private final Handler m_QueryNearbyAlbumHandler = new Handler();
    private final Stack<Uri> m_AddPostQueue = new Stack<>();

    private final ReadWriteLock m_ReadWriteLock = new ReentrantReadWriteLock();
    private final Lock m_WriteLock = m_ReadWriteLock.writeLock();
    private final Lock m_ReadLock = m_ReadWriteLock.writeLock();

    private AlbumManager()
    {
    }

    public static synchronized AlbumManager getInstance()
    {
        if (m_Instance == null) {
            m_Instance = new AlbumManager();
        }
        return m_Instance;
    }

    public Album getAlbum(String albumID)
    {
        return m_Albums.get(albumID);
    }

    public Collection<Album> getAlbums()
    {
        return m_Albums.values();
    }

    public void tryAddPost(Uri postImageURI)
    {
        if (!m_IsLocationInit || m_IsQueryInProgress) {
            m_AddPostQueue.add(postImageURI);
            return;
        }
        addPost(postImageURI);
    }

    private void addPost(Uri postImageURI)
    {
        GeoLocation offsetLocation = offsetAlbumLocation(new GeoLocation(m_QueryLat, m_QueryLon), 10, m_Azimuth);
        m_WriteLock.lock();
        Album closestAlbum = getClosestLocalAlbum();
        if (closestAlbum == null) {
            closestAlbum = new Album(offsetLocation.latitude, offsetLocation.longitude);
            m_Albums.put(closestAlbum.m_AlbumID, closestAlbum);
        }

        String postID = PostManager.getInstance().addPost(postImageURI, closestAlbum.m_AlbumID, offsetLocation.latitude, offsetLocation.longitude);
        closestAlbum.addPost(postID);
        UserManager.getInstance().addPost(postID);
        m_WriteLock.unlock();
    }

    public void removePost(String postID)
    {
        m_WriteLock.lock();
        Post post = PostManager.getInstance().getPost(postID);
        if (post != null) {
            Album album = m_Albums.get(post.m_AlbumID);
            if (album != null) {
                album.removePost(postID);
                if (album.m_PostIDs.isEmpty()) {
                    album.remove();
                    m_Albums.remove(album.m_AlbumID);
                }
            }
            else {
                PostManager.getInstance().removePost(postID);
                removeServerAlbumPost(post.m_AlbumID, post.m_DocRef);
            }
            UserManager.getInstance().removePost(post.m_DocRef);
        }
        m_WriteLock.unlock();
    }

    private void removeServerAlbumPost(String albumID, DocumentReference postRef)
    {
        FirebaseFirestore.getInstance().collection(AlbumConstants.COLLECTION_NAME).document(albumID).get().addOnSuccessListener(albumDoc -> {
            List<DocumentReference> postRefs = (List<DocumentReference>) albumDoc.get(AlbumConstants.POSTS);
            if (postRefs != null && postRefs.contains(postRef)) {
                DocumentReference albumRef = albumDoc.getReference();
                if (postRefs.size() != 1) {
                    albumRef.update(AlbumConstants.POSTS, FieldValue.arrayRemove(postRef));
                    albumRef.update(AlbumConstants.TIME_MODIFIED, Timestamp.now());
                }
                else {
                    albumRef.delete();
                }
            }
        });
    }

    @Override
    public void run()
    {
        if (m_IsLocationInit && !m_IsQueryInProgress)
        {
            m_IsQueryInProgress = true;
            GeoLocation queryLocation = new GeoLocation(m_QueryLat, m_QueryLon);
            List<Task<QuerySnapshot>> queryTasks = FirebaseQueries.GetAlbumQueryTasks(queryLocation, QueryConstants.MAX_QUERY_RADIUS);
            Tasks.whenAllSuccess(queryTasks).addOnSuccessListener(albumsResult -> {
                List<DocumentSnapshot> albumDocs = FirebaseQueries.OnAlbumQueryTasks(albumsResult, queryLocation, QueryConstants.MAX_QUERY_RADIUS);
                m_WriteLock.lock();
                for (DocumentSnapshot albumDoc : albumDocs) {
                    if (!m_Albums.containsKey(albumDoc.getId())) {
                        m_Albums.put(albumDoc.getId(), new Album(albumDoc));
                    }
                }
                m_WriteLock.unlock();

                while (!m_AddPostQueue.empty()) {
                    addPost(m_AddPostQueue.pop());
                }
                m_IsQueryInProgress = false;
            }).addOnFailureListener(e -> {
                m_IsQueryInProgress = false;
            });
        }
        m_QueryNearbyAlbumHandler.postDelayed(this, QueryConstants.QUERY_UPDATE_LOOP_INTERVAL);
    }

    public void onStop()
    {
    }

    public void onPause()
    {
        m_QueryNearbyAlbumHandler.removeCallbacks(this);
    }

    public void onResume()
    {
        m_QueryNearbyAlbumHandler.post(this);
    }

    public void readLock()
    {
        m_ReadLock.lock();
    }

    public void readUnlock()
    {
        m_ReadLock.unlock();
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

    private Album getClosestLocalAlbum()
    {
        if (!m_IsLocationInit) return null;

        m_ReadLock.lock();
        double closestDist = Double.MAX_VALUE;
        Album closestAlbum = null;
        GeoLocation m_QueryLocation = new GeoLocation(m_QueryLat, m_QueryLon);
        for (Map.Entry<String, Album> entry : m_Albums.entrySet()) {
            Album album = entry.getValue();
            GeoLocation albumLocation = new GeoLocation(album.m_GeoPoint.getLatitude(), album.m_GeoPoint.getLongitude());
            double distanceInM = GeoFireUtils.getDistanceBetween(m_QueryLocation, albumLocation);
            if (distanceInM < closestDist)
            {
                closestAlbum = album;
                closestDist = distanceInM;
            }
        }
        m_ReadLock.unlock();
        return closestAlbum;
    }

    public static GeoLocation offsetAlbumLocation(GeoLocation center, int distance, double bearing)
    {
        final double R = 6371e3;

        double lat1 = Math.toRadians(center.latitude);
        double lon1 = Math.toRadians(center.longitude);
        double radbear = Math.toRadians(bearing);

        double lat2 = Math.asin( Math.sin(lat1)*Math.cos(distance / R) + Math.cos(lat1)*Math.sin(distance/R)*Math.cos(radbear));
        double lon2 = lon1 + Math.atan2(Math.sin(radbear)*Math.sin(distance / R) * Math.cos(lat1), Math.cos(distance/R)-Math.sin(lat1)*Math.sin(lat2));
        return new GeoLocation(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    @Override
    public void onBearingSensorAngleChanged(int azimuth)
    {
        m_Azimuth = azimuth;
    }
}
