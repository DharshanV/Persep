package com.perception.persep.firebase_helpers;

import android.net.Uri;
import android.util.Log;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.perception.persep.constants.AlbumConstants;
import com.perception.persep.constants.PostConstants;
import com.perception.persep.constants.QueryConstants;
import com.perception.persep.constants.UserConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseUpdates
{
    private static final String ADD_NEARBY_TAG = "AddGeoqueries";

    private static FirebaseUpdates m_Instance = null;

    public interface UpdatesListener
    {
        void OnUpdateResult(boolean successful, String message);
    }

    public interface PostLikeListener
    {
        void OnLikeUpdate(boolean successful);
    }

    private final FirebaseFirestore m_Firestore;
    private final StorageReference m_FireStoreRef;

    public FirebaseUpdates()
    {
        m_Firestore = FirebaseFirestore.getInstance();
        m_FireStoreRef = FirebaseStorage.getInstance().getReference();
    }

    public static FirebaseUpdates getInstance()
    {
        if (m_Instance == null) m_Instance = new FirebaseUpdates();
        return m_Instance;
    }

    public void AddPost(FirebaseUser user, Uri postImageURI, double lat, double lon, UpdatesListener listener)
    {
        Log.d(ADD_NEARBY_TAG, "===== AddPost =====");

        // Get the user data
        m_Firestore.collection(UserConstants.COLLECTION_NAME).document(user.getUid()).get().addOnSuccessListener(userDoc -> {

            // Upload post image to Firestore
            StorageReference postImageFirestoreRef = m_FireStoreRef.child("images/" + UUID.randomUUID().toString() + ".jpg");
            postImageFirestoreRef.putFile(postImageURI).addOnSuccessListener(taskSnapshot -> {
                // Get the uploaded image uri
                postImageFirestoreRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Create a Post album with the appropriate data
                    Log.d(ADD_NEARBY_TAG, "Successfully uploaded image: " + uri.toString());
                    TryAddPostToClosestAlbum(userDoc, uri, lat, lon, listener);
                }).addOnFailureListener(e -> {
                    listener.OnUpdateResult(false, e.toString());
                });
            }).addOnFailureListener(e -> {
                listener.OnUpdateResult(false, e.toString());
            });

        }).addOnFailureListener(e -> {
            listener.OnUpdateResult(false, e.toString());
        });
    }

    public void likePost(FirebaseUser user, String postID, PostLikeListener listener)
    {
        m_Firestore.collection(PostConstants.COLLECTION_NAME).document(postID).get().addOnSuccessListener(postSnapshot -> {
            List<String> postUsersLiked = (List<String>) postSnapshot.get(PostConstants.USERS_LIKED);
            if (postUsersLiked != null && postUsersLiked.contains(user.getUid()))
            {
                return;
            }

            WriteBatch batch = m_Firestore.batch();
            DocumentReference postRef = postSnapshot.getReference();
            postRef.update(PostConstants.LIKES, FieldValue.increment(1));
            postRef.update(PostConstants.USERS_LIKED, FieldValue.arrayUnion(user.getUid()));
            batch.commit()
                    .addOnSuccessListener(unused -> listener.OnLikeUpdate(true))
                    .addOnFailureListener(e -> listener.OnLikeUpdate(false));
        }).addOnFailureListener(Throwable::printStackTrace);
    }

    private void TryAddPostToClosestAlbum(DocumentSnapshot userDoc, Uri postImageURI, double lat, double lon, UpdatesListener listener)
    {
        GeoLocation postLocation = new GeoLocation(lat, lon);

        FirebaseQueries firebaseQueries = FirebaseQueries.getInstance();

        List<Task<QuerySnapshot>> queryAlbumTasks = firebaseQueries.GetAlbumQueryTasks(postLocation, null, QueryConstants.MAX_ADD_POST_RADIUS);

        Tasks.whenAllComplete(queryAlbumTasks).addOnCompleteListener(t -> {
            DocumentSnapshot closestAlbum = firebaseQueries.OnQueryClosestAlbum(queryAlbumTasks, postLocation, QueryConstants.MAX_ADD_POST_RADIUS);

            if (closestAlbum == null)
            {
                Log.d(ADD_NEARBY_TAG, "No nearby album, creating a new one...");
                Map<String, Object> albumData = new HashMap<>();
                albumData.put(AlbumConstants.GEO_HASH, GeoFireUtils.getGeoHashForLocation(postLocation));
                albumData.put(AlbumConstants.LAT, lat);
                albumData.put(AlbumConstants.LON, lon);
                AddPostToNewAlbum(userDoc, albumData, postImageURI, listener);
            } else
            {
                Log.d(ADD_NEARBY_TAG, "Found a closest album with id: " + closestAlbum.getId());
                AddPostToAlbum(userDoc, closestAlbum, postImageURI, listener);
            }
        }).addOnFailureListener(e -> {
            listener.OnUpdateResult(false, e.toString());
        });
    }

    private void AddPostToAlbum(DocumentSnapshot userDoc, DocumentSnapshot albumDoc, Uri postImageURI, UpdatesListener listener)
    {
        Map<String, Object> postData = new HashMap<>();
        postData.put(PostConstants.ALBUM_ID, albumDoc.getId());
        postData.put(PostConstants.IMAGE_URI, postImageURI.toString());
        postData.put(PostConstants.USER, userDoc.getReference());

        m_Firestore.collection(PostConstants.COLLECTION_NAME).add(postData).addOnSuccessListener(postRef -> {
            Log.d(ADD_NEARBY_TAG, "Successfully created a new post with id: " + postRef.getId());
            albumDoc.getReference().update(AlbumConstants.POSTS, FieldValue.arrayUnion(postRef)).addOnSuccessListener(unused -> {
                Log.d(ADD_NEARBY_TAG, "Successfully linked album against new post");
                listener.OnUpdateResult(true, "Successfully created all data and updates");
            }).addOnFailureListener(e -> {
                listener.OnUpdateResult(false, e.toString());
            });
            userDoc.getReference().update(UserConstants.POSTS, FieldValue.arrayUnion(postRef));
        }).addOnFailureListener(e -> {
            listener.OnUpdateResult(false, e.toString());
        });
    }

    private void AddPostToNewAlbum(DocumentSnapshot userDoc, Map<String, Object> albumData, Uri postImageURI, UpdatesListener listener)
    {
        DocumentReference albumRef = m_Firestore.collection(AlbumConstants.COLLECTION_NAME).document();

        Map<String, Object> postData = new HashMap<>();
        postData.put(PostConstants.ALBUM_ID, albumRef.getId());
        postData.put(PostConstants.IMAGE_URI, postImageURI.toString());
        postData.put(PostConstants.USER, userDoc.getReference());

        m_Firestore.collection(PostConstants.COLLECTION_NAME).add(postData).addOnSuccessListener(postRef -> {
            Log.d(ADD_NEARBY_TAG, "Successfully created a new post with id: " + postRef.getId());
            albumData.put(AlbumConstants.POSTS, FieldValue.arrayUnion(postRef));
            albumRef.set(albumData).addOnSuccessListener(unused -> {
                Log.d(ADD_NEARBY_TAG, "Successfully created new album with id: " + albumRef.getId());
                listener.OnUpdateResult(true, "Successfully created all data and updates");
            }).addOnFailureListener(e -> {
                listener.OnUpdateResult(false, e.toString());
            });
            userDoc.getReference().update(UserConstants.POSTS, FieldValue.arrayUnion(postRef));
        }).addOnFailureListener(e -> {
            listener.OnUpdateResult(false, e.toString());
        });
    }
}
