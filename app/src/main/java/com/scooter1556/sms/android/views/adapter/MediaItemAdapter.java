package com.scooter1556.sms.android.views.adapter;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.views.viewholder.MediaItemViewHolder;

import java.util.List;

public class MediaItemAdapter extends RecyclerView.Adapter<MediaItemViewHolder> {

    private static final String TAG = "MediaItemAdapter";

    private Context context;
    private int itemResourceId;
    private List<MediaBrowserCompat.MediaItem> list;
    private OnItemClicked onClick;

    public interface OnItemClicked {
        void onItemClick(int position);
    }

    public MediaItemAdapter(Context context, List<MediaBrowserCompat.MediaItem> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public MediaItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(itemResourceId, parent, false);
        MediaItemViewHolder vh = new MediaItemViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(MediaItemViewHolder holder, final int position) {
        MediaBrowserCompat.MediaItem item = list.get(position);

        if(list.get(position) != null) {
            CharSequence title = item.getDescription().getTitle();
            CharSequence subtitle = item.getDescription().getSubtitle();

            holder.title.setText(title);

            if (holder.subtitle != null) {
                holder.subtitle.setText(subtitle);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClick.onItemClick(position);
                }
            });

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

    public void setItemResourceId(int itemResourceId) {
        this.itemResourceId = itemResourceId;
    }

    public void setOnClick(OnItemClicked onClick) {
        this.onClick=onClick;
    }
}
