package com.perception.persep.data_gatherer;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.perception.persep.album_data.Comment;
import com.perception.persep.album_data.Post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import timber.log.Timber;

public class CommentManager
{
    private static CommentManager m_Instance = null;
    private final ReadWriteLock m_ReadWriteLock = new ReentrantReadWriteLock();
    private final Lock m_WriteLock = m_ReadWriteLock.writeLock();
    private final Lock m_ReadLock = m_ReadWriteLock.readLock();

    private final Map<String, Comment> m_Comments = new HashMap<>();

    private CommentManager()
    {
    }

    public static synchronized CommentManager getInstance()
    {
        if (m_Instance == null)
        {
            m_Instance = new CommentManager();
        }
        return m_Instance;
    }

    public String addComment(String postID, String commentStr)
    {
        Comment comment = new Comment(postID, commentStr);
        m_WriteLock.lock();
        m_Comments.put(comment.m_CommentID, comment);
        m_WriteLock.unlock();
        return comment.m_CommentID;
    }

    public Comment getComment(String commentID)
    {
        m_ReadLock.lock();
        Comment comment = m_Comments.get(commentID);
        m_ReadLock.unlock();
        return comment;
    }

    public void queryPostComments(String postID, QueryCommentListener listener)
    {
        Post post = PostManager.getInstance().getPost(postID);
        if (post != null) {
            List<String> postCommentsIDs = new ArrayList<>();

            List<Task<DocumentSnapshot>> commentsQueryTasks = new ArrayList<>();
            for (DocumentReference commentRef : post.m_PostComments) {
                Comment comment = getComment(commentRef.getId());
                if (comment != null) {
                    postCommentsIDs.add(comment.m_CommentID);
                }
                else {
                    commentsQueryTasks.add(commentRef.get());
                }
            }

            if (commentsQueryTasks.isEmpty()) {
                listener.onCommentAdded(true, "", postCommentsIDs);
                return;
            }

            Tasks.whenAllSuccess(commentsQueryTasks).addOnSuccessListener(commentsResult -> {
                m_WriteLock.lock();
                for (Object commentObj : commentsResult)
                {
                    DocumentSnapshot commentDoc = (DocumentSnapshot) commentObj;
                    try {
                        Comment comment = new Comment(commentDoc);
                        postCommentsIDs.add(comment.m_CommentID);
                        m_Comments.put(comment.m_CommentID, comment);
                    } catch (NullPointerException ignored) {}
                }
                m_WriteLock.unlock();
                listener.onCommentAdded(true, "", postCommentsIDs);
            }).addOnFailureListener(e -> {
                listener.onCommentAdded(false, "", postCommentsIDs);
            });
        }
    }

    public interface AddCommentListener
    {
        void onCommentAdded(boolean success, String message, String commentID);
    }

    public interface QueryCommentListener
    {
        void onCommentAdded(boolean success, String message, List<String> commentIDs);
    }
}

