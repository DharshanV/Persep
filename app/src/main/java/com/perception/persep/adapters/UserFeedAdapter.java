package com.perception.persep.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.persep.R;

public class UserFeedAdapter extends RecyclerView.Adapter<UserFeedAdapter.UserFeedViewHolder>
{

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
        holder.postImage.setImageResource(R.drawable.ic_launcher_background);
    }

    @Override
    public int getItemCount()
    {
        return 5;
    }

    public class UserFeedViewHolder extends RecyclerView.ViewHolder
    {
        public ImageView postImage;

        public UserFeedViewHolder(@NonNull View itemView)
        {
            super(itemView);
            postImage= itemView.findViewById(R.id.user_feed_post_image);
        }
    }
}
