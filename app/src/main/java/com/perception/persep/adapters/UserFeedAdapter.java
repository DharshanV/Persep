package com.perception.persep.adapters;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ListPopupWindow;
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
import com.perception.persep.data_gatherer.DataGathererManager;
import com.perception.persep.firebase_helpers.FirebaseQueries;
import com.perception.persep.fragments.UserFeed_Fragment;

import java.util.ArrayList;
import java.util.List;

public class UserFeedAdapter extends RecyclerView.Adapter<UserFeedAdapter.UserFeedViewHolder> implements FirebaseQueries.UserPostsQueryListener
{
    private static final String TAG = "UserFeedAdapter";

    public interface RefreshQueryCompleteListener
    {
        void onComplete();
    }
    private RefreshQueryCompleteListener m_RefreshListener = null;

    private UserFeed_Fragment m_UserFeedFragment;
    private boolean m_FirstLoad = true;
    private final List<Post> m_UserPosts = new ArrayList<>();

    public UserFeedAdapter(UserFeed_Fragment userFeedFragment)
    {
        m_UserFeedFragment = userFeedFragment;
        FirebaseUser loggedInUser = FirebaseAuth.getInstance().getCurrentUser();
        if (loggedInUser != null)
        {
//            FirebaseQueries.getInstance().QueryUserPosts(loggedInUser, this);
        }
    }

    public void refresh(RefreshQueryCompleteListener listener)
    {
        FirebaseUser loggedInUser = FirebaseAuth.getInstance().getCurrentUser();
        if (loggedInUser != null) {
            m_RefreshListener = listener;
//            FirebaseQueries.getInstance().QueryUserPosts(loggedInUser, this);
        }
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
        return new UserFeedViewHolder(view, this);
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
    public void OnQueryResult(boolean successful, String message, List<DocumentSnapshot> postSnapshots)
    {
        if (postSnapshots != null)
        {
            if (m_FirstLoad)
            {
                FirstPostsLoad(postSnapshots);
                m_FirstLoad = false;
            }
            else {
                RefreshPostsLoad(postSnapshots);
                if (m_RefreshListener != null) {
                    m_RefreshListener.onComplete();
                    m_RefreshListener = null;
                }
            }
        }
    }

    private void FirstPostsLoad(List<DocumentSnapshot> posts)
    {
        m_UserPosts.clear();
        for (DocumentSnapshot postDoc : posts)
        {
            m_UserPosts.add(new Post(postDoc.getId(), postDoc.getData(), false));
        }
        m_UserPosts.sort((o1, o2) -> o2.m_TimeCreated.compareTo(o1.m_TimeCreated));
        this.notifyDataSetChanged();
    }

    private void RefreshPostsLoad(List<DocumentSnapshot> posts)
    {
        List<Post> newPostsAdded = new ArrayList<>(posts.size());
        for(DocumentSnapshot postDoc : posts)
        {
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
                newPostsAdded.add(new Post(postDoc.getId(), postDoc.getData(), false));
            }
        }
        if (!newPostsAdded.isEmpty()) {
            newPostsAdded.sort((o1, o2) -> o2.m_TimeCreated.compareTo(o1.m_TimeCreated));
            m_UserPosts.addAll(0, newPostsAdded);
            this.notifyItemRangeInserted(0, newPostsAdded.size());
        }
    }

    public void deletePost(int postPosition)
    {
        FirebaseUser loggedInUser = FirebaseAuth.getInstance().getCurrentUser();
        if (loggedInUser != null && postPosition < m_UserPosts.size()) {
            Post post = m_UserPosts.get(postPosition);
            post.delete(loggedInUser, successful -> {
                if (successful) {
                    m_UserPosts.remove(postPosition);
                    this.notifyItemRemoved(postPosition);
                }
            });
        }
    }

    public void sharePost(int postPosition)
    {
        m_UserFeedFragment.openGoogleMaps(-33.858587567654794, 151.21389684447863);
    }

    public static class UserFeedViewHolder extends RecyclerView.ViewHolder
    {
        private final Context m_Context;
        public ImageView m_PostImage;
        public TextView m_PostLikeCount;
        public TextView m_PostGeoLocation;
        public ImageButton m_PostOptionButton;
        private final UserFeedAdapter m_UserFeed;

        int m_PopupWidth;
        int m_PopupHeight;
        final static String[] POST_OPTION_ITEMS = new String[]{"Delete", "Share"};
        ArrayAdapter<String> m_ItemsAdapter;
        ListPopupWindow m_PopupWindow;

        public UserFeedViewHolder(@NonNull View itemView, UserFeedAdapter userFeedAdapter)
        {
            super(itemView);
            m_UserFeed = userFeedAdapter;
            m_PostImage = itemView.findViewById(R.id.user_feed_post_image);
            m_PostLikeCount = itemView.findViewById(R.id.user_post_like_count);
            m_PostGeoLocation = itemView.findViewById(R.id.user_post_geo_location);
            m_PostOptionButton = itemView.findViewById(R.id.post_options_button);
            m_Context = itemView.getContext();

            m_ItemsAdapter = new ArrayAdapter<>(m_Context, R.layout.post_options, R.id.post_option_text, POST_OPTION_ITEMS);
            m_PopupWidth = measureContentWidth(m_ItemsAdapter);
            m_PopupHeight = ListPopupWindow.WRAP_CONTENT;

            m_PopupWindow = new ListPopupWindow(m_Context);
            m_PopupWindow.setAdapter(m_ItemsAdapter);
            m_PopupWindow.setWidth(m_PopupWidth);
            m_PopupWindow.setHeight(m_PopupHeight);
            m_PopupWindow.setModal(true);
            m_PopupWindow.setDropDownGravity(Gravity.END);

            m_PopupWindow.setOnItemClickListener((parent, view, position, id) -> {
                if (position == 0) {
                    m_UserFeed.deletePost(getAdapterPosition());
                }
                else if(position == 1) {
                    m_UserFeed.sharePost(getAdapterPosition());
                }
                m_PopupWindow.dismiss();
            });

            m_PostOptionButton.setOnClickListener(v -> {
                if (!m_PopupWindow.isShowing()) {
                    m_PopupWindow.setAnchorView(v);
                    m_PopupWindow.show();
                }
            });
        }

        public void setLike(long likes)
        {
            String textViewString = m_Context.getString(R.string.user_post_likes, likes);
            m_PostLikeCount.setText(textViewString);
        }

        private int measureContentWidth(ArrayAdapter<String> items)
        {
            ViewGroup measureParentViewGroup = new FrameLayout(m_Context);
            int maxWidth = 0;
            int itemType = 0;
            View itemView = null;

            int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

            for(int i=0;i<items.getCount();i++) {
                int positionType = items.getItemViewType(i);
                if (positionType != itemType) {
                    itemType = positionType;
                    itemView = null;
                }
                itemView = items.getView(i, itemView, measureParentViewGroup);
                itemView.measure(widthMeasureSpec, heightMeasureSpec);
                int itemWidth = itemView.getMeasuredWidth();
                if (itemWidth > maxWidth) {
                    maxWidth = itemWidth;
                }
            }
            return maxWidth;
        }
    }
}
