package com.scooter1556.sms.android.fragment.tv;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseFragment;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.tv.TvAudioPlaybackActivity;
import com.scooter1556.sms.android.activity.tv.TvMediaBrowserActivity;
import com.scooter1556.sms.android.activity.tv.TvMusicActivity;
import com.scooter1556.sms.android.activity.tv.TvSettingsActivity;
import com.scooter1556.sms.android.activity.tv.TvVideoActivity;
import com.scooter1556.sms.android.domain.MenuItem;
import com.scooter1556.sms.android.presenter.MediaMetadataPresenter;
import com.scooter1556.sms.android.presenter.MenuItemPresenter;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.service.RESTService;

public class TvHomeFragment extends BrowseSupportFragment {
    private static final String TAG = "TvHomeFragment";

    private ArrayObjectAdapter rowsAdapter;
    private BackgroundManager backgroundManager;
    private Drawable defaultBackground;
    private String backgroundUri;

    private MediaBrowserCompat mediaBrowser;
    private static MediaControllerCompat mediaController;

    private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            Log.d(TAG, "onPlaybackStateChanged() -> " + state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            Log.d(TAG, "onMetadataChanged()");

            if (metadata != null) {
                updateNowPlayingRow(metadata);
            }
        }
    };

    private final MediaBrowserCompat.ConnectionCallback connectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    try {
                        connectToSession(mediaBrowser.getSessionToken());
                    } catch (RemoteException e) {
                        Log.e(TAG, "Could not connect media controller", e);
                    }
                }
            };

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        Log.d(TAG, "connectToSession()");

        mediaController = new MediaControllerCompat(getActivity(), token);

        MediaControllerCompat.setMediaController(getActivity(), mediaController);
        mediaController.registerCallback(callback);
        PlaybackStateCompat state = mediaController.getPlaybackState();
        MediaMetadataCompat metadata = mediaController.getMetadata();

        if (metadata != null) {
            updateNowPlayingRow(metadata);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");

        setupUI();
        setupRowAdapter();
        setupEventListeners();

        // Subscribe to relevant media service callbacks
        mediaBrowser = new MediaBrowserCompat(getActivity(),
                new ComponentName(getActivity(), MediaService.class),
                connectionCallback, null);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");

        updateBackground();
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart()");

        super.onStart();

        if (mediaBrowser != null && !mediaBrowser.isConnected()) {
            mediaBrowser.connect();
        }
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop()");

        super.onStop();

        BackgroundManager.getInstance(getActivity()).release();
    }

    private void setupRowAdapter() {
        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        createRows();
        setAdapter(rowsAdapter);
    }

    private void updateNowPlayingRow(MediaMetadataCompat metadata) {
        ArrayObjectAdapter nowPlayingAdapter = new ArrayObjectAdapter(new MediaMetadataPresenter());
        nowPlayingAdapter.add(metadata);
        HeaderItem headerItem = new HeaderItem(getString(R.string.heading_now_playing));

        if(rowsAdapter.size() > 1) {
            rowsAdapter.replace(0, new ListRow(headerItem, nowPlayingAdapter));
        } else {
            rowsAdapter.add(0, new ListRow(headerItem, nowPlayingAdapter));
        }

        // Update background
        backgroundUri = RESTService.getInstance().getConnection().getUrl() + "/image/" + metadata.getDescription().getMediaId() + "/fanart?scale=" + getResources().getDisplayMetrics().widthPixels;
        updateBackground();
    }

    private void createRows() {
        MenuItemPresenter menuItemPresenter = new MenuItemPresenter();
        ArrayObjectAdapter rowAdapter = new ArrayObjectAdapter(menuItemPresenter);
        rowAdapter.add(new MenuItem(ContextCompat.getDrawable(getActivity(), R.drawable.tv_menu_browse), getString(R.string.heading_media_browser)));
        rowAdapter.add(new MenuItem(ContextCompat.getDrawable(getActivity(), R.drawable.tv_menu_music), getString(R.string.heading_music)));
        rowAdapter.add(new MenuItem(ContextCompat.getDrawable(getActivity(), R.drawable.tv_menu_video), getString(R.string.heading_video)));
        rowAdapter.add(new MenuItem(ContextCompat.getDrawable(getActivity(), R.drawable.tv_menu_settings), getString(R.string.heading_settings)));
        rowsAdapter.add(new ListRow(rowAdapter));
    }

    private void setupUI() {
        setTitle(getString(R.string.app_name));
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.primary));
        setBadgeDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.app_icon));
        setHeadersState(HEADERS_DISABLED);

        // Setup background manager
        backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());

        // Get and set default background
        defaultBackground = ContextCompat.getDrawable(getActivity(), R.drawable.default_background);
        updateBackground();
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private void updateBackground() {
        // Check we have an image url to fetch and otherwise set default
        if(backgroundUri == null || backgroundUri.isEmpty()) {
            backgroundManager.setDrawable(defaultBackground);
            return;
        }

        // Get screen size
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;

        Glide.with(getActivity())
                .asBitmap()
                .load(backgroundUri)
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        BackgroundManager.getInstance(getActivity()).setBitmap(resource);
                    }

                    @Override public void onLoadFailed(Drawable errorDrawable) {
                        BackgroundManager.getInstance(getActivity()).setDrawable(defaultBackground);
                    }
                });
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {

        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
            Intent intent = null;

            if (item instanceof MenuItem) {
                if (((MenuItem) item).getTitle().contains(getString(R.string.heading_music))) {
                    intent = new Intent(getActivity(), TvMusicActivity.class);
                } else if (((MenuItem) item).getTitle().contains(getString(R.string.heading_video))) {
                    intent = new Intent(getActivity(), TvVideoActivity.class);
                } else if (((MenuItem) item).getTitle().contains(getString(R.string.heading_settings))) {
                    intent = new Intent(getActivity(), TvSettingsActivity.class);
                } else if (((MenuItem) item).getTitle().contains(getString(R.string.heading_media_browser))) {
                    intent = new Intent(getActivity(), TvMediaBrowserActivity.class);
                }
            } else if (item instanceof MediaMetadataCompat) {
                intent = new Intent(getActivity(), TvAudioPlaybackActivity.class);
            }

            if (intent != null) {
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
                startActivity(intent, bundle);
            }
        }
    }
}
