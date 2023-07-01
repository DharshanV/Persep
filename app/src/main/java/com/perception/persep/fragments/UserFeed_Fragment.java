package com.perception.persep.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.example.persep.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.perception.persep.activities.SettingsActivity;
import com.perception.persep.adapters.UserFeedAdapter;
import com.perception.persep.album_data.Post;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.Timer;
import java.util.TimerTask;

public class UserFeed_Fragment extends Fragment implements SlidingUpPanelLayout.PanelSlideListener
{
    private static final String TAG = "UserFeed_Fragment";
    private Timer m_RefreshTimer;
    private SwipeRefreshLayout m_SwipeRefreshLayout;
    private UserFeedAdapter m_UserFeedAdapter;
    private RecyclerView m_UserFeedRecycleView;
    private ViewPager2 m_ViewPager;
    private PopupUserFeed m_PopupUserFeed;
    private TextView m_EmptyPostsTextView;

    public UserFeed_Fragment(ViewPager2 viewPager)
    {
        m_ViewPager = viewPager;
        m_RefreshTimer = new Timer();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.userfeed_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        m_EmptyPostsTextView = view.findViewById(R.id.user_feed_empty_recycle_view);

        m_UserFeedRecycleView = view.findViewById(R.id.user_feed_recycle_view);
        m_UserFeedRecycleView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        m_UserFeedAdapter = new UserFeedAdapter(this);
        m_UserFeedAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver()
        {
            @Override
            public void onChanged()
            {
                super.onChanged();
                m_EmptyPostsTextView.setVisibility(m_UserFeedAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount)
            {
                super.onItemRangeInserted(positionStart, itemCount);
                m_EmptyPostsTextView.setVisibility(m_UserFeedAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount)
            {
                super.onItemRangeRemoved(positionStart, itemCount);
                m_EmptyPostsTextView.setVisibility(m_UserFeedAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });
        m_UserFeedRecycleView.setAdapter(m_UserFeedAdapter);

        m_SwipeRefreshLayout = view.findViewById(R.id.user_feed_swipe_refresh_layout);
        m_SwipeRefreshLayout.setOnRefreshListener(() -> {
            m_UserFeedAdapter.refresh(() -> {
                m_UserFeedRecycleView.smoothScrollToPosition(0);
                m_SwipeRefreshLayout.setRefreshing(false);
                m_SwipeRefreshLayout.setEnabled(false);
                m_RefreshTimer.schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        m_SwipeRefreshLayout.setEnabled(true);
                    }
                }, 2500);
            });
        });
        m_SwipeRefreshLayout.setDistanceToTriggerSync(750);

        m_PopupUserFeed = new PopupUserFeed(getActivity(), view, this);

        TextView userFeedHeaderText = view.findViewById(R.id.username_text_view);
        String username = "Temp-Username";
        Context context = getContext();
        if (context != null)
        {
            FirebaseUser loggedInUser = FirebaseAuth.getInstance().getCurrentUser();
            String firebaseUsername = loggedInUser.getDisplayName();
            if (firebaseUsername != null && !firebaseUsername.isEmpty())
            {
                username = context.getString(R.string.user_feed_username, firebaseUsername);
            }
        }
        userFeedHeaderText.setText(username);

        Button imageButton = view.findViewById(R.id.button_user_settings);
        imageButton.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        });
    }

    public void sharePost(double lat, double lon)
    {
        Uri sharePostURI = Uri.parse("https://www.google.com/maps/search/?api=1&" + String.format("query=%s,%s", lat, lon));
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out my AR post here! " + sharePostURI.toString());
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    public void popupPostComments(Post post, Post.PostRefreshListener listener)
    {
        m_PopupUserFeed.launch(PopupUserFeed.PopupType.COMMENTS, post, listener);
    }

    public void popupPostLike(Post post, Post.PostRefreshListener listener)
    {
        m_PopupUserFeed.launch(PopupUserFeed.PopupType.LIKES, post, listener);
    }

    @Override
    public void onPanelSlide(View panel, float slideOffset)
    {

    }

    @Override
    public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState)
    {
        m_ViewPager.setUserInputEnabled(previousState != SlidingUpPanelLayout.PanelState.EXPANDED || newState != SlidingUpPanelLayout.PanelState.DRAGGING);
    }

    public boolean isPopupExpanded()
    {
        return SlidingUpPanelLayout.PanelState.EXPANDED == m_PopupUserFeed.getPanelState();
    }

    public void collapsePopup()
    {
        m_PopupUserFeed.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }
}