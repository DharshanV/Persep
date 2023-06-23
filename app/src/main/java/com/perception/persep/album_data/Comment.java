package com.perception.persep.album_data;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.perception.persep.constants.CommentConstants;

public class Comment
{
    public String m_CommentID;
    public String m_CommentStr;
    public DocumentReference m_UserRef;
    public DocumentReference m_DocRef;

    public Comment(DocumentSnapshot commentDoc)
    {
        m_CommentID = commentDoc.getId();
        m_CommentStr = (String) commentDoc.get(CommentConstants.COMMENT);
        m_UserRef = (DocumentReference) commentDoc.get(CommentConstants.USER);
        m_DocRef = commentDoc.getReference();
    }
}
