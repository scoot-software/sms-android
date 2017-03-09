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

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.service.MediaService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * A full screen player that shows the current playing music with a background image
 * depicting the album art. The activity also has controls to seek/pause/play media.
 */
public class FullScreenPlayerActivity extends ActionBarCastActivity {
    private static final String TAG = "FSPlayerActivity";

    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

    private ImageView skipPrev;
    private ImageView skipNext;
    private ImageView playPause;
    private TextView start;
    private TextView end;
    private SeekBar seekbar;
    private TextView title;
    private TextView subtitle;
    private TextView extra;
    private ProgressBar loading;
    private View controllers;
    private Drawable pauseDrawable;
    private Drawable playDrawable;
    private ImageView backgroundImage;

    private String currentArtUrl;
    private final Handler handler = new Handler();
    private MediaBrowserCompat mediaBrowser;

    private final Runnable updateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> scheduleFuture;
    private PlaybackStateCompat lastPlaybackState;

    private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
                updateMediaDescription(metadata.getDescription());
                updateDuration(metadata);
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_player);
        initialiseToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        backgroundImage = (ImageView) findViewById(R.id.background_image);
        pauseDrawable = ContextCompat.getDrawable(this, R.drawable.ic_pause_white_48dp);
        playDrawable = ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_white_48dp);
        playPause = (ImageView) findViewById(R.id.play_pause);
        skipNext = (ImageView) findViewById(R.id.next);
        skipPrev = (ImageView) findViewById(R.id.prev);
        start = (TextView) findViewById(R.id.startText);
        end = (TextView) findViewById(R.id.endText);
        seekbar = (SeekBar) findViewById(R.id.seekBar);
        title = (TextView) findViewById(R.id.title);
        subtitle = (TextView) findViewById(R.id.subtitle);
        extra = (TextView) findViewById(R.id.extra);
        loading = (ProgressBar) findViewById(R.id.progressBar);
        controllers = findViewById(R.id.controllers);

        skipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.TransportControls controls = getSupportMediaController().getTransportControls();
                controls.skipToNext();
            }
        });

        skipPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.TransportControls controls = getSupportMediaController().getTransportControls();
                controls.skipToPrevious();
            }
        });

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackStateCompat state = getSupportMediaController().getPlaybackState();
                if (state != null) {
                    MediaControllerCompat.TransportControls controls = getSupportMediaController().getTransportControls();
                    switch (state.getState()) {
                        case PlaybackStateCompat.STATE_PLAYING: // fall through
                        case PlaybackStateCompat.STATE_BUFFERING:
                            controls.pause();
                            stopSeekbarUpdate();
                            break;
                        case PlaybackStateCompat.STATE_PAUSED:
                        case PlaybackStateCompat.STATE_STOPPED:
                            controls.play();
                            scheduleSeekbarUpdate();
                            break;
                    }
                }
            }
        });

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                start.setText(DateUtils.formatElapsedTime(progress / 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                getSupportMediaController().getTransportControls().seekTo(seekBar.getProgress());
                scheduleSeekbarUpdate();
            }
        });

        // Only update from the intent if we are not recreating from a config change:
        if (savedInstanceState == null) {
            updateFromParams(getIntent());
        }

        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MediaService.class), connectionCallback, null);
    }

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        MediaControllerCompat mediaController = new MediaControllerCompat(FullScreenPlayerActivity.this, token);

        if (mediaController.getMetadata() == null) {
            finish();
            return;
        }

        setSupportMediaController(mediaController);
        mediaController.registerCallback(callback);
        PlaybackStateCompat state = mediaController.getPlaybackState();
        updatePlaybackState(state);
        MediaMetadataCompat metadata = mediaController.getMetadata();

        if (metadata != null) {
            updateMediaDescription(metadata.getDescription());
            updateDuration(metadata);
        }

        updateProgress();

        if (state != null && (state.getState() == PlaybackStateCompat.STATE_PLAYING || state.getState() == PlaybackStateCompat.STATE_BUFFERING)) {
            scheduleSeekbarUpdate();
        }
    }

    private void updateFromParams(Intent intent) {
        if (intent != null) {
            MediaDescriptionCompat description = intent.getParcelableExtra(HomeActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION);

            if (description != null) {
                updateMediaDescription(description);
            }
        }
    }

    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        if (!executorService.isShutdown()) {
            scheduleFuture = executorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            handler.post(updateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL, PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekbarUpdate() {
        if (scheduleFuture != null) {
            scheduleFuture.cancel(false);
        }
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

        if (getSupportMediaController() != null) {
            getSupportMediaController().unregisterCallback(callback);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopSeekbarUpdate();
        executorService.shutdown();
    }

    private void updateMediaDescription(MediaDescriptionCompat description) {
        if (description == null) {
            return;
        }

        title.setText(description.getTitle());
        subtitle.setText(description.getSubtitle());

        // Cover Art
        Glide.with(this)
                .load(description.getIconUri().toString())
                .fallback(R.drawable.ic_not_interested_black_48dp)
                .into(backgroundImage);
    }

    private void updateDuration(MediaMetadataCompat metadata) {
        if (metadata == null) {
            return;
        }

        int duration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        seekbar.setMax(duration);
        end.setText(DateUtils.formatElapsedTime(duration/1000));
    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        if (state == null) {
            return;
        }

        lastPlaybackState = state;
        if (getSupportMediaController() != null && getSupportMediaController().getExtras() != null) {
            String castName = getSupportMediaController().getExtras().getString(MediaService.EXTRA_CONNECTED_CAST);
            String extraInfo = castName == null ? "" : getResources().getString(R.string.cast_to_device, castName);
            extra.setText(extraInfo);
        }

        switch(state.getState()) {

            case PlaybackStateCompat.STATE_PLAYING:
                loading.setVisibility(INVISIBLE);
                playPause.setVisibility(VISIBLE);
                playPause.setImageDrawable(pauseDrawable);
                controllers.setVisibility(VISIBLE);
                scheduleSeekbarUpdate();
                break;

            case PlaybackStateCompat.STATE_PAUSED:
                controllers.setVisibility(VISIBLE);
                loading.setVisibility(INVISIBLE);
                playPause.setVisibility(VISIBLE);
                playPause.setImageDrawable(playDrawable);
                stopSeekbarUpdate();
                break;

            case PlaybackStateCompat.STATE_NONE:

            case PlaybackStateCompat.STATE_STOPPED:
                loading.setVisibility(INVISIBLE);
                playPause.setVisibility(VISIBLE);
                playPause.setImageDrawable(playDrawable);
                stopSeekbarUpdate();
                break;

            case PlaybackStateCompat.STATE_BUFFERING:
                playPause.setVisibility(INVISIBLE);
                loading.setVisibility(VISIBLE);
                extra.setText(R.string.state_loading);
                stopSeekbarUpdate();
                break;

            default:
                Log.d(TAG, "Unhandled state: " + state.getState());
        }

        skipNext.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) == 0
                ? INVISIBLE : VISIBLE );
        skipPrev.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) == 0
                ? INVISIBLE : VISIBLE );
    }

    private void updateProgress() {
        if (lastPlaybackState == null) {
            return;
        }

        long currentPosition = lastPlaybackState.getPosition();

        if (lastPlaybackState.getState() != PlaybackStateCompat.STATE_PAUSED) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensures that we do not repeatedly call the getPlaybackState()
            // on MediaControllerCompat.
            long timeDelta = SystemClock.elapsedRealtime() - lastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * lastPlaybackState.getPlaybackSpeed();
        }

        seekbar.setProgress((int) currentPosition);
    }
}
