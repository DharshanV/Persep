package com.perception.persep.album_data;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Post implements Target
{
    private UUID m_PostID;
    public String m_AlbumID;
    public String m_ImageURI;
    public Bitmap m_Image;

    public Post()
    {
        m_PostID = UUID.randomUUID();
    }

    public Post(Map<String, Object> data)
    {
        m_AlbumID = (String) data.get("album_id");
        m_ImageURI = (String) data.get("image_uri");
        Picasso.get().load(m_ImageURI).resize(200, 200).noFade().into(this);
    }

    public UUID getPostID() { return m_PostID; }

    public Map<String, Object> getMap()
    {
        Map<String, Object> post = new HashMap<>();
        post.put("post_id", m_PostID.toString());
        post.put("album_id",m_AlbumID);
        post.put("image_uri", m_ImageURI);
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
}
