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
import com.perception.persep.user_data.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserManager
{
    static private UserManager m_Instance = null;

    public DocumentReference m_DocRef = null;

    private Map<DocumentReference, User> m_UserData = new HashMap<>();

    public interface DeleteUserListener
    {
        void onDeleteListener(boolean successful);
    }

    public interface QueryUserDataListener
    {
        void onQueryUserData(User user);
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
            m_Instance.m_DocRef.get().addOnSuccessListener(userDoc -> m_UserData.put(m_DocRef, new User(userDoc)));
        }
    }

    public void getUserData(DocumentReference userRef, QueryUserDataListener queryUserDataListener)
    {
        User user = m_UserData.get(userRef);
        if (user != null)
        {
            queryUserDataListener.onQueryUserData(user);
        }
        else {
            userRef.get().addOnSuccessListener(userDoc -> {
                User userData = new User(userDoc);
                m_UserData.put(userRef, userData);
                queryUserDataListener.onQueryUserData(userData);
            });
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
            m_UserData.remove(m_DocRef);
        }
    }
}
