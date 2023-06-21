package com.perception.persep.fragments;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.persep.R;
import com.perception.persep.album_data.Post;
import com.perception.persep.user_data.User;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class PopupPost
{
    private final SlidingUpPanelLayout m_SlidingUpPanel;
    private TextView m_PostUsername;
    private ImageView m_PostImage;

    public PopupPost(View popupParentView, SlidingUpPanelLayout.PanelSlideListener panelSlideListener)
    {
        m_SlidingUpPanel = popupParentView.findViewById(R.id.vr_fragment_slide_up_panel_layout);
        m_SlidingUpPanel.setFadeOnClickListener(v -> m_SlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED));
        m_SlidingUpPanel.setDragView(R.id.popup_drag_view);
        m_SlidingUpPanel.addPanelSlideListener(panelSlideListener);

        m_PostUsername = popupParentView.findViewById(R.id.popup_username_text_view);
        m_PostImage = popupParentView.findViewById(R.id.popup_user_post_image);
    }

    public void launch(Post post)
    {
        User userData = post.m_UserData;
        if (userData != null) {
            m_PostUsername.setText(userData.m_Username);
            m_PostImage.setImageBitmap(post.m_Image);
            m_SlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        }
    }
}
