package com.perception.persep.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ablanco.zoomy.Zoomy;
import com.example.persep.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.perception.persep.album_data.Post;
import com.perception.persep.data_gatherer.AlbumManager;
import com.perception.persep.data_gatherer.PostManager;
import com.perception.persep.fragments.UserFeed_Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserFeedAdapter extends RecyclerView.Adapter<UserFeedAdapter.UserFeedViewHolder>
{
    private static final String TAG = "UserFeedAdapter";
    private final UserFeed_Fragment m_UserFeedFragment;
    private final List<String> m_UserPostIDs = new ArrayList<>();
    public UserFeedAdapter(UserFeed_Fragment userFeedFragment)
    {
        m_UserFeedFragment = userFeedFragment;
        FirebaseUser loggedInUser = FirebaseAuth.getInstance().getCurrentUser();
        if (loggedInUser != null)
        {
            PostManager.getInstance().queryUserPosts(loggedInUser.getUid(), postIDs -> {
                if (postIDs == null || postIDs.isEmpty()) return;

                m_UserPostIDs.addAll(postIDs);
                sortUserPosts(m_UserPostIDs);
                this.notifyDataSetChanged();
            });
        }
    }

    static private void sortUserPosts(List<String> postIDs)
    {
        if (postIDs != null)
        {
            postIDs.sort((o1, o2) -> {
                Post post1 = PostManager.getInstance().getPost(o1);
                Post post2 = PostManager.getInstance().getPost(o2);

                return post2.m_TimeCreated.compareTo(post1.m_TimeCreated);
            });
        }
    }

    public void refresh(RefreshQueryCompleteListener listener)
    {
        FirebaseUser loggedInUser = FirebaseAuth.getInstance().getCurrentUser();
        if (loggedInUser != null)
        {
            PostManager.getInstance().queryUserPosts(loggedInUser.getUid(), postIDs -> {
                listener.onComplete();
                if (postIDs == null || postIDs.isEmpty()) return;

                List<String> newPostsAdded = new ArrayList<>();
                for (String postID : postIDs)
                {
                    int postIndex = getPostIndex(postID);
                    if (postIndex != -1)
                    {
                        Post post = PostManager.getInstance().getPost(postID);
                        post.serverRefresh(refreshedFromServer -> this.notifyItemChanged(postIndex));
                    } else
                    {
                        newPostsAdded.add(postID);
                    }
                }

                if (!newPostsAdded.isEmpty())
                {
                    sortUserPosts(newPostsAdded);
                    m_UserPostIDs.addAll(0, newPostsAdded);
                    this.notifyItemRangeInserted(0, newPostsAdded.size());
                }

            });
        }
    }

    private int getPostIndex(String postID)
    {
        for (int i = 0; i < m_UserPostIDs.size(); i++)
        {
            if (m_UserPostIDs.get(i).equals(postID))
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemViewType(int position)
    {
        return R.layout.userfeed_view_holder;
    }

    @NonNull
    @Override
    public UserFeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new UserFeedViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull UserFeedViewHolder holder, int position)
    {
        Post post = PostManager.getInstance().getPost(m_UserPostIDs.get(position));
        if (post != null)
        {
            post.loadInto(holder.m_PostImage);
            holder.m_PostID = post.m_PostID;
            holder.setLike(post.m_Likes);
            holder.setCommentCount(post.m_PostComments.size());
            holder.m_PostGeoLocation.setText(post.m_GeoLocation);
        }
    }

    @Override
    public int getItemCount()
    {
        return m_UserPostIDs.size();
    }

    public void deletePost(int postPosition)
    {
        if (postPosition < m_UserPostIDs.size())
        {
            String postID = m_UserPostIDs.remove(postPosition);
            AlbumManager.getInstance().removePost(postID);
            this.notifyItemRemoved(postPosition);
        }
    }

    public void sharePost(int postPosition)
    {
        if (postPosition < m_UserPostIDs.size())
        {
            Post post = PostManager.getInstance().getPost(m_UserPostIDs.get(postPosition));
            if (post != null)
            {
                m_UserFeedFragment.sharePost(post.m_GeoPoint.getLatitude(), post.m_GeoPoint.getLongitude());
            }
        }
    }

    public void popupPostComments(int postPosition, Post.PostRefreshListener listener)
    {
        if (postPosition < m_UserPostIDs.size())
        {
            Post post = PostManager.getInstance().getPost(m_UserPostIDs.get(postPosition));
            if (post != null)
            {
                m_UserFeedFragment.popupPostComments(post, listener);
            }
        }
    }

    public interface RefreshQueryCompleteListener
    {
        void onComplete();
    }

    public static class UserFeedViewHolder extends RecyclerView.ViewHolder
    {
        final static String[] POST_OPTION_ITEMS = new String[]{"Delete", "Share"};
        private final Context m_Context;
        private final UserFeedAdapter m_UserFeed;
        public String m_PostID;
        public ImageView m_PostImage;
        public TextView m_PostLikeCount;
        public TextView m_PostCommentCount;
        public TextView m_PostGeoLocation;
        public ImageButton m_PostOptionButton;
        public ImageButton m_PostCommentsButton;
        int m_PopupWidth;
        int m_PopupHeight;
        ArrayAdapter<String> m_ItemsAdapter;
        ListPopupWindow m_PopupWindow;

        private final AlertDialog m_ConfirmDeleteDialog;

        public UserFeedViewHolder(@NonNull View itemView, UserFeedAdapter userFeedAdapter)
        {
            super(itemView);
            m_UserFeed = userFeedAdapter;
            m_PostImage = itemView.findViewById(R.id.user_feed_post_image);
            m_PostLikeCount = itemView.findViewById(R.id.user_post_like_count);
            m_PostCommentCount = itemView.findViewById(R.id.user_post_comment_count);
            m_PostGeoLocation = itemView.findViewById(R.id.user_post_geo_location);
            m_PostOptionButton = itemView.findViewById(R.id.post_options_button);
            m_PostCommentsButton = itemView.findViewById(R.id.user_post_comment_image);
            m_Context = itemView.getContext();

            Zoomy.Builder builder = new Zoomy.Builder(getActivity(m_Context)).target(m_PostImage);
            builder.enableImmersiveMode(false);
            builder.register();

            m_ItemsAdapter = new ArrayAdapter<>(m_Context, R.layout.post_options, R.id.post_option_text, POST_OPTION_ITEMS);
            m_PopupWidth = measureContentWidth(m_ItemsAdapter);
            m_PopupHeight = ListPopupWindow.WRAP_CONTENT;

            m_PopupWindow = new ListPopupWindow(m_Context);
            m_PopupWindow.setAdapter(m_ItemsAdapter);
            m_PopupWindow.setWidth(m_PopupWidth);
            m_PopupWindow.setHeight(m_PopupHeight);
            m_PopupWindow.setModal(true);
            m_PopupWindow.setDropDownGravity(Gravity.END);

            m_ConfirmDeleteDialog = new AlertDialog.Builder(m_Context)
                    .setTitle("Delete Post")
                    .setMessage("Do you really want to delete this post?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        m_UserFeed.deletePost(getAdapterPosition());
                    })
                    .setNegativeButton("Cancel", null)
                    .create();

            m_ConfirmDeleteDialog.setOnShowListener(dialog -> {
                TextView messageTextview = m_ConfirmDeleteDialog.findViewById(android.R.id.message);
                if (messageTextview != null)
                {
                    messageTextview.setTypeface(ResourcesCompat.getFont(m_Context, R.font.playfair_display_regular));
                }

                float size = m_Context.getResources().getDimension(R.dimen.delete_post_button_text_size);
                m_ConfirmDeleteDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
                m_ConfirmDeleteDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
            });

            m_PopupWindow.setOnItemClickListener((parent, view, position, id) -> {
                if (position == 0)
                {
                    m_ConfirmDeleteDialog.show();
                } else if (position == 1)
                {
                    m_UserFeed.sharePost(getAdapterPosition());
                }
                m_PopupWindow.dismiss();
            });

            m_PostOptionButton.setOnClickListener(v -> {
                if (!m_PopupWindow.isShowing())
                {
                    m_PopupWindow.setAnchorView(v);
                    m_PopupWindow.show();
                }
            });

            m_PostCommentsButton.setOnClickListener(v -> {
                m_UserFeed.popupPostComments(getAdapterPosition(), refreshedFromServer -> {
                    Post post = PostManager.getInstance().getPost(m_PostID);
                    if (post != null) {
                        setCommentCount(post.m_PostComments.size());
                    }
                });
            });
        }

        public void setLike(long likes)
        {
            String textViewString = m_Context.getString(R.string.user_post_likes, likes);
            m_PostLikeCount.setText(textViewString);
        }

        public void setCommentCount(long commentCount)
        {
            String textViewString = m_Context.getString(R.string.user_post_comments_count, commentCount);
            m_PostCommentCount.setText(textViewString);
        }

        private int measureContentWidth(ArrayAdapter<String> items)
        {
            ViewGroup measureParentViewGroup = new FrameLayout(m_Context);
            int maxWidth = 0;
            int itemType = 0;
            View itemView = null;

            int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

            for (int i = 0; i < items.getCount(); i++)
            {
                int positionType = items.getItemViewType(i);
                if (positionType != itemType)
                {
                    itemType = positionType;
                    itemView = null;
                }
                itemView = items.getView(i, itemView, measureParentViewGroup);
                itemView.measure(widthMeasureSpec, heightMeasureSpec);
                int itemWidth = itemView.getMeasuredWidth();
                if (itemWidth > maxWidth)
                {
                    maxWidth = itemWidth;
                }
            }
            return maxWidth;
        }

        public Activity getActivity(Context context)
        {
            if (context == null)
            {
                return null;
            }
            else if (context instanceof ContextWrapper)
            {
                if (context instanceof Activity)
                {
                    return (Activity) context;
                }
                else
                {
                    return getActivity(((ContextWrapper) context).getBaseContext());
                }
            }

            return null;
        }
    }
}
