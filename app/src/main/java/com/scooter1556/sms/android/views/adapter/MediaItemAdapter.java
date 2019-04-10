package com.scooter1556.sms.android.views.adapter;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.views.viewholder.MediaItemViewHolder;
import com.scooter1556.sms.android.views.viewholder.PlaylistItemViewHolder;

import java.util.List;

public class MediaItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MediaItemAdapter";

    public static final int VIEW_TYPE_UNDEFINED = -1;
    public static final int VIEW_TYPE_AUDIO = 0;
    public static final int VIEW_TYPE_VIDEO = 1;
    public static final int VIEW_TYPE_PLAYLIST = 2;

    private Context context;
    private List<MediaBrowserCompat.MediaItem> list;
    private OnClicked onClicked;

    public interface OnClicked {
        void onItemClicked(int position);
        void onMenuItemClicked(MenuItem item, int position);
    }

    public MediaItemAdapter(Context context, List<MediaBrowserCompat.MediaItem> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getItemViewType(int position) {
        MediaBrowserCompat.MediaItem item = list.get(position);

        if (item == null || item.getMediaId() == null) {
            return VIEW_TYPE_UNDEFINED;
        }

        switch (MediaUtils.parseMediaId(item.getMediaId()).get(0)) {
            case MediaUtils.MEDIA_ID_PLAYLIST:
            case MediaUtils.MEDIA_ID_PLAYLISTS:
                return VIEW_TYPE_PLAYLIST;

            default:
                return VIEW_TYPE_AUDIO;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VIEW_TYPE_PLAYLIST:
                View playlistView = inflater.inflate(R.layout.item_media_playlist, parent, false);
                viewHolder = new PlaylistItemViewHolder(playlistView);
                break;

            default:
                View defaultView = inflater.inflate(R.layout.item_media_audio, parent, false);
                viewHolder = new MediaItemViewHolder(defaultView);
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int position) {

        switch (viewHolder.getItemViewType()) {
            case VIEW_TYPE_PLAYLIST:
                PlaylistItemViewHolder playlistItemViewHolder = (PlaylistItemViewHolder) viewHolder;
                bindPlaylistViewHolder(playlistItemViewHolder, position);
                break;

            default:
                MediaItemViewHolder mediaItemViewHolder = (MediaItemViewHolder) viewHolder;
                bindDefaultViewHolder(mediaItemViewHolder, position);
                break;
        }
    }

    private void bindPlaylistViewHolder(PlaylistItemViewHolder viewHolder, int position) {
        MediaBrowserCompat.MediaItem item = list.get(position);

        if(list.get(position) != null) {
            CharSequence title = item.getDescription().getTitle();
            CharSequence subtitle = item.getDescription().getSubtitle();

            viewHolder.title.setText(title);

            if (viewHolder.subtitle != null) {
                viewHolder.subtitle.setText(subtitle);
            }

            Glide.with(context)
                    .load(item.getDescription().getIconUri())
                    .into((viewHolder).image);

            viewHolder.menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Inflate menu
                    PopupMenu popup = new PopupMenu(view.getContext(), view);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.menu_playlist_item, popup.getMenu());
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            onClicked.onMenuItemClicked(item, position);
                            return true;
                        }
                    });

                    popup.show();
                }
            });

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClicked.onItemClicked(position);
                }
            });
        }
    }

    private void bindDefaultViewHolder(MediaItemViewHolder viewHolder, int position) {
        MediaBrowserCompat.MediaItem item = list.get(position);

        if(item != null) {
            CharSequence title = item.getDescription().getTitle();
            CharSequence subtitle = item.getDescription().getSubtitle();

            viewHolder.title.setText(title);

            if (viewHolder.subtitle != null) {
                viewHolder.subtitle.setText(subtitle);
            }

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClicked.onItemClicked(position);
                }
            });

            if(item.getMediaId()== null) {
                return;
            }

            RequestOptions options;

            switch (MediaUtils.parseMediaId(item.getMediaId()).get(0)) {
                case MediaUtils.MEDIA_ID_DIRECTORY_AUDIO:
                    options = new RequestOptions()
                            .error(R.drawable.ic_album);

                    Glide.with(context)
                            .load(item.getDescription().getIconUri())
                            .apply(options)
                            .into((viewHolder).image);
                    break;

                case MediaUtils.MEDIA_ID_AUDIO:
                    options = new RequestOptions()
                            .error(R.drawable.ic_music);

                    Glide.with(context)
                            .load(item.getDescription().getIconUri())
                            .apply(options)
                            .into((viewHolder).image);
                    break;

                case MediaUtils.MEDIA_ID_VIDEO: case MediaUtils.MEDIA_ID_COLLECTION:
                    options = new RequestOptions()
                            .error(R.drawable.ic_movie);

                    Glide.with(context)
                            .load(item.getDescription().getIconUri())
                            .apply(options)
                            .into((viewHolder).image);
                    break;

                default:
                    options = new RequestOptions()
                            .error(R.drawable.ic_folder);

                    Glide.with(context)
                            .load(item.getDescription().getIconUri())
                            .apply(options)
                            .into((viewHolder).image);
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

    public void setOnClick(OnClicked onClicked) {
        this.onClicked=onClicked;
    }
}
