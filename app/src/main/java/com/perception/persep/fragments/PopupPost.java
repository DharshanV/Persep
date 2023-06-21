package com.perception.persep.fragments;

import android.view.View;

import com.example.persep.R;
import com.perception.persep.album_data.Post;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class PopupPost
{
    private SlidingUpPanelLayout m_SlidingUpPanel;

    public PopupPost(View popupParentView, SlidingUpPanelLayout.PanelSlideListener panelSlideListener)
    {
        m_SlidingUpPanel = popupParentView.findViewById(R.id.vr_fragment_slide_up_panel_layout);
        m_SlidingUpPanel.setFadeOnClickListener(v -> m_SlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED));
        m_SlidingUpPanel.setDragView(R.id.popup_drag_view);
        m_SlidingUpPanel.addPanelSlideListener(panelSlideListener);
    }

    public void launch(Post post)
    {
        m_SlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
    }
}
