package com.perception.persep.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.persep.R;
import com.perception.persep.adapters.UserFeedAdapter;

public class UserFeed_Fragment extends Fragment
{
    private static final String TAG = "UserFeed_Fragment";

    private RecyclerView m_UserFeedRecycleView;

    public UserFeed_Fragment()
    {
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
        super.onViewCreated(view, savedInstanceState);
    }
}