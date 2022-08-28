package com.perception.persep.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class User implements Parcelable
{
    public String m_UserID;
    public String m_Username;
    public String m_Email;

    public User() {}

    public User(Parcel in)
    {
        m_UserID = in.readString();
        m_Username = in.readString();
        m_Email = in.readString();
    }

    public User(Map<String, Object> data)
    {
        m_UserID = (String) data.get("user_id");
        m_Username = (String) data.get("username");
        m_Email = (String) data.get("email");
    }


    public Map<String, Object> getMap()
    {
        Map<String, Object> user = new HashMap<>();
        user.put("user_id", m_UserID);
        user.put("username", m_Username);
        user.put("email", m_Email);
        return user;
    }

    public static final Creator<User> CREATOR = new Creator<User>()
    {
        @Override
        public User createFromParcel(Parcel in)
        {
            return new User(in);
        }

        @Override
        public User[] newArray(int size)
        {
            return new User[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(m_UserID);
        dest.writeString(m_Email);
        dest.writeString(m_Username);
    }

    @Override
    public String toString()
    {
        return "User {" + String.format("%s, %s, %s", m_UserID, m_Username, m_Email) + "}";
    }
}
