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

public class AlbumManager implements Runnable, GPSSensor.Listener
{
    private static final String TAG = "AlbumManager";

    private static AlbumManager m_Instance = null;

    private final Context m_Context;

    private final Map<String, Album> m_Albums = new HashMap<>();

    private double m_QueryLat = 0.0;
    private double m_QueryLon = 0.0;
    private boolean m_IsLocationInit = false;

    private boolean m_IsQueryInProgress = false;
    private final Handler m_QueryNearbyAlbumHandler;
    private final Stack<Uri> m_AddPostQueue = new Stack<>();

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

    public static synchronized AlbumManager getInstance()
    {
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
        if (!m_IsLocationInit) {
            m_AddPostQueue.add(postImageURI);
            return;
        }
        addPost(postImageURI);
    }

    private void addPost(Uri postImageURI)
    {
        Album closestAlbum = getClosestLocalAlbum();
        if (closestAlbum == null) {
            closestAlbum = new Album(m_Context, m_QueryLat, m_QueryLon);
            m_Albums.put(closestAlbum.m_AlbumID, closestAlbum);
        }

        String postID = PostManager.getInstance(m_Context).addPost(postImageURI, closestAlbum.m_AlbumID, m_QueryLat, m_QueryLon);
        closestAlbum.addPost(postID);
        UserManager.getInstance().addPost(postID);
    }

    public void removePost(String postID)
    {
        PostManager.getInstance(m_Context).removePost(postID, (success, postRef, albumID) -> {
            if (!success) return;
            Album album = m_Albums.get(albumID);
            if (album != null) {
                removeLocalAlbumPost(albumID, postID);
            }
            else {
                removeServerAlbumPost(albumID, postRef);
            }
            UserManager.getInstance().removePost(postRef);
        });
    }

    private void removeLocalAlbumPost(String albumID, String postID)
    {
        Album album = m_Albums.get(albumID);
        if (album != null && album.m_PostIDs.contains(postID)) {
            if (album.m_PostIDs.size() != 1) {
                album.removePost(postID);
            }
            else {
                album.remove();
                m_Albums.remove(albumID);
            }
        }
    }

    private void removeServerAlbumPost(String albumID, DocumentReference postRef)
    {
        FirebaseFirestore.getInstance().collection(AlbumConstants.COLLECTION_NAME).document(albumID).get().addOnSuccessListener(albumDoc -> {
            List<DocumentReference> postRefs = (List<DocumentReference>) albumDoc.get(AlbumConstants.POSTS);
            if (postRefs != null && postRefs.contains(postRef)) {
                if (postRefs.size() != 1) {
                    albumDoc.getReference().update(AlbumConstants.POSTS, FieldValue.arrayRemove(postRef));
                }
                else {
                    albumDoc.getReference().delete();
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
                for (DocumentSnapshot albumDoc : albumDocs) {
                    if (!m_Albums.containsKey(albumDoc.getId())) {
                        m_Albums.put(albumDoc.getId(), new Album(albumDoc));
                    }
                }

                while (!m_AddPostQueue.empty()) {
                    addPost(m_AddPostQueue.pop());
                }
            }).addOnFailureListener(Exception::printStackTrace);
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

    public Album getClosestLocalAlbum()
    {
        if (!m_IsLocationInit) return null;

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

        return closestAlbum;
    }
}
