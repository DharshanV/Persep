package com.perception.persep.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.persep.R;
import com.perception.persep.adapters.UserFeedAdapter;

import java.util.Timer;
import java.util.TimerTask;

public class UserFeed_Fragment extends Fragment
{
    private static final String TAG = "UserFeed_Fragment";

    private RecyclerView m_UserFeedRecycleView;
    private SwipeRefreshLayout m_SwipeRefreshLayout;
    private Timer m_RefreshTimer;

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
        m_UserFeedRecycleView = view.findViewById(R.id.user_feed_recycle_view);
        m_UserFeedRecycleView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        m_UserFeedRecycleView.setAdapter(new UserFeedAdapter());

        m_SwipeRefreshLayout = view.findViewById(R.id.user_feed_swipe_refresh_layout);
        m_SwipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Refresh...");
            m_SwipeRefreshLayout.setRefreshing(false);
            m_SwipeRefreshLayout.setEnabled(false);
            m_RefreshTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    m_SwipeRefreshLayout.setEnabled(true);
                }
            }, 5000);
        });

        super.onViewCreated(view, savedInstanceState);
    }
}