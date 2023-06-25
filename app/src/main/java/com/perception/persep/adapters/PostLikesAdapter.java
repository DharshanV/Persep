package com.perception.persep.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.persep.R;
import com.google.firebase.firestore.DocumentReference;
import com.perception.persep.album_data.Post;
import com.perception.persep.data_gatherer.UserManager;
import com.perception.persep.user_data.User;

import java.util.List;

public class PostLikesAdapter extends BaseAdapter
{
    private List<DocumentReference> m_PostLikedUsers;
    private Context m_Context;

    public PostLikesAdapter(Context context, List<DocumentReference> postLikedUsers)
    {
        m_Context = context;
        m_PostLikedUsers = postLikedUsers;
    }

    @Override
    public int getCount()
    {
        return m_PostLikedUsers.size();
    }

    @Override
    public Object getItem(int position)
    {
        return m_PostLikedUsers.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view;
        LayoutInflater inflater = (LayoutInflater) m_Context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            view = inflater.inflate(R.layout.post_liked_user, parent, false);
        } else {
            view = convertView;
        }

        DocumentReference userRef = m_PostLikedUsers.get(position);
        TextView likedUsernameTextView = view.findViewById(R.id.post_liked_user_username_text_view);
        UserManager.getInstance().getUserData(userRef, user -> likedUsernameTextView.setText(user.m_Username));

        return view;
    }
}
