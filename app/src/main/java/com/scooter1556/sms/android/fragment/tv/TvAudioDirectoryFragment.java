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
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
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
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.tv.TvDirectoryDetailsActivity;
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

public class TvAudioDirectoryFragment extends DetailsFragment implements PopupMenu.OnMenuItemClickListener {
    private static final String TAG = "TvAudioDirFragment";

    private static final int ACTION_PLAY = 0;
    private static final int ACTION_ADD_AND_PLAY = 1;
    private static final int ACTION_ADD_TO_PLAYLIST = 2;

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

                    mediaItems.clear();
                    mediaItems.addAll(children);

                    setupMediaList();
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
        setupDetailsOverview();
        setBackground();

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
    public void onStart() {
        super.onStart();

        mediaBrowser.connect();
    }

    @Override
    public void onStop() {
        backgroundManager.release();
        mediaBrowser.disconnect();

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
                    mediaController.getTransportControls().playFromMediaId(mediaItem.getMediaId(), null);
                } else if (action.getId() == ACTION_ADD_AND_PLAY) {
                    // TODO: Start playing
                    Toast.makeText(getActivity(), getString(R.string.notification_audio_directory_play_next), Toast.LENGTH_SHORT).show();
                } else if (action.getId() == ACTION_ADD_TO_PLAYLIST) {
                    // TODO: Start playing
                    Toast.makeText(getActivity(), getString(R.string.notification_audio_directory_add_to_queue), Toast.LENGTH_SHORT).show();
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

        final DetailsOverviewRow row = new DetailsOverviewRow(mediaItem);

        startEntranceTransition();

        Glide.with(getActivity())
                .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + id.get(1) + "/cover/" + DETAIL_THUMB_HEIGHT)
                .asBitmap()
                .dontAnimate()
                .error(defaultBackground)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(final Bitmap resource, GlideAnimation glideAnimation) {
                        row.setImageBitmap(getActivity(), resource);
                    }
                });

        SparseArrayObjectAdapter actionsAdapter = new SparseArrayObjectAdapter();

        actionsAdapter.set(ACTION_PLAY, new Action(ACTION_PLAY, getResources().getString(R.string.label_play), null));
        actionsAdapter.set(ACTION_ADD_AND_PLAY, new Action(ACTION_ADD_AND_PLAY, getResources().getString(R.string.label_play_next), null));
        actionsAdapter.set(ACTION_ADD_TO_PLAYLIST, new Action(ACTION_ADD_TO_PLAYLIST, getResources().getString(R.string.label_add_to_queue), null));

        row.setActionsAdapter(actionsAdapter);

        adapter.add(row);
    }

    private void setupMediaList() {
        // Generate tracklist
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new MediaElementPresenter());
        HeaderItem header = new HeaderItem(0, getString(R.string.heading_tracklist));

        for(MediaBrowserCompat.MediaItem item : mediaItems) {
            listRowAdapter.add(item);
        }

        adapter.add(new ListRow(header, listRowAdapter));
    }

    public void showOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(getActivity(), v);

        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.menu_audio_element);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.play:
                mediaController.getTransportControls().playFromMediaId(selectedMediaItem.getMediaId(), null);
                return true;
            case R.id.play_next:
                // TODO:
                return true;
            case R.id.add_to_queue:
                // TODO:
                return true;
            default:
                return false;
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            Log.d(TAG, "onItemClicked()");

            if (item instanceof MediaBrowserCompat.MediaItem) {
                selectedMediaItem = (MediaBrowserCompat.MediaItem) item;
                showOptionsMenu(itemViewHolder.view);
            }
        }
    }
}
