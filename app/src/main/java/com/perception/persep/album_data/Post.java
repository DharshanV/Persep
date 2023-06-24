package com.perception.persep.album_data;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.perception.persep.activities.HomeActivity;
import com.perception.persep.constants.PostConstants;
import com.perception.persep.data_gatherer.CommentManager;
import com.perception.persep.data_gatherer.PostManager;
import com.perception.persep.data_gatherer.UserManager;
import com.perception.persep.enums.PostError;
import com.perception.persep.user_data.User;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import timber.log.Timber;

public class Post implements Target
{
    private static final String PostLoadImage = "PostLoadImage";
    private final Stack<ImageView> m_LoadIntoViews = new Stack<>();
    public String m_PostID;
    public String m_AlbumID;
    public Uri m_ImageURI;
    public Bitmap m_Image;
    public long m_Likes;
    public DocumentReference m_UserRef;
    public User m_UserData;
    public List<DocumentReference> m_UsersLiked;
    public GeoPoint m_GeoPoint;
    public String m_GeoLocation;
    public Timestamp m_TimeCreated;
    public Timestamp m_TimeModified;
    public DocumentReference m_DocRef;
    public boolean m_IsImageLoaded = false;
    public List<DocumentReference> m_PostComments = new ArrayList<>();

    public Post(Uri postImageURI, String albumID, double lat, double lon)
    {
        m_AlbumID = albumID;
        m_ImageURI = postImageURI;
        m_Likes = 0;
        m_UserRef = UserManager.getInstance().m_DocRef;
        UserManager.getInstance().getUserData(m_UserRef, user -> m_UserData = user);
        m_UsersLiked = new ArrayList<>();
        m_GeoPoint = new GeoPoint(lat, lon);
        m_GeoLocation = GetPostGeoLocation(m_GeoPoint);
        Timestamp now = Timestamp.now();
        m_TimeCreated = now;
        m_TimeModified = now;
        m_Image = PostManager.getInstance().getPostLoadingImage();
        createPostServer();
    }

    public Post(DocumentSnapshot postDoc) throws NullPointerException
    {
        m_PostID = postDoc.getId();
        m_Image = PostManager.getInstance().getPostLoadingImage();
        m_DocRef = postDoc.getReference();

        m_AlbumID = (String) postDoc.get(PostConstants.ALBUM_ID);
        m_ImageURI = Uri.parse((String) postDoc.get(PostConstants.IMAGE_URI));
        m_Likes = (long) postDoc.get(PostConstants.LIKES);
        m_UserRef = (DocumentReference) postDoc.get(PostConstants.USER);
        UserManager.getInstance().getUserData(m_UserRef, user -> m_UserData = user);
        m_UsersLiked = new ArrayList<>((List<DocumentReference>) postDoc.get(PostConstants.USERS_LIKED));
        m_GeoPoint = (GeoPoint) postDoc.get(PostConstants.GEO_POINT);
        m_GeoLocation = (String) postDoc.get(PostConstants.GEO_LOCATION);
        m_TimeCreated = (Timestamp) postDoc.get(PostConstants.TIME_CREATED);
        m_TimeModified = (Timestamp) postDoc.get(PostConstants.TIME_MODIFIED);

        Object commentObj = postDoc.get(PostConstants.COMMENTS);
        if (commentObj != null) {
            m_PostComments = new ArrayList<>((List<DocumentReference>) commentObj);
        }

        Picasso.get().load(m_ImageURI).resize(PostConstants.IMAGE_WIDTH, PostConstants.IMAGE_HEIGHT).noFade().into(this);
    }

