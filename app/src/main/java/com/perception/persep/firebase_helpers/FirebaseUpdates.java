package com.perception.persep.firebase_helpers;

import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.util.Log;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.perception.persep.activities.HomeActivity;
import com.perception.persep.constants.AlbumConstants;
import com.perception.persep.constants.PostConstants;
import com.perception.persep.constants.QueryConstants;
import com.perception.persep.constants.UserConstants;

import java.io.IOException;
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

    public interface PostLikeListener
    {
        void OnLikeUpdate(boolean successful);
    }

    public interface PostDeleteListener
    {
        void OnPostDelete(boolean successful);
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


    public void likePost(FirebaseUser user, String postID, PostLikeListener listener)
    {
        m_Firestore.collection(PostConstants.COLLECTION_NAME).document(postID).get().addOnSuccessListener(postSnapshot -> {
            List<String> postUsersLiked = (List<String>) postSnapshot.get(PostConstants.USERS_LIKED);
            if (postUsersLiked != null && postUsersLiked.contains(user.getUid()))
            {
                return;
            }

            DocumentReference postRef = postSnapshot.getReference();

            WriteBatch batch = m_Firestore.batch();
            postRef.update(PostConstants.LIKES, FieldValue.increment(1));
            postRef.update(PostConstants.USERS_LIKED, FieldValue.arrayUnion(user.getUid()));
            postRef.update(PostConstants.TIME_MODIFIED, Timestamp.now());
            batch.commit()
                    .addOnSuccessListener(unused -> listener.OnLikeUpdate(true))
                    .addOnFailureListener(e -> listener.OnLikeUpdate(false));
        }).addOnFailureListener(Throwable::printStackTrace);
    }

    public void deletePost(FirebaseUser user, String postImageURI, String postID, String albumID, PostDeleteListener listener)
    {
        FirebaseStorage.getInstance().getReferenceFromUrl(postImageURI).delete();

        WriteBatch batch = m_Firestore.batch();

        DocumentReference postRef = m_Firestore.collection(PostConstants.COLLECTION_NAME).document(postID);
        DocumentReference albumRef = m_Firestore.collection(AlbumConstants.COLLECTION_NAME).document(albumID);
        albumRef.update(AlbumConstants.POSTS, FieldValue.arrayRemove(postRef)).addOnSuccessListener(unused -> {
            albumRef.get().addOnSuccessListener(albumDoc -> {
                List<DocumentReference> postRefs = (List<DocumentReference>) albumDoc.get(AlbumConstants.POSTS);
                if (postRefs == null || postRefs.isEmpty()) {
                    albumRef.delete();
                }
            });
        });
        m_Firestore.collection(UserConstants.COLLECTION_NAME).document(user.getUid()).update(UserConstants.POSTS, FieldValue.arrayRemove(postRef));
        postRef.delete();

        batch.commit().addOnSuccessListener(unused -> listener.OnPostDelete(true))
                .addOnFailureListener(unused -> listener.OnPostDelete(false));
    }
}
