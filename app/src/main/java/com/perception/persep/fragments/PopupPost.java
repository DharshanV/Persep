package com.perception.persep.fragments;

import android.app.Activity;
import android.icu.text.SimpleDateFormat;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

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

public class PopupPost
{
    private Activity m_Activity;
    private final SlidingUpPanelLayout m_SlidingUpPanel;
    private TextView m_PostUsername;
    private TextView m_PostCreated;
    private ImageView m_PostImage;

    private Post m_Post = null;
    private EditText m_PostCommentEditText;
    private ImageButton m_SendCommentButton;

    private ListView m_CommentsListView;
    private List<String> m_CommentIDs = new ArrayList<>();

    public PopupPost(Activity activity, View popupParentView, SlidingUpPanelLayout.PanelSlideListener panelSlideListener)
    {
        m_Activity = activity;

        m_SlidingUpPanel = popupParentView.findViewById(R.id.vr_fragment_slide_up_panel_layout);
        m_SlidingUpPanel.setFadeOnClickListener(v -> m_SlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED));
        m_SlidingUpPanel.setDragView(R.id.popup_drag_view);
        m_SlidingUpPanel.addPanelSlideListener(panelSlideListener);

        m_PostUsername = popupParentView.findViewById(R.id.popup_username_text_view);
        m_PostCreated = popupParentView.findViewById(R.id.popup_post_data_text_view);
        m_PostImage = popupParentView.findViewById(R.id.popup_user_post_image);

        m_CommentsListView = popupParentView.findViewById(R.id.popup_post_comments_list);
        m_SlidingUpPanel.setScrollableView(m_CommentsListView);

        m_PostCommentEditText = popupParentView.findViewById(R.id.popup_send_comment_edit_text);
        m_SendCommentButton = popupParentView.findViewById(R.id.popup_send_comment_button);
        m_SendCommentButton.setOnClickListener(v -> {
            if (m_Post != null) {
                String comment = m_PostCommentEditText.getText().toString();
                if (!comment.isEmpty()) {
                    m_CommentIDs.add(m_Post.addComment(comment));
                    refreshPostComments();
                }
            }
            m_PostCommentEditText.setText("");
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

            m_CommentIDs.clear();
            refreshPostComments();

            CommentManager.getInstance().queryPostComments(m_Post.m_PostID, (success, message, commentIDs) -> {
                m_CommentIDs = commentIDs;
                refreshPostComments();
            });

            m_SlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        }

//        new Handler().postDelayed(() -> m_CommentIDs.add(m_Post.addComment("Delay comment add")), 1000);
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
