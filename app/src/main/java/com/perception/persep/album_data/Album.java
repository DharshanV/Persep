package com.perception.persep.album_data;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.WriteBatch;
import com.perception.persep.constants.AlbumConstants;
import com.perception.persep.constants.PostConstants;
import com.perception.persep.data_gatherer.PostManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class Album
{
    public String m_AlbumID;
    public String m_GeoHash;
    public GeoPoint m_GeoPoint;
    public Timestamp m_TimeCreated;
    public Timestamp m_TimeModified;
    public final List<String> m_PostIDs = new ArrayList<>();
    public DocumentReference m_DocRef;

    private Context m_Context;
    private final Set<Integer> m_PendingPostUpdate = new HashSet<>();

    private int m_CurrPostIndex = 0;
    private int m_PrevPostIndex = 0;
    private final Timer m_UpdatePostTimer;
    private PostUpdateIndexTask m_PostUpdateTimerTask;
    private final int TASK_START_DELAY = 5000; // 5 secs

    public Album(Context context, double lat, double lon)
    {
        m_Context = context;
        m_GeoPoint = new GeoPoint(lat, lon);
        m_GeoHash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(lat, lon));
        Timestamp now = Timestamp.now();
        m_TimeCreated = now;
        m_TimeModified = now;
        createAlbumServer();

        m_UpdatePostTimer = new Timer();
        m_PostUpdateTimerTask = new PostUpdateIndexTask();
        m_UpdatePostTimer.scheduleAtFixedRate(m_PostUpdateTimerTask, TASK_START_DELAY, TASK_START_DELAY);
    }

    private void createAlbumServer()
    {
        m_DocRef = FirebaseFirestore.getInstance().collection(AlbumConstants.COLLECTION_NAME).document();
        m_AlbumID = m_DocRef.getId();

        Map<String, Object> albumData = new HashMap<>();
        albumData.put(AlbumConstants.GEO_HASH, m_GeoHash);
        albumData.put(AlbumConstants.GEO_POINT, m_GeoPoint);
        albumData.put(AlbumConstants.POSTS, Collections.emptyList());
        albumData.put(AlbumConstants.TIME_CREATED, m_TimeCreated);
        albumData.put(AlbumConstants.TIME_MODIFIED, m_TimeModified);
        m_DocRef.set(albumData);
    }

    public Album(DocumentSnapshot albumDoc)
    {
        m_AlbumID = albumDoc.getId();
        m_GeoPoint = (GeoPoint) albumDoc.get(AlbumConstants.GEO_POINT);
        m_GeoHash = (String) albumDoc.get(AlbumConstants.GEO_HASH);
        m_TimeCreated = (Timestamp) albumDoc.get(AlbumConstants.TIME_CREATED);
        m_TimeModified = (Timestamp) albumDoc.get(AlbumConstants.TIME_MODIFIED);
        m_DocRef = albumDoc.getReference();

        List<DocumentReference> postRefs = (List<DocumentReference>) albumDoc.get(AlbumConstants.POSTS);
        if (postRefs != null) {
            for (DocumentReference postRef : postRefs)
            {
                PostManager.getInstance().addPost(postRef, (success, message, postID) -> {
                    // TODO: write lock
                    m_PostIDs.add(postID);
                });
            }
        }

        m_UpdatePostTimer = new Timer();
        m_PostUpdateTimerTask = new PostUpdateIndexTask();
        m_UpdatePostTimer.scheduleAtFixedRate(m_PostUpdateTimerTask, TASK_START_DELAY, TASK_START_DELAY);
    }

    public void remove()
    {
        for (String postID : m_PostIDs) {
            Post post = PostManager.getInstance().getPost(postID);
            if (post != null) {
                post.remove();
            }
        }
        m_DocRef.delete();
    }

    public void addPost(String postID)
    {
        m_PostIDs.add(postID);
        addPostServer(postID);
    }

    private void addPostServer(String postID)
    {
        Post post = PostManager.getInstance(m_Context).getPost(postID);
        if (post != null) {
            m_DocRef.update(AlbumConstants.POSTS, FieldValue.arrayUnion(post.m_DocRef));
        }
    }

    public void removePost(String postID)
    {
        m_PostIDs.remove(postID);
        m_TimeModified = Timestamp.now();

        Post post = PostManager.getInstance(m_Context).getPost(postID);
        if (post != null) {
            post.remove();
            WriteBatch batch = FirebaseFirestore.getInstance().batch();
            m_DocRef.update(AlbumConstants.TIME_MODIFIED, m_TimeModified);
            m_DocRef.update(AlbumConstants.POSTS, FieldValue.arrayRemove(post.m_DocRef));
            batch.commit();
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (getClass() != obj.getClass()) return false;
        Album otherAlbum = (Album) obj;
        return m_AlbumID.equals(otherAlbum.m_AlbumID);
    }

    @Override
    public int hashCode()
    {
        return m_AlbumID.hashCode();
    }

    @NonNull
    @Override
    public String toString()
    {
        return "Album{" +
                "m_Posts=" + m_PostIDs +
                ", m_AlbumID='" + m_AlbumID + '\'' +
                ", m_GeoHash='" + m_GeoHash + '\'' +
                ", m_GeoPoint=" + m_GeoPoint +
                '}';
    }

    public boolean ShouldUpdateTexture()
    {
        if (m_PostIDs.size() == 1)
        {
            Post post = PostManager.getInstance().getPost(m_PostIDs.get(0));
            return post.m_Image != null;
        }

        Post post = PostManager.getInstance().getPost(getCurrentPostID());
        if (m_PendingPostUpdate.contains(m_CurrPostIndex) && post.m_Image != null)
        {
            m_PendingPostUpdate.remove(m_CurrPostIndex);
            return true;
        }

        if (m_CurrPostIndex != m_PrevPostIndex)
        {
            m_PrevPostIndex = m_CurrPostIndex;
            return true;
        }
        return true;
    }

    public String getCurrentPostID()
    {
        if (!m_PostIDs.isEmpty() && m_CurrPostIndex < m_PostIDs.size()) {
            return m_PostIDs.get(m_CurrPostIndex);
        }
        return null;
    }

    public void updatePostIndex(boolean next)
    {
        if (next) m_CurrPostIndex = mod(m_CurrPostIndex + 1, m_PostIDs.size());
        else m_CurrPostIndex = mod(m_CurrPostIndex - 1, m_PostIDs.size());

        m_PostUpdateTimerTask.cancel();
        m_PostUpdateTimerTask = new PostUpdateIndexTask();
        m_UpdatePostTimer.scheduleAtFixedRate(m_PostUpdateTimerTask, TASK_START_DELAY, TASK_START_DELAY);
    }

    static int mod(int a, int b)
    {
        int ret = a % b;
        if (ret < 0) ret += b;
        return ret;
    }

    class PostUpdateIndexTask extends TimerTask
    {
        @Override
        public void run()
        {
            if (m_PostIDs.isEmpty()) {
                m_CurrPostIndex = 0;
                return;
            }
            m_CurrPostIndex = mod(m_CurrPostIndex + 1, m_PostIDs.size());
        }
    }
}
