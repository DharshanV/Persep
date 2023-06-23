package com.perception.persep.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.persep.R;
import com.perception.persep.album_data.Comment;
import com.perception.persep.album_data.Post;

import java.util.List;

public class PostCommentAdapter extends BaseAdapter
{
    private List<Comment> m_PostComments;
    private Context m_Context;
    private Post m_Post;

    public PostCommentAdapter(Context context, Post post, List<Comment> postComments)
    {
        m_Context = context;
        m_PostComments = postComments;
        m_Post = post;
    }

    @Override
    public int getCount()
    {
        return m_PostComments.size();
    }

    @Override
    public Object getItem(int position)
    {
        return m_PostComments.get(position);
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
            view = inflater.inflate(R.layout.post_comment, parent, false);
        } else {
            view = convertView;
        }

        Comment comment = m_PostComments.get(position);

        TextView commentUsernameTextView = view.findViewById(R.id.post_comment_username_text_view);
        commentUsernameTextView.setText(m_Post.m_UserData.m_Username);

        TextView commentTextView = view.findViewById(R.id.post_comment_text_view);
        commentTextView.setText(comment.m_CommentStr);

        return view;
    }
}
