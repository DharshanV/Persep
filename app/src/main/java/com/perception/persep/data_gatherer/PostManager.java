package com.perception.persep.data_gatherer;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.perception.persep.album_data.Post;
import com.perception.persep.constants.UserConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PostManager
{

    private static PostManager m_Instance = null;

    private Map<String, Post> m_Posts = new HashMap<>();

    private final ReadWriteLock m_ReadWriteLock = new ReentrantReadWriteLock();
    private final Lock m_WriteLock = m_ReadWriteLock.writeLock();
    private final Lock m_ReadLock = m_ReadWriteLock.writeLock();

    public interface AddPostListener
    {
        void onPostAdded(boolean success, String message, String postID);
    }

    public interface RemovePostListener
    {
        void onPostRemoved(DocumentReference postRef, String albumID);
    }

    public interface QueryUserPostListener
    {
        void onQuery(List<String> postIDs);
    }

    public interface PostLikeListener
    {
        void onLikeUpdate(boolean successful);
    }

    private PostManager()
    {
    }

    public static synchronized PostManager getInstance()
    {
        if (m_Instance == null) {
            m_Instance = new PostManager();
        }
        return m_Instance;
    }

    public String addPost(Uri postImageURI, String albumID, double lat, double lon)
    {
        m_WriteLock.lock();
        Post post = new Post(postImageURI, albumID, lat, lon);
        m_Posts.put(post.m_PostID, post);
        m_WriteLock.unlock();
        return post.m_PostID;
    }

    public void addPost(DocumentReference postRef, AddPostListener listener)
    {
        for(Map.Entry<String, Post> entry : m_Posts.entrySet()) {
            if (entry.getValue().m_DocRef == postRef) {
                listener.onPostAdded(true, "", entry.getKey());
                return;
            }
        }
        postRef.get().addOnSuccessListener(postDoc -> {
            String postID = postDoc.getId();
            m_WriteLock.lock();
            m_Posts.put(postID, new Post(postDoc));
            m_WriteLock.unlock();
            listener.onPostAdded(true, "", postID);
        });
    }

    public Post removePost(String postID)
    {
        m_WriteLock.lock();
        Post post = m_Posts.remove(postID);
        m_WriteLock.unlock();
        if (post != null) {
            post.remove();
        }
        return post;
    }

    public Post getPost(String postID)
    {
        return m_Posts.get(postID);
    }

    public void queryUserPosts(String userID, QueryUserPostListener listener)
    {
        FirebaseFirestore.getInstance().collection(UserConstants.COLLECTION_NAME).document(userID).get().addOnSuccessListener(userDoc -> {
            List<DocumentReference> userDocPostRefs = (List<DocumentReference>) userDoc.get(UserConstants.POSTS);

            if (userDocPostRefs == null) return;

            List<Task<DocumentSnapshot>> postQueryTasks = new ArrayList<>(userDocPostRefs.size());
            for (DocumentReference postRef : userDocPostRefs)
            {
                postQueryTasks.add(postRef.get());
            }

            Tasks.whenAllSuccess(postQueryTasks).addOnSuccessListener(postsResult -> {
                List<String> postIDs = new ArrayList<>();
                for(Object postObj : postsResult) {
                    DocumentSnapshot postDoc = (DocumentSnapshot) postObj;
                    String postID = postDoc.getId();
                    m_WriteLock.lock();
                    if (!m_Posts.containsKey(postID)) {
                        m_Posts.put(postID, new Post(postDoc));
                    }
                    m_WriteLock.unlock();
                    postIDs.add(postID);
                }
                listener.onQuery(postIDs);
            }).addOnFailureListener(Exception::printStackTrace);
        }).addOnFailureListener(Exception::printStackTrace);
    }
}
