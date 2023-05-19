package com.perception.persep.album_data;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.perception.persep.activities.HomeActivity;
import com.perception.persep.constants.AlbumConstants;
import com.perception.persep.constants.PostConstants;
import com.perception.persep.data_gatherer.PostManager;
import com.perception.persep.firebase_helpers.FirebaseUpdates;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Post implements Target
{
    private static final String PostLoadImage = "PostLoadImage";

    public String m_PostID;
    public String m_AlbumID;
    public Uri m_ImageURI;
    public Bitmap m_Image;
    public long m_Likes = 0;
    public double m_Lat, m_Lon;
    public String m_GeoLocation;
    public Timestamp m_TimeCreated;
    public Timestamp m_TimeModified;
    public DocumentReference m_DocRef;

    public boolean m_IsImageLoaded = false;
    private PostManager.AddPostListener m_RemovePostListener = null;

    public Post(Uri postImageURI, String albumID, double lat, double lon, boolean loadNow)
    {
        m_AlbumID = albumID;
        m_ImageURI = postImageURI;
        m_Likes = 0;
        m_Lat = lat;
        m_Lon = lon;
        m_GeoLocation = "";
        Timestamp now = Timestamp.now();
        m_TimeCreated = now;
        m_TimeModified = now;
        createPostServer();

        Log.d(PostLoadImage, "Loading image to post: " + postImageURI);
        Picasso.get().load(m_ImageURI).resize(PostConstants.IMAGE_WIDTH, PostConstants.IMAGE_HEIGHT).noFade().into(this);
    }

    private void createPostServer()
    {
        m_DocRef = FirebaseFirestore.getInstance().collection(PostConstants.COLLECTION_NAME).document();
        m_PostID = m_DocRef.getId();

        Map<String, Object> postData = new HashMap<>();
        postData.put(PostConstants.ALBUM_ID, m_AlbumID);
//        postData.put(PostConstants.USER, User.getInstance().m_DocRef);
        postData.put(PostConstants.LIKES, m_Likes);
        postData.put(PostConstants.USERS_LIKED, Collections.emptyList());
        postData.put(PostConstants.GEO_POINT, new GeoPoint(m_Lat, m_Lon));
        postData.put(PostConstants.GEO_LOCATION, GetPostGeoLocation(m_Lat, m_Lon));
        Timestamp now = Timestamp.now();
        postData.put(PostConstants.TIME_CREATED, now);
        postData.put(PostConstants.TIME_MODIFIED, now);
        m_DocRef.set(postData);

//        StorageReference postImageRef = FirebaseStorage.getInstance().getReference().child("images/" + UUID.randomUUID().toString() + ".jpg");
//        postImageRef.putFile(m_ImageURI).addOnSuccessListener(taskSnapshot -> {
//            postImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
//                m_DocRef.update(PostConstants.IMAGE_URI, uri);
//            });
//        });
    }

    public Post(String postID, Map<String, Object> data, boolean loadNow)
    {
        m_PostID = postID;
        m_AlbumID = (String) data.get(PostConstants.ALBUM_ID);
        m_ImageURI = (Uri) data.get(PostConstants.IMAGE_URI);
        m_Likes = (Long) data.get(PostConstants.LIKES);
        m_GeoLocation = (String) data.get(PostConstants.GEO_LOCATION);
        m_TimeCreated = (Timestamp) data.get(PostConstants.TIME_CREATED);
        m_TimeModified = (Timestamp) data.get(PostConstants.TIME_MODIFIED);

        if (loadNow && !m_ImageURI.toString().isEmpty())
        {
            Log.d(PostLoadImage, "Loading image to post: " + m_ImageURI);
            Picasso.get().load(m_ImageURI).resize(PostConstants.IMAGE_WIDTH, PostConstants.IMAGE_HEIGHT).noFade().into(this);
        }
    }

    public void remove(PostManager.RemovePostListener listener)
    {

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
//        FirebaseUpdates.getInstance().deletePost(user, m_ImageURI, m_PostID, m_AlbumID, listener);
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

    private String GetPostGeoLocation(double lat, double lon)
    {
        Geocoder geocoder = new Geocoder(HomeActivity.getHomeActivityContext());
        try
        {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (!addresses.isEmpty())
            {
                Address address = addresses.get(0);
                String city = address.getLocality() != null ? address.getLocality() : "";
                String state = address.getAdminArea() != null ? address.getAdminArea() : "";
                if (state.isEmpty()) return city;
                else return city + ", " + state;
            }
        } catch (IOException e)
        {
            return "Unknown";
        }
        return "Unknown";
    }
}
