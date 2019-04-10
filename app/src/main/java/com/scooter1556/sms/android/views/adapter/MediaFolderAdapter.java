package com.scooter1556.sms.android.views.adapter;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.views.viewholder.MediaFolderViewHolder;

import java.util.List;

public class MediaFolderAdapter extends RecyclerView.Adapter<MediaFolderViewHolder> {

    private static final String TAG = "MediaFolderAdapter";

    private Context context;
    private List<MediaBrowserCompat.MediaItem> list;
    private OnItemClicked onClick;

    public MediaFolderAdapter(Context context, List<MediaBrowserCompat.MediaItem> list) {
        this.context = context;
        this.list = list;
    }

    public interface OnItemClicked {
        void onItemClick(int position);
    }

    @NonNull
    @Override
    public MediaFolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_folder, parent, false);
        return new MediaFolderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaFolderViewHolder holder, final int position) {
        final MediaBrowserCompat.MediaItem item = list.get(position);

        holder.title.setText(item.getDescription().getTitle());
        holder.subtitle.setText(item.getDescription().getSubtitle());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClick.onItemClick(position);
            }
        });

        RequestOptions options = new RequestOptions()
                .skipMemoryCache(true)
                .placeholder(R.drawable.ic_placeholder)
                .fallback(R.drawable.ic_placeholder);

        Glide.with(context)
                .load(item.getDescription().getIconUri())
                .apply(options)
                .into((holder).image);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void setOnClick(OnItemClicked onClick) {
        this.onClick=onClick;
    }
}
