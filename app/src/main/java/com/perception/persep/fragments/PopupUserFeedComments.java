package com.perception.persep.fragments;

import android.app.Activity;
import android.icu.text.SimpleDateFormat;
import android.view.View;

import com.ablanco.zoomy.Zoomy;
import com.example.persep.R;
import com.perception.persep.album_data.Post;
import com.perception.persep.data_gatherer.CommentManager;
import com.perception.persep.user_data.User;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.Locale;

public class PopupUserFeedComments
{
    private Activity m_Activity;
    private final SlidingUpPanelLayout m_SlidingUpPanel;

    public PopupUserFeedComments(Activity activity, View popupParentView, SlidingUpPanelLayout.PanelSlideListener panelSlideListener)
    {
        m_Activity = activity;

        m_SlidingUpPanel = popupParentView.findViewById(R.id.user_feed_slide_up_panel_layout);
        m_SlidingUpPanel.setFadeOnClickListener(v -> m_SlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED));
        m_SlidingUpPanel.setDragView(R.id.feed_comments_popup_drag_view);
        m_SlidingUpPanel.addPanelSlideListener(panelSlideListener);
    }

    public void launch(Post post)
    {
        m_SlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
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
