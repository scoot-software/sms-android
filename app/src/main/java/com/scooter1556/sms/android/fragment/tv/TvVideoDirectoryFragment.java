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
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.tv.TvDirectoryDetailsActivity;
import com.scooter1556.sms.android.activity.tv.TvMediaGridActivity;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.presenter.DetailsDescriptionPresenter;
import com.scooter1556.sms.android.presenter.MediaElementPresenter;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.MediaUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TvVideoDirectoryFragment extends DetailsFragment {
    private static final String TAG = "TvVideoDirFragment";

    private static final int ACTION_PLAY = 0;

    private static final int DETAIL_THUMB_WIDTH = 274;
    private static final int DETAIL_THUMB_HEIGHT = 274;

    private MediaBrowserCompat.MediaItem mediaItem;
    private MediaBrowserCompat.MediaItem selectedMediaItem;
    private List<MediaBrowserCompat.MediaItem> mediaItems;
    private MediaBrowserCompat mediaBrowser;
    private static MediaControllerCompat mediaController;

    private ArrayObjectAdapter adapter;
    private ClassPresenterSelector presenterSelector;


    private BackgroundManager backgroundManager;
    private Drawable defaultBackground;
    private DisplayMetrics displayMetrics;

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

                    if(parentId.equals(mediaItem.getMediaId())) {
                        mediaItems.clear();
                        mediaItems.addAll(children);

                        setupMediaList();
                    } else {
                        addDirectoryRow(parentId, children);
                    }
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
                    mediaBrowser.subscribe(mediaItem.getMediaId(), subscriptionCallback);

                    try {
                        mediaController = new MediaControllerCompat(getActivity(), mediaBrowser.getSessionToken());
                        MediaControllerCompat.setMediaController(getActivity(), mediaController);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prepareBackgroundManager();

        // Initialise variables
        mediaItems = new ArrayList<>();
        mediaItem = ((TvDirectoryDetailsActivity) getActivity()).getMediaItem();

        setupAdapter();

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.primary_dark));

        // When a Related Movie item is clicked.
        setOnItemViewClickedListener(new ItemViewClickedListener());

        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ToDo: Implement search feature
                Toast.makeText(getActivity(), getString(R.string.error_search_not_implemented), Toast.LENGTH_SHORT).show();
            }
        });

        // Subscribe to relevant media service callbacks
        mediaBrowser = new MediaBrowserCompat(getActivity(),
                new ComponentName(getActivity(), MediaService.class),
                connectionCallback, null);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");

        setupDetailsOverview();
        setBackground();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();

        mediaBrowser.connect();
    }

    @Override
    public void onStop() {
        backgroundManager.release();
        mediaBrowser.disconnect();

        adapter.clear();

        super.onStop();
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

    private void setBackground() {
        List<String> id = MediaUtils.parseMediaId(mediaItem.getMediaId());

        if(id.size() < 2) {
            return;
        }

        Glide.with(getActivity())
                .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + id.get(1) + "/fanart/" + displayMetrics.widthPixels)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>(displayMetrics.widthPixels, displayMetrics.heightPixels) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap>
                            glideAnimation) {
                        backgroundManager.setBitmap(resource);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        backgroundManager.setDrawable(defaultBackground);
                    }
                });
    }

    private void setupAdapter() {
        // Set detail background and style.
        FullWidthDetailsOverviewRowPresenter detailsPresenter =
                new FullWidthDetailsOverviewRowPresenter(new DetailsDescriptionPresenter());

        detailsPresenter.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.primary_dark));
        detailsPresenter.setInitialState(FullWidthDetailsOverviewRowPresenter.STATE_FULL);

        prepareEntranceTransition();

        detailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if(action.getId() == ACTION_PLAY) {
                    if(mediaItems == null || mediaItems.isEmpty()) {
                        Toast.makeText(getActivity(), getString(R.string.error_media_unavailable), Toast.LENGTH_SHORT);
                        return;
                    }


                    // Find the first video element and play it
                    for(MediaBrowserCompat.MediaItem item : mediaItems) {
                        if(MediaUtils.parseMediaId(item.getMediaId()).get(0).equals(MediaUtils.MEDIA_ID_VIDEO)) {
                            mediaController.getTransportControls().playFromMediaId(item.getMediaId(), null);
                            break;
                        }
                    }
                }
            }
        });

        presenterSelector = new ClassPresenterSelector();
        presenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
        presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        adapter = new ArrayObjectAdapter(presenterSelector);
        setAdapter(adapter);
    }

    private void setupDetailsOverview() {
        List<String> id = MediaUtils.parseMediaId(mediaItem.getMediaId());

        if(id.size() < 2) {
            return;
        }

        final DetailsOverviewRow detailsRow = new DetailsOverviewRow(mediaItem);

        startEntranceTransition();

        Glide.with(getActivity())
                .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + id.get(1) + "/cover/" + DETAIL_THUMB_HEIGHT)
                .asBitmap()
                .dontAnimate()
                .error(defaultBackground)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(final Bitmap resource, GlideAnimation glideAnimation) {
                        detailsRow.setImageBitmap(getActivity(), resource);
                    }
                });

        SparseArrayObjectAdapter actionsAdapter = new SparseArrayObjectAdapter();

        actionsAdapter.set(ACTION_PLAY, new Action(ACTION_PLAY,
                getResources().getString(R.string.label_play), null));

        detailsRow.setActionsAdapter(actionsAdapter);

        adapter.add(detailsRow);
    }

    private void setupMediaList() {
        List<MediaBrowserCompat.MediaItem> directories = new ArrayList<>();
        List<MediaBrowserCompat.MediaItem> videos = new ArrayList<>();

        // Determine directory contents
        for(MediaBrowserCompat.MediaItem item : mediaItems) {
            switch(MediaUtils.parseMediaId(item.getMediaId()).get(0)) {
                case MediaUtils.MEDIA_ID_DIRECTORY:
                case MediaUtils.MEDIA_ID_DIRECTORY_VIDEO:
                    directories.add(item);
                    break;

                case MediaUtils.MEDIA_ID_VIDEO:
                    videos.add(item);
                    break;
            }
        }

        // Generate contents if necessary
        if(videos.size() > 1) {
            ArrayObjectAdapter contentsRowAdapter = new ArrayObjectAdapter(new MediaElementPresenter());
            HeaderItem header = new HeaderItem(0, getString(R.string.heading_contents));

            for (MediaBrowserCompat.MediaItem video : videos) {
                contentsRowAdapter.add(video);
            }

            adapter.add(new ListRow(header, contentsRowAdapter));
        }

        // Generate row for sub-directories
        if(!directories.isEmpty()) {
            for(MediaBrowserCompat.MediaItem directory : directories) {
                mediaBrowser.subscribe(directory.getMediaId(), subscriptionCallback);
            }
        }
    }

    private void addDirectoryRow(String parentId, List<MediaBrowserCompat.MediaItem> items) {
        MediaBrowserCompat.MediaItem directory= null;
        List<String> id = MediaUtils.parseMediaId(parentId);

        // Get directory
        for(MediaBrowserCompat.MediaItem item : mediaItems) {
            if(item.getMediaId().equals(parentId)) {
                directory = item;
            }
        }

        if(directory == null || items.size() == 0 || id.size() < 2) {
            return;
        }

        final ArrayObjectAdapter directoryRowAdapter = new ArrayObjectAdapter(new MediaElementPresenter());
        final HeaderItem header = new HeaderItem(Long.parseLong(id.get(1)), String.valueOf(directory.getDescription().getTitle()));

        for(MediaBrowserCompat.MediaItem item : items) {
            directoryRowAdapter.add(item);
        }

        adapter.add(new ListRow(header, directoryRowAdapter));
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof MediaBrowserCompat.MediaItem) {
                if(((MediaBrowserCompat.MediaItem) item).isPlayable()) {
                    mediaController.getTransportControls().playFromMediaId(((MediaBrowserCompat.MediaItem) item).getMediaId(), null);
                } else if(((MediaBrowserCompat.MediaItem) item).isBrowsable()) {
                    Intent intent = new Intent(getActivity(), TvMediaGridActivity.class);
                    intent.putExtra(MediaUtils.EXTRA_MEDIA_ID, mediaItem.getMediaId());
                    intent.putExtra(MediaUtils.EXTRA_MEDIA_ITEM, mediaItem);
                    intent.putExtra(MediaUtils.EXTRA_MEDIA_TITLE, mediaItem.getDescription().getTitle());
                    getActivity().startActivity(intent);
                }
            }
        }
    }
}
