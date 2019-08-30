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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.PlaybackSupportFragment;
import androidx.leanback.app.PlaybackSupportFragmentGlueHost;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.BaseOnItemViewClickedListener;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowPresenter;
import androidx.core.content.ContextCompat;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.DisplayMetrics;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.glue.LeanbackPlayerGlue;
import com.scooter1556.sms.android.playback.PlaybackManager;
import com.scooter1556.sms.android.presenter.QueueItemPresenter;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.MediaUtils;

import java.util.List;

public class TvAudioPlayerFragment extends androidx.leanback.app.PlaybackSupportFragment implements Player.EventListener, PlaybackManager.PlaybackListener, LeanbackPlayerGlue.ActionListener {
    private static final String TAG = "TvAudioPlayerFragment";

    private static final int BACKGROUND_TYPE = PlaybackSupportFragment.BG_DARK;

    private static final int UPDATE_DELAY = 16;

    private static final int CARD_SIZE = 274;

    private ArrayObjectAdapter rowAdapter;
    private ClassPresenterSelector presenterSelector;

    private SimpleExoPlayer player;
    private LeanbackPlayerGlue playerGlue;
    private LeanbackPlayerAdapter playerAdapter;

    private Drawable defaultBackground;
    private DisplayMetrics displayMetrics;

    private MediaBrowserCompat mediaBrowser;

    private boolean isInitialised = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d(TAG, "onActivityCreated()");

        setControlsOverlayAutoHideEnabled(false);
        setupRows();

        // Connect a media browser to keep media service alive
        mediaBrowser = new MediaBrowserCompat(getActivity(),
                new ComponentName(getActivity(), MediaService.class), new MediaBrowserCompat.ConnectionCallback(), null);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");

        if(!isInitialised) {
            initialise();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(TAG, "onStart()");

        if(!BackgroundManager.getInstance(getActivity()).isAttached()) {
            prepareBackgroundManager();
        }

        if (mediaBrowser != null) {
            mediaBrowser.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        Log.d(TAG, "onStop()");

        BackgroundManager.getInstance(getActivity()).release();

        if (mediaBrowser != null) {
            mediaBrowser.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy()");

        // Remove playback manager listener
        PlaybackManager.getInstance().removeListener(this);

        // Remove listener
        player.removeListener(this);

        playerGlue = null;
        playerAdapter = null;
    }

    private void initialise() {
        Log.d(TAG, "initialise()");

        // Add listener for playback manager
        PlaybackManager.getInstance().addListener(this);

        // Setup player
        player = (SimpleExoPlayer) PlaybackManager.getInstance().getCurrentPlayer();
        player.addListener(this);

        // Setup player adapter
        playerAdapter = new LeanbackPlayerAdapter(getActivity(), player, UPDATE_DELAY);

        // Setup player glue
        playerGlue = new LeanbackPlayerGlue(getActivity(), LeanbackPlayerGlue.Mode.AUDIO, playerAdapter, this);
        playerGlue.setHost(new PlaybackSupportFragmentGlueHost(this));

        isInitialised = true;

        updateMetadata();
        updatePlayListRow();
    }

    private void prepareBackgroundManager() {
        Log.d(TAG, "prepareBackgroundManager()");

        setBackgroundType(BACKGROUND_TYPE);

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

    private void setupRows() {
        Log.d(TAG, "setupRows()");

        presenterSelector = new ClassPresenterSelector()
                .addClassPresenter(MediaElement.class, new QueueItemPresenter());
        rowAdapter = new ArrayObjectAdapter(presenterSelector);

        // Add heading
        rowAdapter.add(getString(R.string.heading_queue));

        setAdapter(rowAdapter);
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    public void skipToNext() {
        player.next();
    }

    public void skipToPrevious() {
        player.previous();
    }

    public void rewind() {
        playerGlue.rewind();
    }

    public void fastForward() {
        playerGlue.fastForward();
    }

    private void updateMetadata() {
        Log.d(TAG, "updateMetadata()");

        MediaDescriptionCompat mediaDescription = PlaybackManager.getInstance().getMediaDescription();

        if(mediaDescription == null || mediaDescription.getMediaId() == null) {
            return;
        }

        playerGlue.setTitle(mediaDescription.getTitle());
        playerGlue.setSubtitle(mediaDescription.getSubtitle());


        Glide.with(this)
                .asBitmap()
                .load(mediaDescription.getIconUri())
                .into(new SimpleTarget<Bitmap>(CARD_SIZE, CARD_SIZE) {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        if(playerGlue.getControlsRow() != null) {
                            playerGlue.getControlsRow().setImageBitmap(requireContext(), resource);
                            notifyPlaybackRowChanged();
                        }
                    }
                });

        // Get fanart
        Glide.with(this)
                .asBitmap()
                .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + MediaUtils.parseMediaId(mediaDescription.getMediaId()).get(1) + "/fanart?scale=" + displayMetrics.widthPixels)
                .into(new SimpleTarget<Bitmap>(displayMetrics.widthPixels, displayMetrics.heightPixels) {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        BackgroundManager.getInstance(getActivity()).setBitmap(resource);
                    }

                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                        BackgroundManager.getInstance(getActivity()).setDrawable(defaultBackground);
                    }
                });
    }

    private void updatePlayListRow() {
        // Get current play queue
        List<MediaElement> queue = PlaybackManager.getInstance().getQueue();

        rowAdapter.removeItems(1, rowAdapter.size());

        if (queue == null || queue.isEmpty()) {
            return;
        }

        // Populate playlist
        rowAdapter.addAll(1, queue);
    }

    @Override
    public void onActionClicked(int action) {
        Log.d(TAG, "onActionClicked() -> " + action);

        if (action == LeanbackPlayerGlue.ACTION_NEXT) {
            skipToNext();
        } else if (action == LeanbackPlayerGlue.ACTION_PREVIOUS) {
            skipToPrevious();
        }
    }

    @Override
    public void onQueuePositionChanged(int previousIndex, int newIndex) {
        updateMetadata();
    }

    @Override
    public void onPlayerChanged(Player player) {
        // Nothing to do here...
    }

    private final class ItemViewClickedListener implements BaseOnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Object row) {
            if (row instanceof MediaElement) {
                Log.d(TAG, "onItemClicked() -> Playlist Item");

                int position =  rowAdapter.indexOf(row) - 1;
                PlaybackManager.getInstance().selectQueueItem(position);
            }
        }
    }
}
