package com.perception.persep.album_data;

import android.content.Context;

import androidx.annotation.NonNull;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.perception.persep.constants.AlbumConstants;
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
    public double m_Lat;
    public double m_Lon;
    public Timestamp m_TimeCreated;
    public Timestamp m_TimeModified;
    public final List<String> m_PostIDs = new ArrayList<>();
    public DocumentReference m_DocRef;

    private Context m_Context;
    private final Set<Integer> m_PendingPostUpdate = new HashSet<>();

    private int m_CurrPostIndex = 0;
    private int m_PrevPostIndex = 0;
    private Timer m_UpdatePostTimer;
    private PostUpdateIndexTask m_PostUpdateTimerTask;
    private final int TASK_START_DELAY = 5000; // 5 secs

    public Album(Context context, double lat, double lon)
    {
        m_Context = context;
        m_Lat = lat;
        m_Lon = lon;
        m_GeoHash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(lat, lon));
        Timestamp now = Timestamp.now();
        m_TimeCreated = now;
        m_TimeModified = now;
        createAlbumServer();

        m_UpdatePostTimer = new Timer();
        m_PostUpdateTimerTask = new PostUpdateIndexTask();
        m_UpdatePostTimer.scheduleAtFixedRate(m_PostUpdateTimerTask, TASK_START_DELAY, TASK_START_DELAY);
    }

    public void remove()
    {

    }

    private void createAlbumServer()
    {
        m_DocRef = FirebaseFirestore.getInstance().collection(AlbumConstants.COLLECTION_NAME).document();
        m_AlbumID = m_DocRef.getId();

        Map<String, Object> albumData = new HashMap<>();
        albumData.put(AlbumConstants.GEO_HASH, GeoFireUtils.getGeoHashForLocation(new GeoLocation(m_Lat, m_Lon)));
        albumData.put(AlbumConstants.GEO_POINT, new GeoPoint(m_Lat, m_Lon));
        albumData.put(AlbumConstants.POSTS, Collections.emptyList());
        albumData.put(AlbumConstants.TIME_CREATED, m_TimeCreated);
        albumData.put(AlbumConstants.TIME_MODIFIED, m_TimeModified);
        m_DocRef.set(albumData);
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
        removePostServer(postID);
    }

    public void removePostServer(String postID)
    {

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
                ", m_Lat=" + m_Lat +
                ", m_Lon=" + m_Lon +
                '}';
    }

    public boolean ShouldUpdateTexture()
    {
//        if (m_Posts.size() == 1 && m_Posts.get(m_CurrPostIndex).m_Image != null)
//        {
//            return true;
//        }
//
//        if (m_PendingPostUpdate.contains(m_CurrPostIndex) && m_Posts.get(m_CurrPostIndex).m_Image != null)
//        {
//            m_PendingPostUpdate.remove(m_CurrPostIndex);
//            return true;
//        }
//
//        if (m_CurrPostIndex != m_PrevPostIndex)
//        {
//            m_PrevPostIndex = m_CurrPostIndex;
//            return true;
//        }
        return false;
    }

    public void updatePostIndex(boolean next)
    {
//        if (next) m_CurrPostIndex = mod(m_CurrPostIndex + 1, m_Posts.size());
//        else m_CurrPostIndex = mod(m_CurrPostIndex - 1, m_Posts.size());
//
//        m_PostUpdateTimerTask.cancel();
//        m_PostUpdateTimerTask = new PostUpdateIndexTask();
//        m_UpdatePostTimer.scheduleAtFixedRate(m_PostUpdateTimerTask, TASK_START_DELAY, TASK_START_DELAY);
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
//            m_CurrPostIndex = mod(m_CurrPostIndex + 1, m_Posts.size());
        }
    }
}
