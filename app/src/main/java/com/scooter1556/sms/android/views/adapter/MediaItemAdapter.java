package com.scooter1556.sms.android.views.adapter;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.h6ah4i.android.widget.advrecyclerview.utils.RecyclerViewAdapterUtils;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.listener.OnListItemClickListener;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.views.viewholder.MediaItemViewHolder;

import java.util.List;

public class MediaItemAdapter extends RecyclerView.Adapter<MediaItemViewHolder> implements View.OnClickListener {

    private static final String TAG = "MediaItemAdapter";

    private Context context;
    private int itemResourceId;
    private List<MediaBrowserCompat.MediaItem> list;
    private OnListItemClickListener onItemClickListener;

    public MediaItemAdapter(Context context, int itemResourceId, List<MediaBrowserCompat.MediaItem> list, OnListItemClickListener clickListener) {
        this.context = context;
        this.itemResourceId = itemResourceId;
        this.list = list;
        this.onItemClickListener = clickListener;
    }

    @Override
    public MediaItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(itemResourceId, parent, false);
        MediaItemViewHolder vh = new MediaItemViewHolder(v);
        vh.itemView.setOnClickListener(this);
        return vh;
    }

    @Override
    public void onBindViewHolder(MediaItemViewHolder holder, int position) {
        MediaBrowserCompat.MediaItem item = list.get(position);

        if(list.get(position) != null) {
            CharSequence title = item.getDescription().getTitle();
            CharSequence subtitle = item.getDescription().getSubtitle();

            holder.title.setText(title);

            if (holder.subtitle != null) {
                holder.subtitle.setText(subtitle);
            }

            RequestOptions options;

            switch (MediaUtils.parseMediaId(item.getMediaId()).get(0)) {
                case MediaUtils.MEDIA_ID_DIRECTORY_AUDIO:
                    options = new RequestOptions()
                            .error(R.drawable.ic_album);

                    Glide.with(context)
                            .load(item.getDescription().getIconUri())
                            .apply(options)
                            .into((holder).image);
                    break;

                case MediaUtils.MEDIA_ID_AUDIO:
                    options = new RequestOptions()
                            .error(R.drawable.ic_music);

                    Glide.with(context)
                            .load(item.getDescription().getIconUri())
                            .apply(options)
                            .into((holder).image);
                    break;

                case MediaUtils.MEDIA_ID_VIDEO: case MediaUtils.MEDIA_ID_COLLECTION:
                    options = new RequestOptions()
                            .error(R.drawable.ic_movie);

                    Glide.with(context)
                            .load(item.getDescription().getIconUri())
                            .apply(options)
                            .into((holder).image);
                    break;

                default:
                    options = new RequestOptions()
                            .error(R.drawable.ic_folder);

                    Glide.with(context)
                            .load(item.getDescription().getIconUri())
                            .apply(options)
                            .into((holder).image);
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
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
