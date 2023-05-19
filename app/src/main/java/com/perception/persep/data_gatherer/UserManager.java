package com.perception.persep.data_gatherer;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.perception.persep.album_data.Post;
import com.perception.persep.constants.AlbumConstants;
import com.perception.persep.constants.UserConstants;

public class UserManager
{
    static private UserManager m_Instance = null;

    private final Context m_Context;
    public DocumentReference m_DocRef;

    private UserManager(Context context)
    {
        m_Context = context;
    }

    static public synchronized UserManager getInstance(Context context)
    {
        if (m_Instance == null) {
            m_Instance = new UserManager(context.getApplicationContext());
        }
        return m_Instance;
    }

    static public UserManager getInstance()
    {
        return m_Instance;
    }

    public void create()
    {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            m_DocRef = FirebaseFirestore.getInstance().collection(UserConstants.COLLECTION_NAME).document(firebaseUser.getUid());
        }
    }

    public void addPost(String postID)
    {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            Post post = PostManager.getInstance(m_Context).getPost(postID);
            if (post != null) {
                m_DocRef.update(UserConstants.POSTS, FieldValue.arrayUnion(post.m_DocRef));
            }
        }
    }

    public void removePost(DocumentReference postRef)
    {
        m_DocRef.update(AlbumConstants.POSTS, FieldValue.arrayRemove(postRef));
    }
}
