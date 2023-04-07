package com.perception.persep.firebase_helpers;

import android.net.Uri;
import android.util.Log;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.perception.persep.constants.AlbumConstants;
import com.perception.persep.constants.PostConstants;
import com.perception.persep.constants.UserConstants;

import java.util.Collections;
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

    public FirebaseUpdates.UpdatesListener m_Listener = null;

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

    public void AddPost(FirebaseUser user, Uri postImageURI, double lat, double lon)
    {
        Log.d(ADD_NEARBY_TAG, "===== AddPost =====");

        // Get the user data
        m_Firestore.collection(UserConstants.COLLECTION_NAME).document(user.getUid()).get().addOnSuccessListener(userDoc -> {

            // Upload post image to Firestore
            StorageReference postImageFirestoreRef = m_FireStoreRef.child("images/" + UUID.randomUUID().toString());
            postImageFirestoreRef.putFile(postImageURI).addOnSuccessListener(taskSnapshot -> {
                // Get the uploaded image uri
                postImageFirestoreRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Create a Post album with the appropriate data
                    Log.d(ADD_NEARBY_TAG, "Successfully uploaded image: " + uri.toString());
                    TryAddPostToClosestAlbum(userDoc, uri, lat, lon);
                }).addOnFailureListener(e -> {
                    m_Listener.OnUpdateResult(false, e.toString());
                });
            }).addOnFailureListener(e -> {
                m_Listener.OnUpdateResult(false, e.toString());
            });

        }).addOnFailureListener(e -> {
            m_Listener.OnUpdateResult(false, e.toString());
        });
    }

    private void TryAddPostToClosestAlbum(DocumentSnapshot userDoc, Uri postImageURI, double lat, double lon)
    {
        GeoLocation postLocation = new GeoLocation(lat, lon);

        FirebaseQueries firebaseQueries = FirebaseQueries.getInstance();

        List<Task<QuerySnapshot>> queryAlbumTasks = firebaseQueries.GetAlbumQueryTasks(postLocation, null);

        Tasks.whenAllComplete(queryAlbumTasks).addOnCompleteListener(t -> {
            DocumentSnapshot closestAlbum = firebaseQueries.OnQueryClosestAlbum(queryAlbumTasks, postLocation);

            if (closestAlbum == null)
            {
                Log.d(ADD_NEARBY_TAG, "No nearby album, creating a new one...");
                Map<String, Object> albumData = new HashMap<>();
                albumData.put(AlbumConstants.GEO_HASH, GeoFireUtils.getGeoHashForLocation(postLocation));
                albumData.put(AlbumConstants.LAT, lat);
                albumData.put(AlbumConstants.LON, lon);
                albumData.put(AlbumConstants.POSTS, Collections.emptyList());

                m_Firestore.collection(AlbumConstants.COLLECTION_NAME).add(albumData).addOnSuccessListener(albumRef -> {
                    albumRef.get().addOnSuccessListener(albumDoc -> {
                        Log.d(ADD_NEARBY_TAG, "Successfully created new album with id: " + albumDoc.getId());
                        AddPostToAlbum(userDoc, albumDoc, postImageURI);
                    }).addOnFailureListener(e -> {
                        m_Listener.OnUpdateResult(false, e.toString());
                    });
                }).addOnFailureListener(e -> {
                    m_Listener.OnUpdateResult(false, e.toString());
                });
            } else
            {
                Log.d(ADD_NEARBY_TAG, "Found a closest album with id: " + closestAlbum.getId());
                AddPostToAlbum(userDoc, closestAlbum, postImageURI);
            }
        }).addOnFailureListener(e -> {
            m_Listener.OnUpdateResult(false, e.toString());
        });
    }

    private void AddPostToAlbum(DocumentSnapshot userDoc, DocumentSnapshot albumDoc, Uri postImageURI)
    {
        Map<String, Object> postData = new HashMap<>();
        postData.put(PostConstants.ALBUM_ID, albumDoc.getId());
        postData.put(PostConstants.IMAGE_URI, postImageURI.toString());
        postData.put(PostConstants.USER, userDoc.getReference());

        m_Firestore.collection(PostConstants.COLLECTION_NAME).add(postData).addOnSuccessListener(postRef -> {
            Log.d(ADD_NEARBY_TAG, "Successfully created a new post with id: " + postRef.getId());
            albumDoc.getReference().update(AlbumConstants.POSTS, FieldValue.arrayUnion(postRef)).addOnSuccessListener(unused -> {
                Log.d(ADD_NEARBY_TAG, "Successfully linked album against new post");
                m_Listener.OnUpdateResult(true, "Successfully created all data and updates");
            }).addOnFailureListener(e -> {
                m_Listener.OnUpdateResult(false, e.toString());
            });
        }).addOnFailureListener(e -> {
            m_Listener.OnUpdateResult(false, e.toString());
        });
    }
}
