package com.perception.persep.data_gatherer;

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

    public DocumentReference m_DocRef;

    private UserManager()
    {
    }

    static public synchronized UserManager getInstance()
    {
        if (m_Instance == null)
        {
            m_Instance = new UserManager();
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null)
            {
                m_Instance.m_DocRef = FirebaseFirestore.getInstance().collection(UserConstants.COLLECTION_NAME).document(firebaseUser.getUid());
            }
        }
        return m_Instance;
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
        m_DocRef.update(AlbumConstants.POSTS, FieldValue.arrayRemove(postRef));
    }
}
