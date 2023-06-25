package com.perception.persep.fragments;

import android.app.Activity;
import android.icu.text.SimpleDateFormat;
import android.view.View;
import android.widget.ListView;

import com.ablanco.zoomy.Zoomy;
import com.example.persep.R;
import com.perception.persep.adapters.PostCommentAdapter;
import com.perception.persep.album_data.Comment;
import com.perception.persep.album_data.Post;
import com.perception.persep.data_gatherer.CommentManager;
import com.perception.persep.user_data.User;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PopupUserFeedComments
{
    private Activity m_Activity;
    private final SlidingUpPanelLayout m_SlidingUpPanel;

    private ListView m_CommentsListView;
    private List<String> m_CommentIDs = new ArrayList<>();

    public PopupUserFeedComments(Activity activity, View popupParentView, SlidingUpPanelLayout.PanelSlideListener panelSlideListener)
    {
        m_Activity = activity;

        m_SlidingUpPanel = popupParentView.findViewById(R.id.user_feed_slide_up_panel_layout);
        m_SlidingUpPanel.setFadeOnClickListener(v -> m_SlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED));
        m_SlidingUpPanel.setDragView(R.id.feed_comments_popup_drag_view);
        m_SlidingUpPanel.addPanelSlideListener(panelSlideListener);

        m_CommentsListView = popupParentView.findViewById(R.id.feed_comments_list_view);
    }

    public void launch(Post post, Post.PostRefreshListener listener)
    {
        m_CommentIDs.clear();
        refreshPostComments();

        CommentManager.getInstance().queryPostComments(post.m_PostID, (success, message, commentIDs) -> {
            m_CommentIDs = commentIDs;
            refreshPostComments();
            listener.onPostRefresh(false);
        });

        m_SlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
    }

    private void refreshPostComments()
    {
        List<Comment> postComments = new ArrayList<>();
        for (String commendID : m_CommentIDs)
        {
            Comment comment = CommentManager.getInstance().getComment(commendID);
            if (comment != null) {
                postComments.add(comment);
            }
        }
        if (!postComments.isEmpty()) {
            PostCommentAdapter adapter = new PostCommentAdapter(m_Activity, postComments);
            m_CommentsListView.setAdapter(adapter);
        }
        else {
            m_CommentsListView.setAdapter(null);
        }
    }


    public SlidingUpPanelLayout.PanelState getPanelState()
    {
        return m_SlidingUpPanel.getPanelState();
    }

    public void setPanelState(SlidingUpPanelLayout.PanelState state)
    {
        m_SlidingUpPanel.setPanelState(state);
    }
}
