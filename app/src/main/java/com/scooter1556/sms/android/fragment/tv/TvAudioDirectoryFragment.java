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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.app.DetailsFragmentBackgroundController;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.BaseOnItemViewClickedListener;
import android.support.v17.leanback.widget.BaseOnItemViewSelectedListener;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.tv.TvDirectoryDetailsActivity;
import com.scooter1556.sms.android.presenter.AudioItemPresenter;
import com.scooter1556.sms.android.presenter.DetailsDescriptionPresenter;
import com.scooter1556.sms.android.presenter.HeaderPresenter;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;

public class TvAudioDirectoryFragment extends DetailsFragment {
    private static final String TAG = "TvAudioDirFragment";

    private static final int ACTION_PLAY = 0;
    private static final int ACTION_ADD_AND_PLAY = 1;
    private static final int ACTION_ADD_TO_PLAYLIST = 2;

    private static final int DETAIL_THUMB_SIZE = 274;

    private MediaBrowserCompat.MediaItem mediaItem;
    private MediaDescriptionCompat selectedMediaItem;
    private List<MediaBrowserCompat.MediaItem> mediaItems;
    private MediaBrowserCompat mediaBrowser;
    private static MediaControllerCompat mediaController;

    private ArrayObjectAdapter adapter;
    private ClassPresenterSelector presenterSelector;

    private final DetailsFragmentBackgroundController backgroundController = new DetailsFragmentBackgroundController(this);
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

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());

        // Initialise variables
        mediaItems = new ArrayList<>();
        mediaItem = ((TvDirectoryDetailsActivity) getActivity()).getMediaItem();

        setupAdapter();

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.primary_dark));

        setOnSearchClickedListener(new OnClickListener() {
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

        setBackground();
        setupDetailsOverview();
    }

    @Override
    public void onStart() {
        super.onStart();

        mediaBrowser.connect();
    }

    @Override
    public void onStop() {
        mediaBrowser.disconnect();

        adapter.clear();

        super.onStop();
    }

    private void setBackground() {
        List<String> id = MediaUtils.parseMediaId(mediaItem.getMediaId());

        if(id.size() < 2) {
            return;
        }

        // Get screen size
        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        // Get default background
        defaultBackground = ContextCompat.getDrawable(getActivity(), R.drawable.default_background);

        backgroundController.enableParallax();
        backgroundController.setCoverBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.default_background));

        Glide.with(getActivity())
                .asBitmap()
                .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + id.get(1) + "/fanart/" + displayMetrics.widthPixels)
                .into(new SimpleTarget<Bitmap>(displayMetrics.widthPixels, displayMetrics.heightPixels) {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        backgroundController.setCoverBitmap(resource);
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
                    mediaController.addQueueItem(mediaItem.getDescription(), -1);
                    Toast.makeText(getActivity(), getString(R.string.notification_audio_directory_play_next), Toast.LENGTH_SHORT).show();
                } else if (action.getId() == ACTION_ADD_TO_PLAYLIST) {
                    mediaController.addQueueItem(mediaItem.getDescription());
                    Toast.makeText(getActivity(), getString(R.string.notification_audio_directory_add_to_queue), Toast.LENGTH_SHORT).show();
                }
            }
        });

        presenterSelector = new ClassPresenterSelector();
        presenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
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

        RequestOptions options = new RequestOptions()
                .error(defaultBackground)
                .dontAnimate();

        Glide.with(getActivity())
                .asBitmap()
                .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + id.get(1) + "/cover/" + DETAIL_THUMB_SIZE)
                .apply(options)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
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
        presenterSelector.addClassPresenter(String.class, new HeaderPresenter());
        presenterSelector.addClassPresenter(MediaDescriptionCompat.class, new AudioItemPresenter());

        // Add heading
        adapter.add(getString(R.string.heading_tracklist));

        for(MediaBrowserCompat.MediaItem item : mediaItems) {
            adapter.add(item.getDescription());
        }
    }

    private final class ItemViewClickedListener implements BaseOnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Object row) {
            Log.d(TAG, "onItemClicked()");

            if (row instanceof MediaDescriptionCompat) {
                //showOptionsMenu(itemViewHolder.view);
                mediaController.getTransportControls().playFromMediaId(selectedMediaItem.getMediaId(), null);
            }
        }
    }

    private final class ItemViewSelectedListener implements BaseOnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Object row) {
            if (row instanceof MediaDescriptionCompat) {
                selectedMediaItem = (MediaDescriptionCompat) row;
                Log.d(TAG, ("Item Selected: " + ((MediaDescriptionCompat) row).getMediaId()));
            }
        }
    }
}
