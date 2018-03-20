/*
 * Author: Scott Ware <scoot.software@gmail.com>
 * Copyright (c) 2015 Scott Ware
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.scooter1556.sms.android.fragment.tv;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.tv.TvDirectoryDetailsActivity;
import com.scooter1556.sms.android.activity.tv.TvMediaGridActivity;
import com.scooter1556.sms.android.module.GlideApp;
import com.scooter1556.sms.android.presenter.MediaItemPresenter;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TvMediaGridFragment extends android.support.v17.leanback.app.VerticalGridFragment {
    private static final String TAG = "TvMediaGridFragment";

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int NUM_COLUMNS = 5;

    private ArrayObjectAdapter adapter;
    private String mediaId;
    private List<MediaBrowserCompat.MediaItem> mediaItems;
    private MediaBrowserCompat mediaBrowser;

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

                    mediaItems.clear();
                    mediaItems.addAll(children);

                    setGrid();
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
                    mediaBrowser.subscribe(mediaId, subscriptionCallback);
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

        // Set media folder
        String title = getActivity().getIntent().getStringExtra(MediaUtils.EXTRA_MEDIA_TITLE);
        mediaId = getActivity().getIntent().getStringExtra(MediaUtils.EXTRA_MEDIA_ID);

        if(title == null || mediaId == null) {
            Toast.makeText(getActivity(), getString(R.string.error_loading_media), Toast.LENGTH_LONG).show();
            ActivityCompat.finishAfterTransition(getActivity());
        }

        if (savedInstanceState == null) {
            setTitle(title);
            prepareEntranceTransition();
        }

        // Initialise variables
        mediaItems = new ArrayList<>();
        adapter = new ArrayObjectAdapter(new MediaItemPresenter());

        // Initialise interface
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ToDo: Implement search feature
                Toast.makeText(getActivity(), getString(R.string.error_search_not_implemented), Toast.LENGTH_SHORT).show();
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.primary_dark));

        // Subscribe to relevant media service callbacks
        mediaBrowser = new MediaBrowserCompat(getActivity(),
                new ComponentName(getActivity(), MediaService.class),
                connectionCallback, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();

        mediaBrowser.connect();

        if(!BackgroundManager.getInstance(getActivity()).isAttached()) {
            prepareBackgroundManager();
        }
    }

    @Override
    public void onStop() {
        mediaBrowser.disconnect();

        if (backgroundTimer != null) {
            backgroundTimer.cancel();
            backgroundTimer = null;
        }

        BackgroundManager.getInstance(getActivity()).release();

        super.onStop();
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

    private void setGrid() {
        adapter.clear();

        if(!mediaItems.isEmpty()) {
            // Add media elements to grid
            for (MediaBrowserCompat.MediaItem item : mediaItems) {
                adapter.add(item);
            }
        }

        setAdapter(adapter);
        startEntranceTransition();
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof MediaBrowserCompat.MediaItem) {
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
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof MediaBrowserCompat.MediaItem) {
                MediaBrowserCompat.MediaItem mediaItem = (MediaBrowserCompat.MediaItem) item;
                List<String> id = MediaUtils.parseMediaId(mediaItem.getMediaId());

                if(id.size() > 1) {
                    backgroundURI = RESTService.getInstance().getConnection().getUrl() + "/image/" + id.get(1) + "/fanart?scale=" + displayMetrics.widthPixels;
                    startBackgroundTimer();
                }
            }
        }
    }

    //
    // Background
    //
    private void updateBackground() {
        // Get screen size
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        GlideApp.with(getActivity())
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
}
