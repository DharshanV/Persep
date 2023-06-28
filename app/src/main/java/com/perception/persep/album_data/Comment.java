package com.perception.persep.album_data;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.perception.persep.constants.CommentConstants;
import com.perception.persep.data_gatherer.PostManager;
import com.perception.persep.data_gatherer.UserManager;

import java.util.HashMap;
import java.util.Map;

public class Comment
{
    public String m_CommentID;
    public String m_CommentStr;
    public Timestamp m_TimeCreated;
    public Timestamp m_TimeModified;
    public DocumentReference m_PostRef;
    public DocumentReference m_UserRef;
    public DocumentReference m_DocRef;

    public Comment(String postID, String commentStr)
    {
        m_CommentStr = commentStr;
        m_PostRef = PostManager.getInstance().getPost(postID).m_DocRef;
        m_UserRef = UserManager.getInstance().m_DocRef;
        Timestamp now = Timestamp.now();
        m_TimeCreated = now;
        m_TimeModified = now;
        createCommentServer();
    }

    public Comment(DocumentSnapshot commentDoc)
    {
        m_CommentID = commentDoc.getId();
        m_CommentStr = (String) commentDoc.get(CommentConstants.COMMENT);
        m_UserRef = (DocumentReference) commentDoc.get(CommentConstants.USER);
        m_TimeCreated = (Timestamp) commentDoc.get(CommentConstants.TIME_CREATED);
        m_TimeModified = (Timestamp) commentDoc.get(CommentConstants.TIME_MODIFIED);
        m_DocRef = commentDoc.getReference();
    }

    private void createCommentServer()
    {
        m_DocRef = FirebaseFirestore.getInstance().collection(CommentConstants.COLLECTION_NAME).document();
        m_CommentID = m_DocRef.getId();

        Map<String, Object> commentData = new HashMap<>();
        commentData.put(CommentConstants.USER, m_UserRef);
        commentData.put(CommentConstants.POST, m_PostRef);
        commentData.put(CommentConstants.COMMENT, m_CommentStr);
        commentData.put(CommentConstants.TIME_CREATED, m_TimeCreated);
        commentData.put(CommentConstants.TIME_MODIFIED, m_TimeModified);
        m_DocRef.set(commentData);
    }
}
