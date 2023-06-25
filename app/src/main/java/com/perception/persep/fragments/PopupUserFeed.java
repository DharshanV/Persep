package com.perception.persep.fragments;

import android.app.Activity;
import android.icu.text.SimpleDateFormat;
import android.view.View;
import android.widget.ListView;

import com.ablanco.zoomy.Zoomy;
import com.example.persep.R;
import com.perception.persep.adapters.PostCommentAdapter;
import com.perception.persep.adapters.PostLikesAdapter;
import com.perception.persep.album_data.Comment;
import com.perception.persep.album_data.Post;
import com.perception.persep.data_gatherer.CommentManager;
import com.perception.persep.data_gatherer.UserManager;
import com.perception.persep.user_data.User;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PopupUserFeed
{
    private Activity m_Activity;
    private final SlidingUpPanelLayout m_SlidingUpPanel;

    private ListView m_PopupListView;
    private Post m_Post;
    private List<String> m_CommentIDs = new ArrayList<>();

    public enum PopupType
    {
        COMMENTS,
        LIKES;
    }

    public PopupUserFeed(Activity activity, View popupParentView, SlidingUpPanelLayout.PanelSlideListener panelSlideListener)
    {
        m_Activity = activity;

        m_SlidingUpPanel = popupParentView.findViewById(R.id.user_feed_slide_up_panel_layout);
        m_SlidingUpPanel.setFadeOnClickListener(v -> m_SlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED));
        m_SlidingUpPanel.setDragView(R.id.feed_popup_popup_drag_view);
        m_SlidingUpPanel.addPanelSlideListener(panelSlideListener);

        m_PopupListView = popupParentView.findViewById(R.id.feed_popup_list_view);
    }

    public void launch(PopupType popupType, Post post, Post.PostRefreshListener listener)
    {
        m_Post = post;
        m_PopupListView.setAdapter(null);

        if (popupType == PopupType.COMMENTS) {
            m_CommentIDs.clear();

            CommentManager.getInstance().queryPostComments(post.m_PostID, (success, message, commentIDs) -> {
                m_CommentIDs = commentIDs;
                refreshPostComments();
                listener.onPostRefresh(false);
            });
        }
        else if (popupType == PopupType.LIKES) {
            UserManager.getInstance().queryPostLikes(post, (UserManager.QueryUsersLikedListener) (success, message, userIDs) -> {
                refreshPostLikes();
                listener.onPostRefresh(false);
            });
        }

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
            m_PopupListView.setAdapter(adapter);
        }
        else {
            m_PopupListView.setAdapter(null);
        }
    }

    private void refreshPostLikes()
    {
        if (!m_Post.m_UsersLiked.isEmpty()) {
            PostLikesAdapter adapter = new PostLikesAdapter(m_Activity, m_Post.m_UsersLiked);
            m_PopupListView.setAdapter(adapter);
        }
        else {
            m_PopupListView.setAdapter(null);
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
