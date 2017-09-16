package com.scooter1556.sms.android.views.adapter;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.h6ah4i.android.widget.advrecyclerview.utils.RecyclerViewAdapterUtils;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.listener.OnListItemClickListener;
import com.scooter1556.sms.android.views.viewholder.MediaFolderViewHolder;

import java.util.List;

public class MediaFolderAdapter extends RecyclerView.Adapter<MediaFolderViewHolder> implements View.OnClickListener {

    private static final String TAG = "MediaFolderAdapter";

    private Context context;
    private List<MediaBrowserCompat.MediaItem> list;
    private OnListItemClickListener onItemClickListener;

    public MediaFolderAdapter(Context context, List<MediaBrowserCompat.MediaItem> list, OnListItemClickListener clickListener) {
        this.context = context;
        this.list = list;
        this.onItemClickListener = clickListener;
    }

    @Override
    public MediaFolderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_folder, parent, false);
        MediaFolderViewHolder vh = new MediaFolderViewHolder(v);
        vh.itemView.setOnClickListener(this);
        return vh;
    }

    @Override
    public void onBindViewHolder(MediaFolderViewHolder holder, int position) {
        final MediaBrowserCompat.MediaItem item = list.get(position);

        holder.title.setText(item.getDescription().getTitle());
        holder.subtitle.setText(item.getDescription().getSubtitle());

        RequestOptions options1 = new RequestOptions()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.ic_placeholder_1)
                .error(R.drawable.ic_placeholder_1);

        RequestOptions options2 = new RequestOptions()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.ic_placeholder_2)
                .error(R.drawable.ic_placeholder_2);

        RequestOptions options3 = new RequestOptions()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.ic_placeholder_3)
                .error(R.drawable.ic_placeholder_3);

        RequestOptions options4 = new RequestOptions()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.ic_placeholder_4)
                .error(R.drawable.ic_placeholder_4);


        Glide.with(context)
                .load(item.getDescription().getIconUri())
                .apply(options1)
                .into((holder).image1);

        Glide.with(context)
                .load(item.getDescription().getIconUri())
                .apply(options2)
                .into((holder).image2);

        Glide.with(context)
                .load(item.getDescription().getIconUri())
                .apply(options3)
                .into((holder).image3);

        Glide.with(context)
                .load(item.getDescription().getIconUri())
                .apply(options4)
                .into((holder).image4);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public void onClick(View v) {
        RecyclerView rv = RecyclerViewAdapterUtils.getParentRecyclerView(v);
        RecyclerView.ViewHolder vh = rv.findContainingViewHolder(v);

        int rootPosition = vh.getAdapterPosition();
        if (rootPosition == RecyclerView.NO_POSITION) {
            return;
        }

        // Determine adapter local position like this:
        RecyclerView.Adapter rootAdapter = rv.getAdapter();
        int localPosition = WrapperAdapterUtils.unwrapPosition(rootAdapter, this, rootPosition);

        if(localPosition >= list.size()) {
            Log.d(TAG, "Item out of bounds: " + localPosition);
            return;
        }

        if (onItemClickListener != null) {
            onItemClickListener.onItemClicked(list.get(localPosition));
        }
    }
}
