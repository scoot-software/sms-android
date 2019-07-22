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
package com.scooter1556.sms.android.activity;

import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.RepeatModeUtil;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.playback.PlaybackManager;

/**
 * A full screen player that shows the current playing music with a background image
 * depicting the album art. The activity also has controls to seek/pause/play media.
 */
public class FullScreenPlayerActivity extends ActionBarCastActivity implements PlaybackManager.PlaybackListener {
    private static final String TAG = "FSPlayerActivity";

    private PlayerView playerView;
    private ImageView artwork;
    private TextView title;
    private TextView subtitle;
    private TextView extra;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_full_screen_player);
        initialiseToolbar();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        playerView = findViewById(R.id.player);
        artwork = findViewById(R.id.player_artwork);

        playerView.setControllerHideOnTouch(false);
        playerView.setControllerShowTimeoutMs(0);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS);
        playerView.setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE | RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE | RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL);
        playerView.setShowShuffleButton(true);
        playerView.setRewindIncrementMs(0);
        playerView.setFastForwardIncrementMs(0);
        playerView.showController();

        title = findViewById(R.id.title);
        subtitle = findViewById(R.id.subtitle);
        extra = findViewById(R.id.extra);
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(TAG, "onStart()");

        PlaybackManager playbackManager = PlaybackManager.getInstance();
        Player player = playbackManager.getCurrentPlayer();

        if(player == null) {
            finish();
        } else {
            // Add listener
            playbackManager.addListener(this);

            playerView.setPlayer(player);
            updateMetadata();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        Log.d(TAG, "onStop()");

        // Remove listener
        PlaybackManager.getInstance().removeListener(this);
    }

    @Override
    public void onQueuePositionChanged(int previousIndex, int newIndex) {
        Log.d(TAG, "onQueuePositionChanged()");

        updateMetadata();
    }

    private void updateMetadata() {
        Log.d(TAG, "updateMetadata()");

        int index = PlaybackManager.getInstance().getCurrentItemIndex();

        if(index == C.INDEX_UNSET) {
            return;
        }

        MediaDescriptionCompat mediaDescription = PlaybackManager.getInstance().getMediaDescription();

        if(mediaDescription == null) {
            finish();
            return;
        }

        Glide.with(this)
                .load(mediaDescription.getIconUri())
                .into(artwork);

        title.setText(mediaDescription.getTitle());
        subtitle.setText(mediaDescription.getSubtitle());
    }
}
