package com.perception.persep.data_gatherer;

import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.perception.persep.activities.HomeActivity;
import com.perception.persep.album_data.Album;
import com.perception.persep.album_data.Post;
import com.perception.persep.constants.AlbumConstants;
import com.perception.persep.constants.PostConstants;
import com.perception.persep.constants.QueryConstants;
import com.perception.persep.enums.AlbumError;
import com.perception.persep.firebase_helpers.FirebaseQueries;

import java.util.Collection;
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

    private final Map<String, Album> m_Albums = new HashMap<>();
    private final Handler m_QueryNearbyAlbumHandler = new Handler();
    private final Stack<Uri> m_AddPostQueue = new Stack<>();
    private final Map<Uri, Integer> m_PostBearing = new HashMap<>();
    private final ReadWriteLock m_ReadWriteLock = new ReentrantReadWriteLock();
    private final Lock m_WriteLock = m_ReadWriteLock.writeLock();
    private final Lock m_ReadLock = m_ReadWriteLock.readLock();
    private double m_QueryLat = 0.0;
    private double m_QueryLon = 0.0;
    private boolean m_IsLocationInit = false;
    private boolean m_IsQueryInProgress = false;

    private AlbumManager()
    {
    }

    public static synchronized AlbumManager getInstance()
    {
        if (m_Instance == null)
        {
            m_Instance = new AlbumManager();
        }
        return m_Instance;
    }

    public static GeoLocation offsetAlbumLocation(GeoLocation center, int distance, double bearing)
    {
        final double R = 6371e3;

        double lat1 = Math.toRadians(center.latitude);
        double lon1 = Math.toRadians(center.longitude);
        double radbear = Math.toRadians(bearing);

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distance / R) + Math.cos(lat1) * Math.sin(distance / R) * Math.cos(radbear));
        double lon2 = lon1 + Math.atan2(Math.sin(radbear) * Math.sin(distance / R) * Math.cos(lat1), Math.cos(distance / R) - Math.sin(lat1) * Math.sin(lat2));
        return new GeoLocation(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    public Album getAlbum(String albumID)
    {
        m_ReadLock.lock();
        Album album = m_Albums.get(albumID);
        m_ReadLock.unlock();
        return album;
    }

    public Collection<Album> getAlbums()
    {
        m_ReadLock.lock();
        Collection<Album> albums = m_Albums.values();
        m_ReadLock.unlock();
        return albums;
    }

    public void tryAddPost(Uri postImageURI, int bearing)
    {
        m_PostBearing.put(postImageURI, bearing);
        if (!m_IsLocationInit || m_IsQueryInProgress)
        {
            m_AddPostQueue.add(postImageURI);
            return;
        }
        addPost(postImageURI);
    }

    private void addPost(Uri postImageURI)
    {
        int postBearing = m_PostBearing.containsKey(postImageURI) ? m_PostBearing.remove(postImageURI) : 0;
        GeoLocation offsetLocation = offsetAlbumLocation(new GeoLocation(m_QueryLat, m_QueryLon), 10, postBearing);
        Album closestLocalAlbum = getClosestLocalAlbum();
        if (closestLocalAlbum == null)
        {
            m_QueryNearbyAlbumHandler.removeCallbacks(this);
            m_IsQueryInProgress = true;
            GeoLocation queryLocation = new GeoLocation(m_QueryLat, m_QueryLon);
            List<Task<QuerySnapshot>> queryTasks = FirebaseQueries.GetAlbumQueryTasks(queryLocation, QueryConstants.MAX_QUERY_RADIUS);
            Tasks.whenAllSuccess(queryTasks).addOnSuccessListener(albumsResult -> {
                DocumentSnapshot closestServerAlbum = FirebaseQueries.OnAlbumQueryTasksClosestAlbum(albumsResult, queryLocation, QueryConstants.MAX_QUERY_RADIUS);

                Album newClosestLocalAlbum;
                if (closestServerAlbum == null)
                {
                    newClosestLocalAlbum = new Album(offsetLocation.latitude, offsetLocation.longitude);
                }
                else
                {
                    newClosestLocalAlbum = new Album(closestServerAlbum);
                }

                m_WriteLock.lock();
                m_Albums.put(newClosestLocalAlbum.m_AlbumID, newClosestLocalAlbum);
                addPostLocalAlbum(newClosestLocalAlbum, postImageURI, offsetLocation);

                while (!m_AddPostQueue.isEmpty())
                {
                    Uri pendingPostUri = m_AddPostQueue.pop();
                    int pendingPostBearing = m_PostBearing.containsKey(pendingPostUri) ? m_PostBearing.remove(pendingPostUri) : 0;
                    GeoLocation pendingPostLocation = offsetAlbumLocation(new GeoLocation(m_QueryLat, m_QueryLon), 10, pendingPostBearing);
                    addPostLocalAlbum(newClosestLocalAlbum, pendingPostUri, pendingPostLocation);
                }
                m_WriteLock.unlock();

                m_IsQueryInProgress = false;
                m_QueryNearbyAlbumHandler.post(this);
            }).addOnFailureListener(e -> {
                m_IsQueryInProgress = false;
                m_QueryNearbyAlbumHandler.post(this);
            });
        }
        else
        {
            addPostLocalAlbum(closestLocalAlbum, postImageURI, offsetLocation);
        }
    }

    private void addPostLocalAlbum(Album album, Uri postImageURI, GeoLocation offsetLocation)
    {
        m_WriteLock.lock();
        String postID = PostManager.getInstance().addPost(postImageURI, album.m_AlbumID, offsetLocation.latitude, offsetLocation.longitude);
        album.addPost(postID);
        UserManager.getInstance().addPost(postID);
        m_WriteLock.unlock();
    }

    public void removePost(String postID)
    {
        m_WriteLock.lock();
        Post post = PostManager.getInstance().getPost(postID);
        if (post != null)
        {
            Album album = m_Albums.get(post.m_AlbumID);
            if (album != null)
            {
                album.removePost(postID);
                if (album.m_PostIDs.isEmpty())
                {
                    album.remove();
                    m_Albums.remove(album.m_AlbumID);
                }
            } else
            {
                PostManager.getInstance().removePost(postID);
                removeServerAlbumPost(post.m_AlbumID, post.m_DocRef);
            }
            UserManager.getInstance().removePost(post.m_DocRef);
        }
        m_WriteLock.unlock();
    }

    public void removePost(DocumentSnapshot postDoc)
    {
        String albumID = (String) postDoc.get(PostConstants.ALBUM_ID);
        if (albumID != null) {
            Album album = m_Albums.get(albumID);
            if (album != null)
            {
                album.removePost(postDoc.getId());
                if (album.m_PostIDs.isEmpty())
                {
                    album.remove();
                    m_Albums.remove(album.m_AlbumID);
                }
            } else
            {
                removeServerAlbumPost(albumID, postDoc.getReference());
                PostManager.getInstance().removePost(postDoc);
            }
        }
    }

    public void removeInvalidAlbumPost(String albumID, DocumentReference postRef)
    {
        removeServerAlbumPost(albumID, postRef);
    }

    private void removeAlbum(String albumID)
    {
        m_WriteLock.lock();
        Album album = m_Albums.get(albumID);
        if (album != null)
        {
            album.remove();
        }
        m_Albums.remove(albumID);
        m_WriteLock.unlock();
    }

    private void removeServerAlbumPost(String albumID, DocumentReference postRef)
    {
        FirebaseFirestore.getInstance().collection(AlbumConstants.COLLECTION_NAME).document(albumID).get().addOnSuccessListener(albumDoc -> {
            List<DocumentReference> postRefs = (List<DocumentReference>) albumDoc.get(AlbumConstants.POSTS);
            if (postRefs != null && postRefs.contains(postRef))
            {
                DocumentReference albumRef = albumDoc.getReference();
                if (postRefs.size() != 1)
                {
                    WriteBatch batch = FirebaseFirestore.getInstance().batch();
                    albumRef.update(AlbumConstants.POSTS, FieldValue.arrayRemove(postRef));
                    albumRef.update(AlbumConstants.TIME_MODIFIED, Timestamp.now());
                    batch.commit();
                } else
                {
                    albumRef.delete();
                }
            }
        });
    }

    @Override
    public void run()
    {
        for (Map.Entry<String, Album> albumEntry : m_Albums.entrySet())
        {
            Album album = albumEntry.getValue();
            GeoLocation albumLocation = new GeoLocation(album.m_GeoPoint.getLatitude(), album.m_GeoPoint.getLongitude());
            GeoLocation currLocation = new GeoLocation(m_QueryLat, m_QueryLon);
            double distanceInM = GeoFireUtils.getDistanceBetween(albumLocation, currLocation);
            if (distanceInM > QueryConstants.MAX_QUERY_RADIUS) {
                m_WriteLock.lock();
                album.unload();
                m_Albums.remove(albumEntry.getKey());
                m_WriteLock.unlock();
            }
        }

        if (m_IsLocationInit && !m_IsQueryInProgress)
        {
            m_IsQueryInProgress = true;
            GeoLocation queryLocation = new GeoLocation(m_QueryLat, m_QueryLon);
            List<Task<QuerySnapshot>> queryTasks = FirebaseQueries.GetAlbumQueryTasks(queryLocation, QueryConstants.MAX_QUERY_RADIUS);
            Tasks.whenAllSuccess(queryTasks).addOnSuccessListener(albumsResult -> {
                List<DocumentSnapshot> albumDocs = FirebaseQueries.OnAlbumQueryTasks(albumsResult, queryLocation, QueryConstants.MAX_QUERY_RADIUS);
                m_WriteLock.lock();
                for (DocumentSnapshot albumDoc : albumDocs)
                {
                    if (!m_Albums.containsKey(albumDoc.getId()))
                    {
                        m_Albums.put(albumDoc.getId(), new Album(albumDoc));
                    }
                }
                m_WriteLock.unlock();

                while (!m_AddPostQueue.empty())
                {
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

    public void onAlbumError(String albumID, String postID, AlbumError error)
    {
        switch (error)
        {
            case SET_DOCUMENT_REF:
            case CREATE_DOCUMENT_SNAPSHOT:
                showToast("Error occurred creating album.");
                removeAlbum(albumID);
                break;
            case UPDATE_POSTS:
                showToast("Unable to update album post's. Try again later.");
                removePost(postID);
                break;
        }
    }

    private void showToast(String message)
    {
        Toast.makeText(HomeActivity.getHomeActivityContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location)
    {
        if (!m_IsQueryInProgress) {
            m_QueryLat = location.getLatitude();
            m_QueryLon = location.getLongitude();
        }
        if (!m_IsLocationInit)
        {
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
        for (Map.Entry<String, Album> entry : m_Albums.entrySet())
        {
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
}
