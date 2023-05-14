package com.perception.persep.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.persep.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.perception.persep.activities.HomeActivity;
import com.perception.persep.activities.LoginActivity;
import com.perception.persep.activities.SettingsActivity;
import com.perception.persep.adapters.UserFeedAdapter;

import java.util.Timer;
import java.util.TimerTask;

public class UserFeed_Fragment extends Fragment
{
    private static final String TAG = "UserFeed_Fragment";

    private SwipeRefreshLayout m_SwipeRefreshLayout;
    private final Timer m_RefreshTimer;
    private UserFeedAdapter m_UserFeedAdapter;
    private RecyclerView m_UserFeedRecycleView;

    public UserFeed_Fragment()
    {
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

        m_UserFeedRecycleView = view.findViewById(R.id.user_feed_recycle_view);
        m_UserFeedRecycleView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        m_UserFeedAdapter = new UserFeedAdapter();
        m_UserFeedRecycleView.setAdapter(m_UserFeedAdapter);

        m_SwipeRefreshLayout = view.findViewById(R.id.user_feed_swipe_refresh_layout);
        m_SwipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Refresh...");
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

        TextView userFeedHeaderText = view.findViewById(R.id.username_text_view);
        String username = "Temp-Username";
        Context context = getContext();
        if (context != null) {
            FirebaseUser loggedInUser = FirebaseAuth.getInstance().getCurrentUser();
            String firebaseUsername = loggedInUser.getDisplayName();
            if (firebaseUsername != null && !firebaseUsername.isEmpty()) {
                username = context.getString(R.string.user_feed_username, firebaseUsername);
            }
        }
        userFeedHeaderText.setText(username);

        ImageButton imageButton = view.findViewById(R.id.button_user_settings);
        imageButton.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        });
    }
}