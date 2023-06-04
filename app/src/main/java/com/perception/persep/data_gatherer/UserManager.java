package com.perception.persep.data_gatherer;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.perception.persep.album_data.Post;
import com.perception.persep.constants.PostConstants;
import com.perception.persep.constants.UserConstants;

import java.util.List;

public class UserManager
{
    static private UserManager m_Instance = null;

    public DocumentReference m_DocRef = null;

    public interface DeleteUserListener
    {
        void onDeleteListener(boolean successful);
    }

    private UserManager()
    {
    }

    static public synchronized UserManager getInstance()
    {
        if (m_Instance == null)
        {
            m_Instance = new UserManager();
        }
        return m_Instance;
    }

    public void init()
    {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null)
        {
            m_Instance.m_DocRef = FirebaseFirestore.getInstance().collection(UserConstants.COLLECTION_NAME).document(firebaseUser.getUid());
        }
    }

    public void addPost(String postID)
    {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null)
        {
            Post post = PostManager.getInstance().getPost(postID);
            if (post != null)
            {
                m_DocRef.update(UserConstants.POSTS, FieldValue.arrayUnion(post.m_DocRef));
            }
        }
    }

    public void removePost(DocumentReference postRef)
    {
        m_DocRef.update(UserConstants.POSTS, FieldValue.arrayRemove(postRef));
    }

    public void deleteUser(DeleteUserListener listener)
    {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            m_DocRef.get().addOnSuccessListener(userDoc -> {
                List<DocumentReference> userPostRefs = (List<DocumentReference>) userDoc.get(UserConstants.POSTS);
                if (userPostRefs != null) {
                    for (DocumentReference postRef : userPostRefs) {
                        postRef.get().addOnSuccessListener(postDoc -> {
                            AlbumManager.getInstance().removePost(postDoc);
                        });
                    }
                }
                m_DocRef.delete();
                m_DocRef = null;
                firebaseUser.delete().addOnFailureListener(e -> listener.onDeleteListener(false));
            }).addOnFailureListener(e -> listener.onDeleteListener(false));
        }
    }
}
