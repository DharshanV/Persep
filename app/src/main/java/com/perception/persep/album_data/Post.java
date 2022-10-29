package com.perception.persep.album_data;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Post
{
    private Bitmap m_Image;
    private UUID m_UniquePostID;

    public Post(Context context, String imagePath)
    {
        m_UniquePostID = UUID.randomUUID();
    }

    public UUID getUniqueID() { return m_UniquePostID; }

    public Map<String, Object> getMap()
    {
        Map<String, Object> post = new HashMap<>();
        post.put("postID", m_UniquePostID.toString());
        return post;
    }
}
