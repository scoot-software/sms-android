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
import android.os.Build;
import android.os.Bundle;
import androidx.leanback.app.PlaybackFragment;
import androidx.leanback.app.VideoSupportFragment;
import androidx.leanback.app.VideoSupportFragmentGlueHost;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.glue.VideoPlayerGlue;
import com.scooter1556.sms.android.playback.PlaybackManager;

import java.util.List;

public class TvVideoPlayerFragment extends VideoSupportFragment implements TextRenderer.Output {
    private static final String TAG = "TvVideoPlaybackFragment";

    private static final int UPDATE_DELAY = 16;
    private static final int BACKGROUND_TYPE = PlaybackFragment.BG_LIGHT;

    public static final int ACTION_VIDEO_QUALITY_ID = 0x7f0f0014;
    public static final int ACTION_AUDIO_TRACK_ID = 0x7f0f0015;
    public static final int ACTION_SUBTITLE_TRACK_ID = 0x7f0f0016;

    private Player player;
    private VideoPlayerGlue playerGlue;
    private LeanbackPlayerAdapter playerAdapter;

    private ArrayObjectAdapter rowsAdapter;

    private SubtitleView subtitleView;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        subtitleView = (SubtitleView) getActivity().findViewById(R.id.subtitle_view);
        if (subtitleView != null) {
            subtitleView.setUserDefaultStyle();
            subtitleView.setUserDefaultTextSize();
        }

        setBackgroundType(BACKGROUND_TYPE);
        setFadingEnabled(false);
    }

    @Override
    public void onStart() {
        super.onStart();

        initialise();
    }

    @Override
    public void onResume() {
        super.onResume();

        initialise();
    }

    /** Pauses the player. */
    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onPause() {
        super.onPause();

        if (playerGlue != null && playerGlue.isPlaying()) {
            playerGlue.pause();
        }

        release();
    }

    @Override
    public void onStop() {
        super.onStop();

        release();
    }

    private void initialise() {
        player = PlaybackManager.getInstance().getCurrentPlayer();
        playerAdapter = new LeanbackPlayerAdapter(getActivity(), player, UPDATE_DELAY);
        playerGlue = new VideoPlayerGlue(getActivity(), playerAdapter);
        playerGlue.setHost(new VideoSupportFragmentGlueHost(this));
        playerGlue.playWhenPrepared();

        setupRows();

        //playerGlue.setTitle(video.title);
        //playerGlue.setSubtitle(video.description);
    }

    private void release() {
        playerGlue = null;
        playerAdapter = null;

        // Stop and reset player
        player.stop(true);
    }

    private void setupRows() {
        ClassPresenterSelector presenter = new ClassPresenterSelector();
        presenter.addClassPresenter(playerGlue.getControlsRow().getClass(), playerGlue.getPlaybackRowPresenter());

        rowsAdapter = new ArrayObjectAdapter(presenter);

        setAdapter(rowsAdapter);
    }

    public void skipToNext() {
        playerGlue.next();
    }

    public void skipToPrevious() {
        playerGlue.previous();
    }

    public void rewind() {
        playerGlue.rewind();
    }

    public void fastForward() {
        playerGlue.fastForward();
    }

    @Override
    public void onCues(List<Cue> cues) {
        if (subtitleView != null)
            subtitleView.onCues(cues);
    }
}
