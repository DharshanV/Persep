package com.perception.persep.album_data;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.perception.persep.constants.PostConstants;
import com.perception.persep.firebase_helpers.FirebaseUpdates;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.Map;

public class Post implements Target
{
    private static final String PostLoadImage = "PostLoadImage";

    public String m_PostID;
    public String m_AlbumID;
    public String m_ImageURI;
    public Bitmap m_Image;
    public long m_Likes = 0;
    public String m_GeoLocation;
    public Timestamp m_TimeCreated;
    public Timestamp m_TimeModified;

    public boolean m_IsImageLoaded = false;

    public Post(String postID, Map<String, Object> data, boolean loadNow)
    {
        m_PostID = postID;
        m_AlbumID = (String) data.get(PostConstants.ALBUM_ID);
        m_ImageURI = (String) data.get(PostConstants.IMAGE_URI);
        m_Likes = (Long) data.get(PostConstants.LIKES);
        m_GeoLocation = (String) data.get(PostConstants.GEO_LOCATION);
        m_TimeCreated = (Timestamp) data.get(PostConstants.TIME_CREATED);
        m_TimeModified = (Timestamp) data.get(PostConstants.TIME_MODIFIED);

        if (loadNow && !m_ImageURI.isEmpty())
        {
            Log.d(PostLoadImage, "Loading image to post: " + m_ImageURI);
            Picasso.get().load(m_ImageURI).resize(PostConstants.IMAGE_WIDTH, PostConstants.IMAGE_HEIGHT).noFade().into(this);
        }
    }

    public void loadNow(ImageView imageView)
    {
        Log.d(PostLoadImage, "Loading image to imageview: " + m_ImageURI);
        Picasso.get().load(m_ImageURI).resize(PostConstants.IMAGE_WIDTH, PostConstants.IMAGE_HEIGHT).into(imageView);
    }

    public void like(FirebaseUser user, FirebaseUpdates.PostLikeListener listener)
    {
        FirebaseUpdates.getInstance().likePost(user, m_PostID, listener);
    }

    public void delete(FirebaseUser user, FirebaseUpdates.PostDeleteListener listener)
    {
        FirebaseUpdates.getInstance().deletePost(user, m_ImageURI, m_PostID, m_AlbumID, listener);
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from)
    {
        Log.d(PostLoadImage, "Image loaded from: " + from.toString() + " with: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        m_IsImageLoaded = true;
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

    @NonNull
    @Override
    public String toString()
    {
        return "Post{" +
                "m_PostID='" + m_PostID + '\'' +
                ", m_AlbumID='" + m_AlbumID + '\'' +
                ", m_ImageURI='" + m_ImageURI + '\'' +
                ", m_Image=" + m_Image + '\'' +
                ", m_Likes=" + m_Likes + '\'' +
                "  m_GeoLocation" + m_GeoLocation +
                '}';
    }
}
