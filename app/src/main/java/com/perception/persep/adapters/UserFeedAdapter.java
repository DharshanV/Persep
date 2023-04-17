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
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.perception.persep.album_data.Post;
import com.perception.persep.constants.PostConstants;
import com.perception.persep.constants.UserConstants;
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

    public void refresh()
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore.getInstance().collection(UserConstants.COLLECTION_NAME).document(user.getUid()).get().addOnSuccessListener(userDoc -> {
            List<DocumentReference> userDocPostRefs = (List<DocumentReference>) userDoc.get(UserConstants.POSTS);

            if (userDocPostRefs == null || userDocPostRefs.isEmpty()) return;

            List<Task<DocumentSnapshot>> postQueryTasks = new ArrayList<>(userDocPostRefs.size());
            for (DocumentReference postRef : userDocPostRefs)
            {
                postQueryTasks.add(postRef.get());
            }

            Tasks.whenAllSuccess(postQueryTasks).addOnSuccessListener(postsResult -> {
                // Check for if post has been modified or added
                for (Object postObj : postsResult)
                {
                    DocumentSnapshot postDoc = (DocumentSnapshot) postObj;
                    Timestamp postDocTimeModified = (Timestamp) postDoc.get(PostConstants.TIME_MODIFIED);

                    int index = findPostIndex(postDoc.getId());
                    if (index != -1)
                    {
                        Post currPost = m_UserPosts.get(index);
                        if (postDocTimeModified != currPost.m_TimeModified)
                        {
                            currPost.m_Likes = (Long) postDoc.get(PostConstants.LIKES);
                            this.notifyItemChanged(index, currPost.m_Likes);
                            currPost.m_TimeModified = postDocTimeModified;
                        }
                    } else
                    {
                        m_UserPosts.add(0, new Post(postDoc.getId(), postDoc.getData(), false));
                        this.notifyItemInserted(0);
                    }
                }
            });
        });

    }

    private int findPostIndex(String postDocID)
    {
        for (int i = 0; i < m_UserPosts.size(); i++)
        {
            Post post = m_UserPosts.get(i);
            if (post.m_PostID.equals(postDocID))
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
            holder.m_PostGeoLocation.setText(post.m_GeoLocation);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull UserFeedViewHolder holder, int position, @NonNull List<Object> payloads)
    {
        if (!payloads.isEmpty())
        {
            long postLike = (Long) payloads.get(0);
            holder.setLike(postLike);
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public int getItemCount()
    {
        return m_UserPosts.size();
    }

    @Override
    public void OnQueryResult(boolean successful, String message, List<Post> posts)
    {
        if (posts != null) {
            m_UserPosts = posts;
            m_UserPosts.sort((o1, o2) -> o2.m_TimeCreated.compareTo(o1.m_TimeCreated));
            this.notifyDataSetChanged();
        }
    }

    public class UserFeedViewHolder extends RecyclerView.ViewHolder
    {
        private Context m_Context;
        public ImageView m_PostImage;
        public TextView m_PostLikeCount;
        public TextView m_PostGeoLocation;

        public UserFeedViewHolder(@NonNull View itemView)
        {
            super(itemView);
            m_PostImage = itemView.findViewById(R.id.user_feed_post_image);
            m_PostLikeCount = itemView.findViewById(R.id.user_post_like_count);
            m_PostGeoLocation = itemView.findViewById(R.id.user_post_geo_location);
            m_Context = itemView.getContext();
        }

        public void setLike(long likes)
        {
            String textViewString = m_Context.getString(R.string.user_post_likes, likes);
            m_PostLikeCount.setText(textViewString);
        }
    }
}
