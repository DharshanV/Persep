package com.perception.persep.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.persep.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.perception.persep.album_data.Post;
import com.perception.persep.firebase_helpers.FirebaseQueries;

import java.util.ArrayList;
import java.util.List;

public class UserFeedAdapter extends RecyclerView.Adapter<UserFeedAdapter.UserFeedViewHolder> implements FirebaseQueries.UserPostsQueryListener
{
    private static final String TAG = "UserFeedAdapter";

    private List<Post> m_UserPosts = new ArrayList<>();

    public UserFeedAdapter()
    {
        FirebaseUser loggedInUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseQueries.getInstance().QueryUserPosts(loggedInUser, this);
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
        return new UserFeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserFeedViewHolder holder, int position)
    {
        Post post = m_UserPosts.get(position);
        if (post != null)
        {
            post.loadNow(holder.m_PostImage);
            holder.setLike(post.m_Likes);
        }
    }

    @Override
    public int getItemCount()
    {
        return m_UserPosts.size();
    }

    @Override
    public void OnQueryResult(boolean successful, String message, List<Post> posts)
    {
        m_UserPosts = posts;
        this.notifyDataSetChanged();
    }

    public class UserFeedViewHolder extends RecyclerView.ViewHolder
    {
        private Context m_Context;
        public ImageView m_PostImage;
        public TextView m_PostLikeCount;

        public UserFeedViewHolder(@NonNull View itemView)
        {
            super(itemView);
            m_PostImage = itemView.findViewById(R.id.user_feed_post_image);
            m_PostLikeCount = itemView.findViewById(R.id.user_post_like_count);
            m_Context = itemView.getContext();
        }

        public void setLike(long likes)
        {
            String textViewString = m_Context.getString(R.string.user_post_likes, likes);
            m_PostLikeCount.setText(textViewString);
        }
    }
}
