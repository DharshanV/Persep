package com.perception.persep.album_data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Album implements Parcelable
{
    private List<Post> m_Posts = new ArrayList<>();
    public int postIndex;

    public String m_GeoHash;
    public double m_Lat;
    public double m_Lon;

    public Album(double lat, double lon)
    {
        m_Lat = lat;
        m_Lon = lon;
    }

    public Album(Parcel in)
    {
        m_Lat = in.readDouble();
        m_Lon = in.readDouble();
    }

    public Album(Map<String, Object> data)
    {
        m_GeoHash = (String) data.get("geohash");
        m_Lat = (double) data.get("lat");
        m_Lon = (double) data.get("lon");
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
        user.put("lat", m_Lat);
        user.put("lon", m_Lon);
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
                ", postIndex=" + postIndex +
                ", m_GeoHash='" + m_GeoHash + '\'' +
                ", m_Lat=" + m_Lat +
                ", m_Lon=" + m_Lon +
                '}';
    }

    public void AddPost(Post post)
    {
        m_Posts.add(post);
    }

    public List<Post> GetPosts() { return m_Posts; }
}
