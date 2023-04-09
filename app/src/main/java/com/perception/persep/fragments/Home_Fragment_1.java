package com.perception.persep.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.persep.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Home_Fragment_1#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Home_Fragment_1 extends Fragment
{
    private static final String TAG = "Home_Fragment_1";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private RecyclerView recyclerView;

    public Home_Fragment_1()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Home_Fragment_1.
     */
    // TODO: Rename and change types and number of parameters
    public static Home_Fragment_1 newInstance(String param1, String param2)
    {
        Home_Fragment_1 fragment = new Home_Fragment_1();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_home__1, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        recyclerView = view.findViewById(R.id.recycleView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setAdapter(new RandomNumListAdapter(1234));

//        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
//            int lastX = 0;
//            @Override
//            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
//                switch (e.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        lastX = (int) e.getX();
//                        break;
//                    case MotionEvent.ACTION_MOVE:
//                        boolean isScrollingRight = e.getX() < lastX;
//                        if (isScrollingRight) {
////                            viewPager.setUserInputEnabled(true);
//                            Log.d(TAG, "ViewPager: True");
//                        } else {
////                            viewPager.setUserInputEnabled(false);
//                            Log.d(TAG, "ViewPager: false");
//                        }
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        lastX = 0;
//                        Log.d(TAG, "ViewPager: True");
////                        viewPager.setUserInputEnabled(true);
//                        break;
//                }
//                return false;
//            }
//
//            @Override
//            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
//            }
//
//            @Override
//            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
//
//            }
//        });
        super.onViewCreated(view, savedInstanceState);
    }
}