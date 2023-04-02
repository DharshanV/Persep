package com.perception.persep.firebase_helpers;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.perception.persep.album_data.Post;
import com.perception.persep.constants.PostConstants;

public class FirebaseUpdates
{
    private static final String ADD_NEARBY_TAG = "AddGeoqueries";

    private static FirebaseUpdates m_Instance = null;

    private final FirebaseFirestore m_FirebaseDB;

    public FirebaseUpdates()
    {
        m_FirebaseDB = FirebaseFirestore.getInstance();
    }

    public static FirebaseUpdates getInstance()
    {
        if (m_Instance == null) m_Instance = new FirebaseUpdates();
        return m_Instance;
    }


    public void AddPost(Post post, double lat, double lon)
    {
//        Log.d(ADD_NEARBY_TAG, "===== AddPost =====");
//        GeoLocation postLocation = new GeoLocation(lat, lon);
//
//        List<Task<QuerySnapshot>> queryAlbumTasks = GetAlbumQueryTasks(postLocation);
//
//        Tasks.whenAllComplete(queryAlbumTasks).addOnCompleteListener(t -> {
//            DocumentSnapshot closestAlbum = OnQueryClosestAlbum(queryAlbumTasks, postLocation);
//
//            if (closestAlbum == null) {
//                Log.d(ADD_NEARBY_TAG, "No nearby album, creating a new one...");
//                Map<String, Object> data = new HashMap<>();
//
//                m_FirebaseDB.collection(AlbumConstants.COLLECTION_NAME).add(data).addOnSuccessListener(documentReference -> {
//                }).addOnFailureListener(e -> { });
//            }
//            else {
//                Log.d(ADD_NEARBY_TAG, "Found an album, adding post to it...");
//                AddPostToAlbum(closestAlbum, post);
//            }
//        }).addOnFailureListener(e -> { });
    }

    private void AddPostToAlbum(DocumentSnapshot albumDoc, Post post)
    {
        DocumentReference postRef = m_FirebaseDB.collection(PostConstants.COLLECTION_NAME).document(post.m_PostID);
        postRef.set(post.getMap()).addOnSuccessListener(unused -> {

        });
    }
}
