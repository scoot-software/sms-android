package com.scooter1556.sms.android.fragment.tv;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.tv.TvAudioPlaybackActivity;
import com.scooter1556.sms.android.activity.tv.TvAudioSettingsActivity;
import com.scooter1556.sms.android.activity.tv.TvConnectionActivity;
import com.scooter1556.sms.android.activity.tv.TvDirectoryDetailsActivity;
import com.scooter1556.sms.android.activity.tv.TvMediaGridActivity;
import com.scooter1556.sms.android.activity.tv.TvTranscodeSettingsActivity;
import com.scooter1556.sms.android.activity.tv.TvVideoSettingsActivity;
import com.scooter1556.sms.android.presenter.MediaDescriptionPresenter;
import com.scooter1556.sms.android.presenter.MediaElementPresenter;
import com.scooter1556.sms.android.presenter.MediaFolderPresenter;
import com.scooter1556.sms.android.presenter.SettingsItemPresenter;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TvMainFragment extends BrowseFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "TvMainFragment";

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int ELEMENTS_TO_LOAD = 10;

    private static final int ROW_NOW_PLAYING = 0;
    private static final int ROW_MEDIA_BROWSER = 1;
    private static final int ROW_RECENTLY_ADDED = 2;
    private static final int ROW_RECENTLY_PLAYED = 3;
    private static final int ROW_SETTINGS = 4;

    private ArrayObjectAdapter rowsAdapter;

    private MediaBrowserCompat mediaBrowser;
    private static MediaControllerCompat mediaController;

    // Media Lists
    List<MediaBrowserCompat.MediaItem> mediaFolders;
    List<MediaBrowserCompat.MediaItem> recentlyAdded;
    List<MediaBrowserCompat.MediaItem> recentlyPlayed;

    // Background
    private final Handler handler = new Handler();
    private DisplayMetrics displayMetrics;
    private Drawable defaultBackground;
    private Timer backgroundTimer;
    private String backgroundURI;
    private BackgroundManager backgroundManager;

    private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
                setRows();
            }
        }
    };

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
                        case MediaUtils.MEDIA_ID_FOLDERS:
                            mediaFolders.clear();
                            mediaFolders.addAll(children);
                            break;

                        case MediaUtils.MEDIA_ID_RECENTLY_ADDED:
                            recentlyAdded.clear();
                            recentlyAdded.addAll(children);
                            break;

                        case MediaUtils.MEDIA_ID_RECENTLY_PLAYED:
                            recentlyPlayed.clear();
                            recentlyPlayed.addAll(children);
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

                    // Subscribe to media browser event
                    mediaBrowser.subscribe(MediaUtils.MEDIA_ID_FOLDERS, subscriptionCallback);
                    mediaBrowser.subscribe(MediaUtils.MEDIA_ID_RECENTLY_ADDED, subscriptionCallback);
                    mediaBrowser.subscribe(MediaUtils.MEDIA_ID_RECENTLY_PLAYED, subscriptionCallback);

                    try {
                        mediaController = new MediaControllerCompat(getActivity(), mediaBrowser.getSessionToken());
                        MediaControllerCompat.setMediaController(getActivity(), mediaController);
                        mediaController.registerCallback(callback);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Failed to connect media controller", e);
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d(TAG, "onActivityCreated()");

        // Initialisation
        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        mediaFolders = new ArrayList<>();
        recentlyAdded = new ArrayList<>();
        recentlyPlayed = new ArrayList<>();

        // Initialise interface
        prepareBackgroundManager();
        initialiseInterfaceElements();
        initialiseEventListeners();
        prepareEntranceTransition();

        // Set adapter
        setAdapter(rowsAdapter);
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

        setRows();
    }

    @Override
    public void onDestroy() {
        if (backgroundTimer != null) {
            backgroundTimer.cancel();
            backgroundTimer = null;
        }

        backgroundManager = null;

        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mediaBrowser != null) {
            mediaBrowser.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mediaBrowser != null) {
            mediaBrowser.disconnect();
        }

        if (mediaController != null) {
            mediaController.unregisterCallback(callback);
        }

        backgroundManager.release();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "onSharedPreferencesChanged(" + key + ")");
    }

    private void prepareBackgroundManager() {
        // Setup background manager
        backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());

        // Get default background
        defaultBackground = ContextCompat.getDrawable(getActivity(), R.drawable.default_background);
        backgroundManager.setDrawable(defaultBackground);

        // Get screen size
        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    }

    private void initialiseInterfaceElements() {
        setBadgeDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.app_icon));
        setTitle(getString(R.string.app_name));
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // Set fastLane background color
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.primary));

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.primary_dark));
    }

    private void initialiseEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ToDo: Implement search feature
                Toast.makeText(getActivity(), getString(R.string.error_search_not_implemented), Toast.LENGTH_SHORT).show();
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private void setRows() {
        Log.d(TAG, "setRows()");

        rowsAdapter.clear();

        if(mediaController != null && mediaController.getMetadata() != null) {
            HeaderItem nowPlayingHeader = new HeaderItem(ROW_NOW_PLAYING, getString(R.string.heading_now_playing));
            MediaDescriptionPresenter mediaDescriptionPresenter = new MediaDescriptionPresenter();
            ArrayObjectAdapter nowPlayingRowAdapter = new ArrayObjectAdapter(mediaDescriptionPresenter);

            // Add current playlist item
            nowPlayingRowAdapter.add(mediaController.getMetadata().getDescription());

            rowsAdapter.add(new ListRow(nowPlayingHeader, nowPlayingRowAdapter));
        }

        // Media Browser
        if(mediaFolders != null && !mediaFolders.isEmpty()) {
            HeaderItem mediaBrowserHeader = new HeaderItem(ROW_MEDIA_BROWSER, getString(R.string.heading_media_folders));
            MediaFolderPresenter mediaFolderPresenter = new MediaFolderPresenter();
            ArrayObjectAdapter mediaBrowserRowAdapter = new ArrayObjectAdapter(mediaFolderPresenter);

            // Add media folders to row
            for (MediaBrowserCompat.MediaItem mediaFolder : mediaFolders) {
                mediaBrowserRowAdapter.add(mediaFolder);
            }

            rowsAdapter.add(new ListRow(mediaBrowserHeader, mediaBrowserRowAdapter));
        }

        // Recently Added
        if(recentlyAdded != null && !recentlyAdded.isEmpty()) {
            HeaderItem recentlyAddedHeader = new HeaderItem(ROW_RECENTLY_ADDED, getString(R.string.heading_recently_added));
            MediaElementPresenter recentlyAddedPresenter = new MediaElementPresenter();
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
            MediaElementPresenter recentlyPlayedPresenter = new MediaElementPresenter();
            ArrayObjectAdapter recentlyPlayedRowAdapter = new ArrayObjectAdapter(recentlyPlayedPresenter);

            // Add media elements to row
            for (MediaBrowserCompat.MediaItem element : recentlyPlayed) {
                recentlyPlayedRowAdapter.add(element);
            }

            rowsAdapter.add(new ListRow(recentlyPlayedHeader, recentlyPlayedRowAdapter));
        }

        // Settings
        HeaderItem settingsHeader = new HeaderItem(ROW_SETTINGS, getString(R.string.preferences_title));
        SettingsItemPresenter settingsPresenter = new SettingsItemPresenter();
        ArrayObjectAdapter settingsRowAdapter = new ArrayObjectAdapter(settingsPresenter);
        settingsRowAdapter.add(getString(R.string.preferences_title_connections));
        settingsRowAdapter.add(getString(R.string.preferences_title_audio));
        settingsRowAdapter.add(getString(R.string.preferences_title_video));
        settingsRowAdapter.add(getString(R.string.preferences_title_transcode));
        rowsAdapter.add(new ListRow(settingsHeader, settingsRowAdapter));
    }

    private void updateBackground() {
        // Get screen size
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        Glide.with(getActivity())
                .load(backgroundURI)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap>
                            glideAnimation) {
                        backgroundManager.setBitmap(resource);
                    }

                    @Override public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        backgroundManager.setDrawable(defaultBackground);
                    }
                });

        backgroundTimer.cancel();
    }

    private void startBackgroundTimer() {
        if (backgroundTimer != null) {
            backgroundTimer.cancel();
        }

        backgroundTimer = new Timer();
        backgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
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

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof String) {
                if (((String) item).contains(getString(R.string.preferences_title_audio))) {
                    Intent intent = new Intent(getActivity(), TvAudioSettingsActivity.class);
                    startActivity(intent);
                } else if (((String) item).contains(getString(R.string.preferences_title_video))) {
                    Intent intent = new Intent(getActivity(), TvVideoSettingsActivity.class);
                    startActivity(intent);
                } else if (((String) item).contains(getString(R.string.preferences_title_transcode))) {
                    Intent intent = new Intent(getActivity(), TvTranscodeSettingsActivity.class);
                    startActivity(intent);
                } else if (((String) item).contains(getString(R.string.preferences_title_connections))) {
                    Intent intent = new Intent(getActivity(), TvConnectionActivity.class);
                    startActivity(intent);
                }
            } else if (item instanceof MediaBrowserCompat.MediaItem) {
                MediaBrowserCompat.MediaItem mediaItem = (MediaBrowserCompat.MediaItem) item;

                if (mediaItem.isPlayable()) {
                    MediaControllerCompat.getMediaController(getActivity()).getTransportControls().playFromMediaId(mediaItem.getMediaId(), null);
                } else if (mediaItem.isBrowsable()) {
                    Intent intent = null;

                    switch (MediaUtils.parseMediaId(mediaItem.getMediaId()).get(0)) {
                        case MediaUtils.MEDIA_ID_FOLDER:
                        case MediaUtils.MEDIA_ID_DIRECTORY:
                            intent = new Intent(getActivity(), TvMediaGridActivity.class);
                            break;

                        case MediaUtils.MEDIA_ID_DIRECTORY_AUDIO:
                        case MediaUtils.MEDIA_ID_DIRECTORY_VIDEO:
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
            } else if(item instanceof MediaDescriptionCompat) {
                // Now Playing
                Intent intent = new Intent(getActivity(), TvAudioPlaybackActivity.class);
                getActivity().startActivity(intent);
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof String) {
                backgroundManager.setDrawable(defaultBackground);
            } else if (item instanceof MediaBrowserCompat.MediaItem) {
                MediaBrowserCompat.MediaItem mediaItem = (MediaBrowserCompat.MediaItem) item;

                List<String> id = MediaUtils.parseMediaId(mediaItem.getMediaId());

                if (id.size() > 1) {
                    backgroundURI = RESTService.getInstance().getConnection().getUrl() + "/image/" + id.get(1) + "/fanart/" + displayMetrics.widthPixels;
                    startBackgroundTimer();
                } else {
                    backgroundManager.setDrawable(defaultBackground);
                }
            }
        }
    }
}
