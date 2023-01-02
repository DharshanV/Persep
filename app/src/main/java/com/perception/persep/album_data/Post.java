package com.perception.persep.album_data;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.perception.persep.constants.PostConstants;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.HashMap;
import java.util.Map;

public class Post implements Target
{
    public String m_PostID;
    public String m_AlbumID;
    public String m_ImageURI;
    public Bitmap m_Image;

    public Post(String postID)
    {
        m_PostID = postID;
    }

    public Post(String postID, Map<String, Object> data)
    {
        m_PostID = postID;
        m_AlbumID = (String) data.get(PostConstants.ALBUM_ID);
        m_ImageURI = (String) data.get(PostConstants.IMAGE_URI);
        Picasso.get().load(m_ImageURI).resize(200, 200).noFade().into(this);
    }

    public Map<String, Object> getMap()
    {
        Map<String, Object> post = new HashMap<>();
        post.put(PostConstants.ALBUM_ID, m_AlbumID);
        post.put(PostConstants.IMAGE_URI, m_ImageURI);
        return post;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from)
    {
        m_Image = bitmap;
    }

    @Override
    public void onBitmapFailed(Exception e, Drawable errorDrawable)
    {

    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable)
    {

    }

    @Override
    public String toString()
    {
        return "Post{" +
                "m_PostID='" + m_PostID + '\'' +
                ", m_AlbumID='" + m_AlbumID + '\'' +
                ", m_ImageURI='" + m_ImageURI + '\'' +
                ", m_Image=" + m_Image +
                '}';
    }
}
