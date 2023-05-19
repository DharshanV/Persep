package com.perception.persep.data_gatherer;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.perception.persep.album_data.Post;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PostManager
{

    private static PostManager m_Instance = null;

    private final Context m_Context;
    private Map<String, Post> m_Posts = new HashMap<>();

    public interface AddPostListener
    {
        void onPostAdded(boolean success, String message, String postID);
    }

    public interface RemovePostListener
    {
        void onPostRemoved(boolean success, DocumentReference postRef, String albumID);
    }

    private PostManager(Context context)
    {
        m_Context = context;
    }

    public static synchronized PostManager getInstance(Context context)
    {
        if (m_Instance == null) {
            m_Instance = new PostManager(context.getApplicationContext());
        }
        return m_Instance;
    }

    public String addPost(Uri postImageURI, String albumID, double lat, double lon)
    {
        Post post = new Post(postImageURI, albumID, lat, lon, false);
        m_Posts.put(post.m_PostID, post);
        return post.m_PostID;
    }

    public void removePost(String postID, RemovePostListener listener)
    {
        Post post = m_Posts.get(postID);
        if (post != null) {
            post.remove(listener);
        }
        m_Posts.remove(postID);
    }

    public Post getPost(String postID)
    {
        return m_Posts.get(postID);
    }
}
