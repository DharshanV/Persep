package com.perception.persep.album_data;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.perception.persep.constants.AlbumConstants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class Album
{
    public final List<Post> m_Posts = new ArrayList<>();

    public String m_AlbumID;
    public String m_GeoHash;
    public double m_Lat;
    public double m_Lon;
    public Timestamp m_TimeCreated;
    public Timestamp m_TimeModified;
    private final Set<Integer> m_PendingPostUpdate = new HashSet<>();

    private int m_CurrPostIndex = 0;
    private int m_PrevPostIndex = 0;
    private Timer m_UpdatePostTimer;
    private PostUpdateIndexTask m_PostUpdateTimerTask;
    private final int TASK_START_DELAY = 5000; // 5 secs

    public Album(String albumID, Map<String, Object> data)
    {
        m_AlbumID = albumID;
        m_GeoHash = (String) data.get(AlbumConstants.GEO_HASH);
        m_Lat = (Double) data.get(AlbumConstants.LAT);
        m_Lon = (Double) data.get(AlbumConstants.LON);
        m_TimeCreated = (Timestamp) data.get(AlbumConstants.TIME_CREATED);
        m_TimeModified = (Timestamp) data.get(AlbumConstants.TIME_MODIFIED);

        m_UpdatePostTimer = new Timer();
        m_PostUpdateTimerTask = new PostUpdateIndexTask();
        m_UpdatePostTimer.scheduleAtFixedRate(m_PostUpdateTimerTask, TASK_START_DELAY, TASK_START_DELAY);
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
                "m_Posts=" + m_Posts +
                ", m_AlbumID='" + m_AlbumID + '\'' +
                ", m_GeoHash='" + m_GeoHash + '\'' +
                ", m_Lat=" + m_Lat +
                ", m_Lon=" + m_Lon +
                '}';
    }

    public void AddPost(Post post)
    {
        if (post != null) {
            m_Posts.add(post);
            m_PendingPostUpdate.add(m_Posts.size() - 1);
        }
    }

    public boolean ShouldUpdateTexture()
    {
        if (m_Posts.size() == 1 && m_Posts.get(m_CurrPostIndex).m_Image != null)
        {
            return true;
        }

        if (m_PendingPostUpdate.contains(m_CurrPostIndex))
        {
            m_PendingPostUpdate.remove(m_CurrPostIndex);
            return true;
        }

        if (m_CurrPostIndex != m_PrevPostIndex)
        {
            m_PrevPostIndex = m_CurrPostIndex;
            return true;
        }
        return false;
    }

    public Post getCurrentPost()
    {
        return m_Posts.get(m_CurrPostIndex);
    }

    public void updatePostIndex(boolean next)
    {
        if (next) m_CurrPostIndex = mod(m_CurrPostIndex + 1, m_Posts.size());
        else m_CurrPostIndex = mod(m_CurrPostIndex - 1, m_Posts.size());

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
            m_CurrPostIndex = mod(m_CurrPostIndex + 1, m_Posts.size());
        }
    }
}
