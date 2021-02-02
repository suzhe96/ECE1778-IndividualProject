package com.example.instgram;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import java.util.LinkedList;

/**
 * Shows how to implement a simple Adapter for a RecyclerView.
 * Demonstrates how to add a click handler for each item in the ViewHolder.
 */
public class CommentAdapter extends
        RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    // Utils
    private Utils utils = null;

    private final LinkedList<CommentItems> commentItemList;
    private final LayoutInflater commentInflater;

    class CommentViewHolder extends RecyclerView.ViewHolder {
        public final ImageView commentViewPic;
        public final TextView commentTextUser;
        public final TextView commentTextWord;
        final CommentAdapter commentAdapter;

        public CommentViewHolder(View itemView, CommentAdapter adapter) {
            super(itemView);
            commentViewPic = itemView.findViewById(R.id.commentItemImage);
            commentTextUser = itemView.findViewById(R.id.commentItemUserName);
            commentTextWord = itemView.findViewById(R.id.commentItemWord);
            this.commentAdapter = adapter;
        }
    }

    public CommentAdapter(Context context, LinkedList<CommentItems> commentList) {
        utils = new Utils();
        commentInflater = LayoutInflater.from(context);
        this.commentItemList = commentList;
    }

    @Override
    public CommentAdapter.CommentViewHolder onCreateViewHolder(ViewGroup parent,
                                                                     int viewType) {
        // Inflate an item view.
        View mItemView = commentInflater.inflate(
                R.layout.comment_item, parent, false);
        return new CommentViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(CommentAdapter.CommentViewHolder holder,
                                 int position) {
        // Retrieve the data for that position.
        CommentItems mCurrent = commentItemList.get(position);
        // Add the data to the view holder.
        Bitmap bitmap = BitmapFactory.decodeByteArray(
                mCurrent.getUserPic(), 0, mCurrent.getUserPic().length);
        holder.commentViewPic.setImageBitmap(utils.toRoundBitMap(
                utils.cropProfileBitmap(bitmap, true)
        ));
        holder.commentTextUser.setText(mCurrent.getUserName());
        holder.commentTextWord.setText(mCurrent.getUserComment());
    }

    @Override
    public int getItemCount() {
        return commentItemList.size();
    }
}
