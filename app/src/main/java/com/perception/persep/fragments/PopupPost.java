package com.perception.persep.fragments;

import android.app.Activity;
import android.icu.text.SimpleDateFormat;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.ablanco.zoomy.Zoomy;
import com.example.persep.R;
import com.perception.persep.album_data.Post;
import com.perception.persep.user_data.User;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.Locale;

public class PopupPost
{
    private final SlidingUpPanelLayout m_SlidingUpPanel;
    private TextView m_PostUsername;
    private TextView m_PostCreated;
    private ImageView m_PostImage;

    private Post m_Post = null;
    private EditText m_PostCommentEditText;
    private ImageButton m_SendCommentButton;

    public PopupPost(Activity activity, View popupParentView, SlidingUpPanelLayout.PanelSlideListener panelSlideListener)
    {
        m_SlidingUpPanel = popupParentView.findViewById(R.id.vr_fragment_slide_up_panel_layout);
        m_SlidingUpPanel.setFadeOnClickListener(v -> m_SlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED));
        m_SlidingUpPanel.setDragView(R.id.popup_drag_view);
        m_SlidingUpPanel.addPanelSlideListener(panelSlideListener);

        m_PostUsername = popupParentView.findViewById(R.id.popup_username_text_view);
        m_PostCreated = popupParentView.findViewById(R.id.popup_post_data_text_view);
        m_PostImage = popupParentView.findViewById(R.id.popup_user_post_image);

        m_PostCommentEditText = popupParentView.findViewById(R.id.popup_send_comment_edit_text);
        m_SendCommentButton = popupParentView.findViewById(R.id.popup_send_comment_button);
        m_SendCommentButton.setOnClickListener(v -> {
            if (m_Post != null) {
                String comment = m_PostCommentEditText.getText().toString();
                if (!comment.isEmpty()) {
                    m_Post.addComment(comment);
                }
            }
        });

        Zoomy.Builder builder = new Zoomy.Builder(activity).target(m_PostImage);
        builder.enableImmersiveMode(false);
        builder.register();
    }

    public void launch(Post post)
    {
        User userData = post.m_UserData;
        if (userData != null) {
            m_Post = post;
            m_PostUsername.setText(userData.m_Username);
            SimpleDateFormat timeStampFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            m_PostCreated.setText(timeStampFormat.format(post.m_TimeCreated.toDate()).replace('-', '/'));
            m_PostImage.setImageBitmap(post.m_Image);
            m_PostCommentEditText.setText("");

            m_SlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        }
    }
}
