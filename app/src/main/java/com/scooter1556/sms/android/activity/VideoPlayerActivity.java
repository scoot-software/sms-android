package com.scooter1556.sms.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import com.loopj.android.http.TextHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.lib.android.domain.MediaElement;
import com.scooter1556.sms.lib.android.player.SMSVideoPlayer;
import com.scooter1556.sms.lib.android.service.RESTService;

import java.util.Formatter;
import java.util.Locale;

/**
 * Video Playback Activity
 *
 * Created by scott2ware.
 */
public class VideoPlayerActivity extends Activity implements SurfaceHolder.Callback, SMSVideoPlayer.Listener {

    private static final String TAG = "VideoPlayerActivity";

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
        surface.getHolder().addCallback(this);
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
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(player != null) {
            if(player.getJobID() != null) {
                restService.endJob(player.getJobID());
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

        // Initialise Stream
        restService.initialiseStream(mediaElement.getID(), new TextHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                player.setJobID(Long.parseLong(responseString));
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
        // Get settings
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        // Configure Player
        player.setID(mediaElement.getID());
        player.setQuality(settings.getString("pref_video_quality", "360"));

        if(mediaElement.getDuration() != null) {
            player.setDuration(mediaElement.getDuration());
        }

        String streamUrl = restService.getConnection().getUrl() + "/stream/video" + "?id=" + player.getJobID() + "&client=android" + "&quality=" + player.getQuality() + "&offset=" + (int) (player.getOffset() * 0.001);
        Uri contentUri = Uri.parse(streamUrl);

        player.addListener(this);
        player.initialise(this, contentUri, holder.getSurface(), !paused);
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
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
            }
        }
    }

    private void seekTo(int position) {
        if(player != null) {
            // Generate new stream uri
            String streamUrl = restService.getConnection().getUrl() + "/stream/video" + "?id=" + player.getJobID() + "&client=android" + "&quality=" + player.getQuality() + "&offset=" + (int) (position * 0.001);
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
