package com.perception.persep.data_gatherer;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

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
import java.util.HashMap;
import java.util.List;
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

    public interface QueryUserPostListener
    {
        void onQuery(List<String> postIDs);
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

    public static synchronized PostManager getInstance()
    {
        return m_Instance;
    }

    public String addPost(Uri postImageURI, String albumID, double lat, double lon)
    {
        Post post = new Post(postImageURI, albumID, lat, lon);
        m_Posts.put(post.m_PostID, post);
        return post.m_PostID;
    }

    public void addPost(DocumentReference postRef, AddPostListener listener)
    {
        for(Map.Entry<String, Post> entry : m_Posts.entrySet()) {
            if (entry.getValue().m_DocRef == postRef) {
                listener.onPostAdded(true, "", entry.getKey());
            }
        }
        postRef.get().addOnSuccessListener(postDoc -> {
            String postID = postDoc.getId();
            m_Posts.put(postID, new Post(postDoc));
            listener.onPostAdded(true, "", postID);
        });
    }

    private void addPost(DocumentSnapshot postDoc)
    {

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

    public void queryUserPosts(String userID, QueryUserPostListener listener)
    {
        FirebaseFirestore.getInstance().collection(UserConstants.COLLECTION_NAME).document(userID).get().addOnSuccessListener(userDoc -> {
            List<DocumentReference> userDocPostRefs = (List<DocumentReference>) userDoc.get(UserConstants.POSTS);

            if (userDocPostRefs == null || userDocPostRefs.isEmpty()) return;

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
                    if (m_Posts.containsKey(postID)) {
                        Post post = m_Posts.get(postID);
                        post.update(postDoc);
                    }
                    else {
                        m_Posts.put(postID, new Post(postDoc));
                    }
                    postIDs.add(postID);
                }
                listener.onQuery(postIDs);
            }).addOnFailureListener(Exception::printStackTrace);
        }).addOnFailureListener(Exception::printStackTrace);
    }
}
