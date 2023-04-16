package com.perception.persep.album_data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;
import com.perception.persep.constants.AlbumConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class Album implements Parcelable
{
    public final List<Post> m_Posts = new ArrayList<>();

    public String m_AlbumID;
    public String m_GeoHash;
    public double m_Lat;
    public double m_Lon;
    public Timestamp m_TimeCreated;
    public Timestamp m_TimeModified;
    private Set<Integer> m_PendingPostUpdate = new HashSet<>();

    private int m_CurrPostIndex = 0;
    private int m_PrevPostIndex = 0;
    private Timer m_UpdatePostTimer;
    private PostUpdateIndexTask m_PostUpdateTimerTask;
    private final int TASK_START_DELAY = 5000; // 5 secs

    public Album(String albumID, double lat, double lon)
    {
        m_AlbumID = albumID;
        m_Lat = lat;
        m_Lon = lon;
    }

    public Album(Parcel in)
    {
        m_Lat = in.readDouble();
        m_Lon = in.readDouble();
    }

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
        Album otherAlbum = (Album) obj;
        return m_AlbumID.equals(otherAlbum.m_AlbumID);
    }

    @Override
    public int hashCode()
    {
        return m_AlbumID.hashCode();
    }

    public static final Creator<Album> CREATOR = new Creator<Album>()
    {
        @Override
        public Album createFromParcel(Parcel in)
        {
            return new Album(in);
        }

        @Override
        public Album[] newArray(int size)
        {
            return new Album[size];
        }
    };

    public Map<String, Object> getMap()
    {
        Map<String, Object> user = new HashMap<>();
        user.put(AlbumConstants.LAT, m_Lat);
        user.put(AlbumConstants.LON, m_Lon);
        return user;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeDouble(m_Lat);
        dest.writeDouble(m_Lon);
    }

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
        m_Posts.add(post);
        m_PendingPostUpdate.add(m_Posts.size() - 1);
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

    int mod(int a, int b)
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
