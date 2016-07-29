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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer.ExoPlayer;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.lib.android.domain.MediaElement;
import com.scooter1556.sms.lib.android.domain.TranscodeProfile;
import com.scooter1556.sms.lib.android.player.SMSVideoPlayer;
import com.scooter1556.sms.lib.android.service.RESTService;

import org.json.JSONObject;

import java.util.Formatter;
import java.util.Locale;

public class VideoPlayerActivity extends Activity implements SurfaceHolder.Callback, SMSVideoPlayer.Listener {

    private static final String TAG = "VideoPlayerActivity";

    static final String SUPPORTED_CODECS = "h264,vp8,aac,mp3,vorbis";
    static final String FORMAT = "matroska";

    static final int MAX_SAMPLE_RATE = 48000;
    static final int MAX_MOBILE_QUALITY = 2;

    private static final int CONTROLLER_TIMEOUT = 5000;

    // REST Client
    RESTService restService = null;

    // UI
    private SurfaceView surface;
    private SurfaceHolder holder;
    private View controller = null;
    private long lastActionTime = 0L;
    private SeekBar timeline = null;
    private TextView duration, currentTime;
    private ImageButton playButton = null;
    private StringBuilder formatBuilder;
    private Formatter formatter;
    private ImageView videoOverlay;

    // Player
    private SMSVideoPlayer player;
    private MediaElement mediaElement;

    // Locks
    WifiManager.WifiLock wifiLock;

