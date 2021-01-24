package com.example.instgram;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.LinkedList;

/**
 * Shows how to implement a simple Adapter for a RecyclerView.
 * Demonstrates how to add a click handler for each item in the ViewHolder.
 */
public class ContentImgAdapter extends
        RecyclerView.Adapter<ContentImgAdapter.ContentImgViewHolder> {

    // Utils
    private Utils utils = null;

    private final LinkedList<Bitmap> mBitmapList;
    private final LayoutInflater mInflater;

    class ContentImgViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public final ImageView contentImgView;
        final ContentImgAdapter mAdapter;

        /**
         * Creates a new custom view holder to hold the view to display in
         * the RecyclerView.
         *
         * @param itemView The view in which to display the data.
         * @param adapter The adapter that manages the the data and views
         *                for the RecyclerView.
         */
        public ContentImgViewHolder(View itemView, ContentImgAdapter adapter) {
            super(itemView);
            contentImgView = itemView.findViewById(R.id.contentImgView);
            this.mAdapter = adapter;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // current context
            Context context = view.getContext();
            // click for full screen
            Intent contentImageFullScreenIntent =
                    new Intent(context, ContentImageFullScreen.class);
            contentImageFullScreenIntent.putExtra("extraContentImageBitmapByte",
                    utils.compressBitmapToByteArray(mBitmapList.get(getLayoutPosition())));
            view.getContext().startActivity(contentImageFullScreenIntent,
                    ActivityOptions.makeSceneTransitionAnimation(
                            (Activity) view.getContext(),
                            view, "sharedContentImageEnlarged").toBundle());
            return;
        }
    }

    public ContentImgAdapter(Context context, LinkedList<Bitmap> bitmapList) {
        utils = new Utils();
        mInflater = LayoutInflater.from(context);
        this.mBitmapList = bitmapList;
    }

    @Override
    public ContentImgAdapter.ContentImgViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        // Inflate an item view.
        View mItemView = mInflater.inflate(
                R.layout.content_image_item, parent, false);
        return new ContentImgViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(ContentImgAdapter.ContentImgViewHolder holder,
                                 int position) {
        // Retrieve the data for that position.
        Bitmap mCurrent = mBitmapList.get(position);
        // Add the data to the view holder.
        holder.contentImgView.setImageBitmap(mCurrent);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return mBitmapList.size();
    }
}
