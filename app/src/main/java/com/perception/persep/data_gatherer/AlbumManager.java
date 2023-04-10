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
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.perception.persep.album_data.Album;
import com.perception.persep.album_data.Post;
import com.perception.persep.constants.AlbumConstants;
import com.perception.persep.constants.QueryConstants;
import com.perception.persep.firebase_helpers.FirebaseQueries;
import com.perception.persep.firebase_helpers.FirebaseUpdates;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AlbumManager implements Runnable, FirebaseQueries.QueryListener, FirebaseUpdates.UpdatesListener, GPSSensor.Listener, EventListener<QuerySnapshot>
{
    private static final String TAG = "AlbumManager";

    public interface Listener
    {
        void onAlbumUpdate(List<Album> albums);
    }

    private final Context m_Context;
    private Listener m_Listener = null;

    ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    Lock m_AlbumWriteLock = readWriteLock.writeLock();
    Lock m_AlbumReadLock = readWriteLock.readLock();
    private final List<Album> m_Albums;

    private final FirebaseQueries m_FirebaseQueries;
    private final FirebaseUpdates m_FirebaseUpdates;
    private boolean m_IsQueryInProgress = false;

    private double m_QueryLat = 0.0;
    private double m_QueryLon = 0.0;
    private boolean m_IsLocationInit = false;

    private final long UPDATE_LOOP_INTERVAL = 5000; //update and notify every 5 secs
    Handler m_Handler;

    AlbumManager(Context context)
    {
        m_Context = context;
        m_Handler = new Handler();
        m_FirebaseQueries = FirebaseQueries.getInstance();
        m_FirebaseUpdates = FirebaseUpdates.getInstance();
        m_FirebaseQueries.m_Listener = this;
        m_FirebaseUpdates.m_Listener = this;
        m_Albums = new ArrayList<>();
    }

    @Override
    public void run()
    {
        m_Handler.postDelayed(this, UPDATE_LOOP_INTERVAL);

        if (m_IsLocationInit && !m_IsQueryInProgress && m_Listener != null)
        {
            List<String> ignoreAlbums = GetAlbumsToIgnore();
            m_FirebaseQueries.QueryNearbyAlbums(m_QueryLat, m_QueryLon, ignoreAlbums);
            m_IsQueryInProgress = true;
        }
    }

    @Override
    public void onLocationChanged(double lat, double lon)
    {
        m_QueryLat = lat;
        m_QueryLon = lon;
        m_IsLocationInit = true;
    }

    public void onPause()
    {
        m_Handler.removeCallbacks(this);
        FirebaseQueries.getInstance().RemoveAlbumListener();
    }

    public void onResume()
    {
        m_Handler.postDelayed(this, UPDATE_LOOP_INTERVAL);
        FirebaseQueries.getInstance().AddAlbumListener(this);
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

    @Override
    public void OnUpdateResult(boolean successful, String message)
    {
        if (successful)
        {
            Toast.makeText(m_Context, "Successfully created your post! Restart your app", Toast.LENGTH_LONG).show();
        }
    }

    public void AddPost(FirebaseUser user, Uri postImageURI)
    {
        m_FirebaseUpdates.AddPost(user, postImageURI, m_QueryLat, m_QueryLon);
    }

    private void UpdateAlbums(List<Album> albums)
    {
        m_AlbumWriteLock.lock();
        RemoveInvalidAlbums();
        m_Albums.addAll(albums);
        m_AlbumWriteLock.unlock();

        if (m_Listener != null)
        {
            m_AlbumReadLock.lock();
            m_Listener.onAlbumUpdate(m_Albums);
            m_AlbumReadLock.unlock();
        }
    }

    private void UpdateAlbumModify(Album album, QueryDocumentSnapshot albumDoc)
    {
        List<DocumentReference> albumDocPostRefs = (List<DocumentReference>) albumDoc.get(AlbumConstants.POSTS);
        List<Task<DocumentSnapshot>> postQueryTasks = new ArrayList<>(albumDocPostRefs.size());
        for (DocumentReference postRef : albumDocPostRefs)
        {
            postQueryTasks.add(postRef.get());
        }

        Tasks.whenAllSuccess(postQueryTasks).addOnSuccessListener(postsResult -> {
            for (Object postObj : postsResult)
            {
                DocumentSnapshot postDoc = (DocumentSnapshot) postObj;
                if (postDoc == null || album == null) continue;

                boolean albumContainsPost = false;
                for (Post post : album.m_Posts)
                {
                    if (post.m_PostID.equals(postDoc.getId()))
                    {
                        albumContainsPost = true;
                        break;
                    }
                }

                if (!albumContainsPost)
                {
                    album.AddPost(new Post(postDoc.getId(), postDoc.getData()));
                }
            }
        });
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error)
    {
        if (value == null || error != null)
        {
            Log.d(TAG, "Album listener error!");
            return;
        }

        for (DocumentChange dc : value.getDocumentChanges())
        {
            switch (dc.getType())
            {
                case ADDED:
                case REMOVED:
                    return;
                case MODIFIED:
                    m_AlbumReadLock.lock();
                    for (Album album : m_Albums)
                    {
                        QueryDocumentSnapshot albumDoc = dc.getDocument();
                        if (album.m_AlbumID.equals(albumDoc.getId()))
                        {
                            UpdateAlbumModify(album, albumDoc);
                        }
                    }
                    m_AlbumReadLock.unlock();
                    return;
            }
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
        ArrayList<Integer> albumIdxToRemove = new ArrayList<>();
        for (int i = 0; i < m_Albums.size(); i++)
        {
            Album album = m_Albums.get(i);
            GeoLocation albumLocation = new GeoLocation(album.m_Lat, album.m_Lon);
            double distanceInM = GeoFireUtils.getDistanceBetween(albumLocation, queryLocation);

            if (distanceInM >= QueryConstants.MAX_QUERY_RADIUS)
            {
                Log.d(TAG, "Remove album with ID: " + album.m_AlbumID);
                albumIdxToRemove.add(i);
            }
        }

        for (Integer albumToRemove : albumIdxToRemove)
        {
            m_Albums.remove(albumToRemove);
        }
    }
}