    // Override automatic controller visibility
    private Boolean showController = false;
    private Boolean initialised = false;
    private Boolean ready = false;
    private Boolean seekInProgress = false;
    private Boolean paused = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_player);

        restService = RESTService.getInstance();

        player = new SMSVideoPlayer();

        surface = (SurfaceView) findViewById(R.id.video_surface);
        surface.setOnSystemUiVisibilityChangeListener(onSystemUiVisibilityChanged);
        surface.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                togglePlayback();
                return false;
            }
        });

        holder = surface.getHolder();
        holder.addCallback(this);

        videoOverlay = (ImageView) findViewById(R.id.video_overlay);
        videoOverlay.setImageResource(R.drawable.play_overlay);

        timeline = (SeekBar)findViewById(R.id.video_controller_timeline);
        timeline.setOnSeekBarChangeListener(onSeek);

        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());

        duration = (TextView) findViewById(R.id.video_controller_duration);
        duration.setText(stringForTime(0));

        currentTime = (TextView) findViewById(R.id.video_controller_current_time);
        currentTime.setText(stringForTime(0));

        playButton = (ImageButton)findViewById(R.id.video_controller_play);
        playButton.setImageResource(R.drawable.ic_play_light);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePlayback();
            }
        });

        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "sms");

        controller = findViewById(R.id.video_controller);
    }

    @Override
    public void onResume() {
        super.onResume();

        controller.setVisibility(View.VISIBLE);
        showController = true;

        // Hide action bar
        surface.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        // Retrieve media element to play
        Intent intent = getIntent();
        mediaElement = (MediaElement) intent.getSerializableExtra("mediaElement");

        // If our surface is ready start playing video (surface is not destroyed if the power button is pressed)
        if(ready) {
            preparePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        surface.removeCallbacks(updateController);
        videoOverlay.setVisibility(View.VISIBLE);
        paused = true;

        if(player != null) {
            player.setOffset(player.getCurrentPosition());
            player.removeListener(this);
            player.release();

            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(player != null) {
            if(player.getTranscodeProfile() != null) {
                restService.endJob(player.getTranscodeProfile().getID());
            }

            releasePlayer();
        }
    }

    private SeekBar.OnSeekBarChangeListener onSeek = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int position, boolean fromUser) {
            if (fromUser) {
                currentTime.setText(stringForTime(position));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            seekInProgress = true;
            showController = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            lastActionTime = SystemClock.elapsedRealtime();
            seekInProgress = false;
            showController = false;
            int position = seekBar.getProgress();
            seekTo(position);
        }
    };

    private void initialiseVideo() {

        if(mediaElement == null) {
            Toast error = Toast.makeText(getApplicationContext(), getString(R.string.video_player_error), Toast.LENGTH_SHORT);
            error.show();
        }

        // Get settings
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        // Initialise Stream
        restService.initialiseStream(getApplicationContext(), mediaElement.getID(), null, SUPPORTED_CODECS, null, FORMAT, Integer.parseInt(settings.getString("pref_video_quality", "0")), MAX_SAMPLE_RATE, null, null, settings.getBoolean("pref_direct_play", false), new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                // Parse profile
                Gson parser = new Gson();
                TranscodeProfile profile = parser.fromJson(response.toString(), TranscodeProfile.class);

                // Configure Player
                player.setTranscodeProfile(profile);
                player.setID(mediaElement.getID());
                player.setQuality(settings.getString("pref_video_quality", "0"));

                if(mediaElement.getDuration() != null) {
                    player.setDuration(mediaElement.getDuration());
                }

                preparePlayer();
                initialised = true;
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                Toast error = Toast.makeText(getApplicationContext(), getString(R.string.video_player_error), Toast.LENGTH_SHORT);
                error.show();
            }
        });
    }

    private void preparePlayer() {
        String streamUrl = restService.getConnection().getUrl() + "/stream/" + player.getTranscodeProfile().getID() + "?offset=" + (int) (player.getOffset() * 0.001);
        Uri contentUri = Uri.parse(streamUrl);

        player.addListener(this);
        player.initialise(this, contentUri, holder.getSurface(), !paused);

        wifiLock.acquire();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;

            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
        }
    }

    private void togglePlayback() {
        lastActionTime = SystemClock.elapsedRealtime();

        if (player != null) {
            if (player.isPlaying()) {
                playButton.setImageResource(R.drawable.ic_play_light);
                videoOverlay.setVisibility(View.VISIBLE);
                showController = true;
                surface.removeCallbacks(updateController);

                player.pause();
                paused = true;

                // Allow screen to turn off
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                if (wifiLock.isHeld()) {
                    wifiLock.release();
                }
            }
            else {
                playButton.setImageResource(R.drawable.ic_pause_light);
                videoOverlay.setVisibility(View.INVISIBLE);
                player.start();
                paused = false;
                showController = false;
                surface.postDelayed(updateController, 1000);

                // Keep screen on
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                wifiLock.acquire();
            }
        }
    }

    private void seekTo(int position) {
        if(player != null) {
            // Generate new stream uri
            String streamUrl = restService.getConnection().getUrl() + "/stream/" + player.getTranscodeProfile().getID() + "?offset=" + (int) (position * 0.001);
            Uri contentUri = Uri.parse(streamUrl);

            // Initialise player in new position
            player.release();
            player.setOffset(position);
            player.initialise(this, contentUri, holder.getSurface(), !paused);
        }
    }

    private View.OnSystemUiVisibilityChangeListener onSystemUiVisibilityChanged = new View.OnSystemUiVisibilityChangeListener() {
        @Override
        public void onSystemUiVisibilityChange(int visibility) {
            if(showController) { return; }

            if ((visibility & (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN)) == 0) {
                lastActionTime = SystemClock.elapsedRealtime();
                controller.setVisibility(View.VISIBLE);
            } else {
                controller.setVisibility(View.GONE);
            }
        }
    };

    private Runnable updateController = new Runnable() {
        public void run() {
            if (!showController && (lastActionTime > 0) && ((SystemClock.elapsedRealtime() - lastActionTime) > CONTROLLER_TIMEOUT)) {
                lastActionTime = 0;
                surface.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }

            if (player != null && !seekInProgress) {
                timeline.setProgress((int) player.getCurrentPosition());
                currentTime.setText(stringForTime(player.getCurrentPosition()));
            }

            surface.postDelayed(updateController, 1000);
        }
    };

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        switch(playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                break;
            case ExoPlayer.STATE_ENDED:
                releasePlayer();
                finish();
                break;
            case ExoPlayer.STATE_IDLE:
                break;
            case ExoPlayer.STATE_PREPARING:
                break;
            case ExoPlayer.STATE_READY:
                // Set timeline
                timeline.setProgress((int) player.getCurrentPosition());
                currentTime.setText(stringForTime(player.getCurrentPosition()));

                timeline.setMax((int) (player.getDuration() * 1000));
                duration.setText(stringForTime(player.getDuration() * 1000));

                if(player.isPlaying()) {
                    playButton.setImageResource(R.drawable.ic_pause_light);
                    videoOverlay.setVisibility(View.INVISIBLE);

                    // Keep screen on
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    showController = false;
                    surface.postDelayed(updateController, 1000);
                    lastActionTime = SystemClock.elapsedRealtime();
                } else {
                    playButton.setImageResource(R.drawable.ic_play_light);
                    videoOverlay.setVisibility(View.VISIBLE);
                }

                break;

            default:
                break;
        }
    }

    @Override
    public void onError(Exception e) {
        // Display error message
        Toast warning = Toast.makeText(this, getString(R.string.error_media_playback), Toast.LENGTH_SHORT);
        warning.show();

        // Release player and finish activity
        releasePlayer();
        finish();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(initialised) {
            paused = true;
            preparePlayer();
        } else {
            initialiseVideo();
        }

        // Surface is ready
        ready = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Do nothing.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (player != null) {
            ready = false;
            player.release();
        }
    }

    //
    // Helper Functions
    //

    private String stringForTime(long timeMs) {
        long totalSeconds = timeMs / 1000;

        int seconds = (int)totalSeconds % 60;
        int minutes = (int)(totalSeconds / 60) % 60;
        int hours   = (int)totalSeconds / 3600;

        formatBuilder.setLength(0);
        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }
}
