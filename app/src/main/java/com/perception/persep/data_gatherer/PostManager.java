package com.perception.persep.data_gatherer;

import android.content.Context;
import android.net.Uri;

import com.perception.persep.album_data.Post;

import java.util.Map;

public class PostManager
{

    private static PostManager m_Instance = null;

    private final Context m_Context;

    public interface AddPostListener
    {
        void onPostAdded(boolean success, String message, String postID);
    }

    public interface AddPostAlbumListener
    {
        void onPostAdded(boolean success, String message, String postID, Map<String,Object> albumData);
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

    public void addPost(Uri postImageURI, AddPostAlbumListener listener)
    {

    }

    public void addPost(Uri postImageURI, String albumID, AddPostListener listener)
    {

    }
}
