package com.scooter1556.sms.android.fragment.tv;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.DisplayMetrics;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.tv.TvDirectoryDetailsActivity;
import com.scooter1556.sms.android.activity.tv.TvMediaGridActivity;
import com.scooter1556.sms.android.presenter.MediaItemPresenter;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TvMusicFragment extends BrowseFragment implements OnItemViewClickedListener, OnItemViewSelectedListener {
    private static final String TAG = "TvMusicFragment";

    private static final int BACKGROUND_UPDATE_DELAY = 300;

    private static final int ROW_RECENTLY_ADDED = 0;
    private static final int ROW_RECENTLY_PLAYED = 1;

    private static final int ROW_COUNT = 2;

    private ArrayObjectAdapter rowsAdapter;

    private MediaBrowserCompat mediaBrowser;

    // Media Lists
    List<MediaBrowserCompat.MediaItem> recentlyAdded;
    List<MediaBrowserCompat.MediaItem> recentlyPlayed;

    // Background
    private final Handler handler = new Handler();
    private DisplayMetrics displayMetrics;
    private Drawable defaultBackground;
    private Timer backgroundTimer;
    private String backgroundURI;

    private final MediaBrowserCompat.SubscriptionCallback subscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(@NonNull String parentId,
                                             @NonNull List<MediaBrowserCompat.MediaItem> children) {
                    Log.d(TAG, "onChildrenLoaded() parentId=" + parentId);

                    if(children.isEmpty()) {
                        Log.d(TAG, "Result for " + parentId + " is empty");
                        return;
                    }

                    String id = MediaUtils.parseMediaId(parentId).get(0);

                    switch(id) {
                        case MediaUtils.MEDIA_ID_RECENTLY_ADDED_AUDIO:
                            recentlyAdded.clear();
                            recentlyAdded.addAll(children);
                            mediaBrowser.unsubscribe(MediaUtils.MEDIA_ID_RECENTLY_ADDED_AUDIO);
                            break;

                        case MediaUtils.MEDIA_ID_RECENTLY_PLAYED_AUDIO:
                            recentlyPlayed.clear();
                            recentlyPlayed.addAll(children);
                            mediaBrowser.unsubscribe(MediaUtils.MEDIA_ID_RECENTLY_PLAYED_AUDIO);
                            break;

                    }

                    setRows();
                }

                @Override
                public void onError(@NonNull String id) {
                    Log.e(TAG, "Media subscription error: " + id);
                }
            };

    private MediaBrowserCompat.ConnectionCallback connectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "onConnected()");

                    if (isDetached()) {
                        return;
                    }

                    // Subscribe to media browser events
                    if(rowsAdapter.size() < ROW_COUNT) {
                        mediaBrowser.subscribe(MediaUtils.MEDIA_ID_RECENTLY_ADDED_AUDIO, subscriptionCallback);
                        mediaBrowser.subscribe(MediaUtils.MEDIA_ID_RECENTLY_PLAYED_AUDIO, subscriptionCallback);
                    }
                }

                @Override
                public void onConnectionFailed() {
                    Log.d(TAG, "onConnectionFailed");
                }

                @Override
                public void onConnectionSuspended() {
                    Log.d(TAG, "onConnectionSuspended");
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        // Initialisation
        recentlyAdded = new ArrayList<>();
        recentlyPlayed = new ArrayList<>();

        // Initialise interface
        initialiseInterfaceElements();
        prepareEntranceTransition();

        setAdapter(rowsAdapter);
        setOnItemViewClickedListener(this);
        setOnItemViewSelectedListener(this);

        startEntranceTransition();

        // Subscribe to relevant media service callbacks
        mediaBrowser = new MediaBrowserCompat(getActivity(),
                new ComponentName(getActivity(), MediaService.class),
                connectionCallback, null);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart()");

        super.onStart();

        if (mediaBrowser != null) {
            mediaBrowser.connect();
        }

        if(!BackgroundManager.getInstance(getActivity()).isAttached()) {
            prepareBackgroundManager();
        }
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop()");

        super.onStop();

        if (mediaBrowser != null) {
            mediaBrowser.disconnect();
        }

        if (backgroundTimer != null) {
            backgroundTimer.cancel();
            backgroundTimer = null;
        }

        BackgroundManager.getInstance(getActivity()).release();
    }

    private void initialiseInterfaceElements() {
        setTitle(getString(R.string.heading_music));
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // Set fastLane background color
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.primary));

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.primary_dark));
    }

    private void setRows() {
        Log.d(TAG, "setRows()");

        rowsAdapter.clear();

        if(recentlyAdded != null && !recentlyAdded.isEmpty()) {
            HeaderItem recentlyAddedHeader = new HeaderItem(ROW_RECENTLY_ADDED, getString(R.string.heading_recently_added));
            MediaItemPresenter recentlyAddedPresenter = new MediaItemPresenter();
            ArrayObjectAdapter recentlyAddedRowAdapter = new ArrayObjectAdapter(recentlyAddedPresenter);

            // Add media elements to row
            for (MediaBrowserCompat.MediaItem element : recentlyAdded) {
                recentlyAddedRowAdapter.add(element);
            }

            rowsAdapter.add(new ListRow(recentlyAddedHeader, recentlyAddedRowAdapter));
        }

        // Recently Played
        if(recentlyPlayed != null && !recentlyPlayed.isEmpty()) {
            HeaderItem recentlyPlayedHeader = new HeaderItem(ROW_RECENTLY_PLAYED, getString(R.string.heading_recently_played));
            MediaItemPresenter recentlyPlayedPresenter = new MediaItemPresenter();
            ArrayObjectAdapter recentlyPlayedRowAdapter = new ArrayObjectAdapter(recentlyPlayedPresenter);

            // Add media elements to row
            for (MediaBrowserCompat.MediaItem element : recentlyPlayed) {
                recentlyPlayedRowAdapter.add(element);
            }

            rowsAdapter.add(new ListRow(recentlyPlayedHeader, recentlyPlayedRowAdapter));
        }
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        if (item instanceof MediaBrowserCompat.MediaItem) {
            MediaBrowserCompat.MediaItem mediaItem = (MediaBrowserCompat.MediaItem) item;

            if (mediaItem.isPlayable()) {
                MediaControllerCompat.getMediaController(getActivity()).getTransportControls().playFromMediaId(mediaItem.getMediaId(), null);
            } else if (mediaItem.isBrowsable()) {
                Intent intent = null;

                switch (MediaUtils.parseMediaId(mediaItem.getMediaId()).get(0)) {
                    case MediaUtils.MEDIA_ID_DIRECTORY:
                        intent = new Intent(getActivity(), TvMediaGridActivity.class);
                        break;

                    case MediaUtils.MEDIA_ID_DIRECTORY_AUDIO:
                        intent = new Intent(getActivity(), TvDirectoryDetailsActivity.class);
                        break;
                }

                if (intent != null) {
                    intent.putExtra(MediaUtils.EXTRA_MEDIA_ID, mediaItem.getMediaId());
                    intent.putExtra(MediaUtils.EXTRA_MEDIA_ITEM, mediaItem);
                    intent.putExtra(MediaUtils.EXTRA_MEDIA_TITLE, mediaItem.getDescription().getTitle());
                    getActivity().startActivity(intent);
                }
            } else {
                Log.w(TAG, "Ignoring MediaItem that is neither browsable nor playable: mediaID=" + mediaItem.getMediaId());
            }
        }
    };

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        if (item instanceof MediaBrowserCompat.MediaItem) {
            MediaBrowserCompat.MediaItem mediaItem = (MediaBrowserCompat.MediaItem) item;

            List<String> id = MediaUtils.parseMediaId(mediaItem.getMediaId());

            if (id.size() > 1) {
                backgroundURI = RESTService.getInstance().getConnection().getUrl() + "/image/" + id.get(1) + "/fanart?scale=" + displayMetrics.widthPixels;
                startBackgroundTimer();
            } else {
                BackgroundManager.getInstance(getActivity()).setDrawable(defaultBackground);
            }

            Log.d(TAG, "onItemSelected() -> " + mediaItem.getMediaId());
        }
    }

    private void prepareBackgroundManager() {
        Log.d(TAG, "prepareBackgroundManager()");

        // Setup background manager
        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());

        // Get default background
        defaultBackground = ContextCompat.getDrawable(getActivity(), R.drawable.default_background);
        backgroundManager.setDrawable(defaultBackground);

        // Get screen size
        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    }

    private void updateBackground() {
        // Get screen size
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        Glide.with(getActivity())
                .asBitmap()
                .load(backgroundURI)
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        BackgroundManager.getInstance(getActivity()).setBitmap(resource);
                    }

                    @Override public void onLoadFailed(Drawable errorDrawable) {
                        BackgroundManager.getInstance(getActivity()).setDrawable(defaultBackground);
                    }
                });

        backgroundTimer.cancel();
    }

    private void startBackgroundTimer() {
        if (backgroundTimer != null) {
            backgroundTimer.cancel();
        }

        backgroundTimer = new Timer();
        backgroundTimer.schedule(new TvMusicFragment.UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    private class UpdateBackgroundTask extends TimerTask {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (backgroundURI != null) {
                        updateBackground();
                    }
                }
            });
        }
    }
}