    private void createPostServer()
    {
        m_DocRef = FirebaseFirestore.getInstance().collection(PostConstants.COLLECTION_NAME).document();
        m_PostID = m_DocRef.getId();

        Map<String, Object> postData = new HashMap<>();
        postData.put(PostConstants.ALBUM_ID, m_AlbumID);
        postData.put(PostConstants.USER, m_UserRef);
        postData.put(PostConstants.LIKES, m_Likes);
        postData.put(PostConstants.USERS_LIKED, m_UsersLiked);
        postData.put(PostConstants.GEO_POINT, m_GeoPoint);
        postData.put(PostConstants.GEO_LOCATION, m_GeoLocation);
        postData.put(PostConstants.TIME_CREATED, m_TimeCreated);
        postData.put(PostConstants.TIME_MODIFIED, m_TimeModified);
        postData.put(PostConstants.COMMENTS, m_PostComments);
        m_DocRef.set(postData).addOnFailureListener(e -> PostManager.getInstance().onPostError(m_PostID, PostError.SET_DOCUMENT_REF));

        StorageReference postImageRef = FirebaseStorage.getInstance().getReference().child("images/" + UUID.randomUUID().toString() + ".jpg");
        postImageRef.putFile(m_ImageURI).addOnSuccessListener(taskSnapshot -> {
            postImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                m_ImageURI = uri;
                Picasso.get().load(m_ImageURI).resize(PostConstants.IMAGE_WIDTH, PostConstants.IMAGE_HEIGHT).noFade().into(this);
                m_DocRef.update(PostConstants.IMAGE_URI, m_ImageURI).addOnFailureListener(e -> PostManager.getInstance().onPostError(m_PostID, PostError.UPDATE_IMAGE_URI));
            }).addOnFailureListener(e -> PostManager.getInstance().onPostError(m_PostID, PostError.PUT_FILE_STORAGE_REF));
        }).addOnFailureListener(e -> PostManager.getInstance().onPostError(m_PostID, PostError.PUT_FILE_STORAGE_REF));
    }

    public void serverRefresh(PostRefreshListener listener)
    {
        m_DocRef.get().addOnSuccessListener(postDoc -> {
            Timestamp postTimeModified = (Timestamp) postDoc.get(PostConstants.TIME_MODIFIED);
            boolean timeDiffFromServer = postTimeModified != m_TimeModified;
            if (timeDiffFromServer)
            {
                m_TimeModified = postTimeModified;
                m_Likes = (long) postDoc.get(PostConstants.LIKES);
                Object commentObj = postDoc.get(PostConstants.COMMENTS);
                if (commentObj != null) {
                    m_PostComments = new ArrayList<>((List<DocumentReference>) commentObj);
                }
            }
            listener.onPostRefresh(timeDiffFromServer);
        }).addOnFailureListener(e -> listener.onPostRefresh(false));
    }

    public void remove()
    {
        StorageReference postImageRef = FirebaseStorage.getInstance().getReferenceFromUrl(m_ImageURI.toString());
        postImageRef.delete();
        m_Image = null;
        m_DocRef.delete();

        for (DocumentReference commentRef : m_PostComments)
        {
            CommentManager.getInstance().removeComment(commentRef);
        }
    }

    public void unload()
    {
        m_Image.recycle();
        m_Image = null;
    }

    public void loadInto(ImageView imageView)
    {
        imageView.setImageBitmap(m_Image);
        if (!m_IsImageLoaded)
        {
            m_LoadIntoViews.add(imageView);
        }
    }

    public void like(PostManager.PostLikeListener listener)
    {
        DocumentReference userRef = UserManager.getInstance().m_DocRef;
        if (userRef == null || m_UsersLiked.contains(userRef))
        {
            return;
        }

        m_Likes++;
        m_TimeModified = Timestamp.now();
        m_UsersLiked.add(userRef);

        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        m_DocRef.update(PostConstants.LIKES, m_Likes);
        m_DocRef.update(PostConstants.TIME_MODIFIED, m_TimeModified);
        m_DocRef.update(PostConstants.USERS_LIKED, FieldValue.arrayUnion(userRef));
        batch.commit().addOnSuccessListener(unused -> listener.onLikeUpdate(true));
    }

    public void unlike(PostManager.PostLikeListener listener)
    {
        DocumentReference userRef = UserManager.getInstance().m_DocRef;
        if (userRef == null || !m_UsersLiked.contains(userRef))
        {
            return;
        }

        m_Likes--;
        if (m_Likes < 0) {
            m_Likes = 0;
        }
        m_TimeModified = Timestamp.now();
        m_UsersLiked.remove(userRef);

        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        m_DocRef.update(PostConstants.LIKES, m_Likes);
        m_DocRef.update(PostConstants.TIME_MODIFIED, m_TimeModified);
        m_DocRef.update(PostConstants.USERS_LIKED, FieldValue.arrayRemove(userRef));
        batch.commit().addOnSuccessListener(unused -> listener.onLikeUpdate(true));
    }

    public String addComment(String commentStr)
    {
        String commentID = CommentManager.getInstance().addComment(m_PostID, commentStr);
        Comment comment = CommentManager.getInstance().getComment(commentID);
        m_TimeModified = Timestamp.now();
        m_PostComments.add(comment.m_DocRef);

        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        m_DocRef.update(PostConstants.TIME_MODIFIED, m_TimeModified);
        m_DocRef.update(PostConstants.COMMENTS, FieldValue.arrayUnion(comment.m_DocRef));
        batch.commit();
        return commentID;
    }

    public boolean isLikedByCurrentUser()
    {
        for (DocumentReference userRef : m_UsersLiked) {
            if (userRef.equals(UserManager.getInstance().m_DocRef)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCurrentUserPost()
    {
        return m_UserRef == UserManager.getInstance().m_DocRef;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from)
    {
        m_IsImageLoaded = true;
        m_Image = bitmap;

        while (!m_LoadIntoViews.empty())
        {
            m_LoadIntoViews.pop().setImageBitmap(m_Image);
        }
    }

    @Override
    public void onBitmapFailed(Exception e, Drawable errorDrawable)
    {
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

    private String GetPostGeoLocation(GeoPoint geoPoint)
    {
        Geocoder geocoder = new Geocoder(HomeActivity.getHomeActivityContext());
        try
        {
            List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
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

    public interface PostRefreshListener
    {
        void onPostRefresh(boolean refreshedFromServer);
    }
}
