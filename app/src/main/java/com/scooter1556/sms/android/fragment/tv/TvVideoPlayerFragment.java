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

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.app.PlaybackFragment;
import androidx.leanback.app.VideoSupportFragment;
import androidx.leanback.app.VideoSupportFragmentGlueHost;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.dialog.TrackSelectionDialog;
import com.scooter1556.sms.android.glue.VideoPlayerGlue;
import com.scooter1556.sms.android.playback.PlaybackManager;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.TrackSelectionUtils;

import java.util.List;

public class TvVideoPlayerFragment extends VideoSupportFragment implements TextRenderer.Output, Player.EventListener, VideoPlayerGlue.ActionListener {
    private static final String TAG = "TvVideoPlayerFragment";

    // Saved instance state keys.
    private static final String KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters";

    private static final int UPDATE_DELAY = 16;
    private static final int BACKGROUND_TYPE = PlaybackFragment.BG_LIGHT;

    private static final int CARD_SIZE = 240;

    public static final int ACTION_TRACK_SELECTION_ID = 0x7f0f0014;

    private Player player;
    private VideoPlayerGlue playerGlue;
    private LeanbackPlayerAdapter playerAdapter;
    private boolean isInitialised = false;

    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private TrackSelectionUtils trackSelectionUtils;
    private TrackGroupArray lastSeenTrackGroupArray;
    private boolean isShowingTrackSelectionDialog;

    private ArrayObjectAdapter rowsAdapter;

    private SubtitleView subtitleView;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated()");

        super.onActivityCreated(savedInstanceState);

        subtitleView = (SubtitleView) getActivity().findViewById(R.id.subtitle_view);
        if (subtitleView != null) {
            subtitleView.setUserDefaultStyle();
            subtitleView.setUserDefaultTextSize();
        }

        if (savedInstanceState != null) {
            trackSelectorParameters = savedInstanceState.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS);
        } else {
            trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder().build();
        }

        setBackgroundType(BACKGROUND_TYPE);
        setControlsOverlayAutoHideEnabled(true);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");

        super.onResume();

        if(isInitialised) {
            playerGlue.playWhenPrepared();
        } else {
            initialise();
        }
    }

    /** Pauses the player. */
    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");

        super.onPause();

        if (playerGlue != null && playerGlue.isPlaying()) {
            playerGlue.pause();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        super.onDestroy();

        // Stop and reset player
        player.stop(true);

        // Remove listener
        player.removeListener(this);

        playerGlue = null;
        playerAdapter = null;
        trackSelector = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        updateTrackSelectorParameters();
        outState.putParcelable(KEY_TRACK_SELECTOR_PARAMETERS, trackSelectorParameters);
    }

    private void initialise() {
        Log.d(TAG, "initialise()");

        player = PlaybackManager.getInstance().getCurrentPlayer();
        player.addListener(this);
        playerAdapter = new LeanbackPlayerAdapter(getActivity(), player, UPDATE_DELAY);
        playerGlue = new VideoPlayerGlue(getActivity(), playerAdapter, this);
        playerGlue.setHost(new VideoSupportFragmentGlueHost(this));

        trackSelector = PlaybackManager.getInstance().getCurrentTrackSelector();
        trackSelectionUtils = new TrackSelectionUtils(trackSelector);

        isInitialised = true;

        setupRows();
        updateMetadata();

        playerGlue.playWhenPrepared();
    }

    private void setupRows() {
        Log.d(TAG, "setupRows()");

        ClassPresenterSelector presenter = new ClassPresenterSelector();
        presenter.addClassPresenter(playerGlue.getControlsRow().getClass(), playerGlue.getPlaybackRowPresenter());

        rowsAdapter = new ArrayObjectAdapter(presenter);

        setAdapter(rowsAdapter);
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

        if(mediaDescription == null) {
            return;
        }

        playerGlue.setTitle(mediaDescription.getTitle());
        playerGlue.setSubtitle(mediaDescription.getSubtitle());

        if(playerGlue.getControlsRow() != null) {
            Glide.with(this)
                    .asBitmap()
                    .load(mediaDescription.getIconUri())
                    .into(new SimpleTarget<Bitmap>(CARD_SIZE, CARD_SIZE) {
                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            playerGlue.getControlsRow().setImageBitmap(getActivity(), resource);
                            rowsAdapter.notifyArrayItemRangeChanged(0, rowsAdapter.size());
                        }
                    });
        }
    }

    @Override
    public void onCues(List<Cue> cues) {
        if (subtitleView != null)
            subtitleView.onCues(cues);
    }

    private void updateTrackSelectorParameters() {
        if(trackSelector != null) {
            trackSelectorParameters = trackSelector.getParameters();
        }
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        Log.d(TAG, "onTracksChanged()");

        if (trackGroups != lastSeenTrackGroupArray) {
            lastSeenTrackGroupArray = trackGroups;

            // Update glue actions
            for (int i = 0; i < trackSelections.length; i++) {
                switch(player.getRendererType(i)) {
                    case C.TRACK_TYPE_AUDIO:
                        playerGlue.audioTrackAction.setIndex(i);
                        break;

                    case C.TRACK_TYPE_TEXT:
                        playerGlue.textTrackAction.setIndex(i);
                        break;
                }
            }
        }
    }

    @Override
    public void onActionClicked(int action) {
        Log.d(TAG, "onActionClicked() -> " + action);

        if (action == VideoPlayerGlue.ACTION_NEXT) {
            skipToNext();
        } else if (action == VideoPlayerGlue.ACTION_PREVIOUS) {
            skipToPrevious();
        } else if (action == VideoPlayerGlue.ACTION_AUDIO_TRACK) {
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

            if (mappedTrackInfo != null) {
                trackSelectionUtils.showSelectionDialog(getActivity(), trackSelector.getCurrentMappedTrackInfo(), playerGlue.audioTrackAction.getIndex());
            }
        } else if (action == VideoPlayerGlue.ACTION_TEXT_TRACK) {
             MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

            if (mappedTrackInfo != null) {
                trackSelectionUtils.showSelectionDialog(getActivity(), trackSelector.getCurrentMappedTrackInfo(), playerGlue.textTrackAction.getIndex());
            }
        }

    }
}
