package com.perception.persep.album_data;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.perception.persep.constants.PostConstants;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.HashMap;
import java.util.Map;

public class Post implements Target
{
    private static final String PostLoadImage = "PostLoadImage";

    public String m_PostID;
    public String m_AlbumID;
    public String m_ImageURI;
    public Bitmap m_Image;
    public boolean m_IsLoadingImage = true;

    public Post(String postID)
    {
        m_PostID = postID;
    }

    public Post(String postID, Map<String, Object> data)
    {
        m_PostID = postID;
        m_AlbumID = (String) data.get(PostConstants.ALBUM_ID);
        m_ImageURI = (String) data.get(PostConstants.IMAGE_URI);

        Log.d(PostLoadImage, "Loading image: " + m_ImageURI);
        Picasso.get().load(m_ImageURI).resize(PostConstants.IMAGE_WIDTH, PostConstants.IMAGE_HEIGHT).noFade().into(this);
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
        Log.d(PostLoadImage, "Image loaded: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        m_IsLoadingImage = false;
        m_Image = bitmap;
    }

    @Override
    public void onBitmapFailed(Exception e, Drawable errorDrawable)
    {
        Log.d(PostLoadImage, "Couldn't load image: " + e.toString());
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
