package com.sms.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.TextHttpResponseHandler;
import com.sms.android.R;
import com.sms.lib.android.domain.MediaElement;
import com.sms.lib.android.domain.SMSPlayer;
import com.sms.lib.android.service.RESTService;

import org.apache.http.Header;

import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;

/**
 * Created by scott2ware on 19/07/15.
 */
public class VideoPlayerActivity extends Activity implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, SurfaceHolder.Callback {

    private static final String TAG = "VideoPlayerActivity";

    private static final int CONTROLLER_TIMEOUT = 5000;

    // REST Client
    RESTService restService = null;

    private SMSPlayer player;
    private MediaElement mediaElement;
    private Long jobID = null;
    private int startOffset = 0;

    // Override automatic controller visibility
    private Boolean showController = false;
    private Boolean initialised = false;
    private Boolean ready = false;
    private Boolean seekInProgress = false;

    private SurfaceView surface;
    private SurfaceHolder holder;
    private View controller = null;
    private long lastActionTime = 0L;
    private SeekBar timeline = null;
    private TextView duration, currentTime;
    private ImageButton playButton = null;
    private StringBuilder formatBuilder;
    private Formatter formatter;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
    }

    @Override
    protected void onPause() {
        super.onPause();

        surface.removeCallbacks(updateController);

        if(player != null) {
            if(player.isStreaming()) { startOffset = player.getCurrentPosition() + player.getOffset(); }
            else { startOffset = player.getCurrentPosition(); }

            player.stop();
            player.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        restService = RESTService.getInstance();

        surface = (SurfaceView)findViewById(R.id.video_surface);
        surface.setOnSystemUiVisibilityChangeListener(onSystemUiVisibilityChanged);

        holder = surface.getHolder();
        holder.addCallback(this);

        timeline = (SeekBar)findViewById(R.id.video_controller_timeline);
        timeline.setOnSeekBarChangeListener(onSeek);

        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());

        duration = (TextView) findViewById(R.id.video_controller_duration);
        duration.setText(stringForTime(0));

        currentTime = (TextView) findViewById(R.id.video_controller_current_time);
        currentTime.setText(stringForTime(startOffset));

        playButton = (ImageButton)findViewById(R.id.video_controller_play);
        playButton.setImageResource(R.drawable.ic_action_play_light);
        playButton.setOnClickListener(onPlayButtonPressed);

        controller = findViewById(R.id.video_controller);
        controller.setVisibility(View.VISIBLE);
        showController = true;

        // Hide action bar
        surface.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        // Retrieve media element to play
        Intent intent = getIntent();
        mediaElement = (MediaElement) intent.getSerializableExtra("mediaElement");

        // If our surface is ready start playing video
        if(ready) { setupPlayer(); }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(player != null) {
            if(jobID != null) { restService.endJob(jobID); }
            player.release();
            player = null;
        }
    }

    private void initialiseVideo() {

        if(mediaElement == null) {
            Toast error = Toast.makeText(getApplicationContext(), getString(R.string.video_player_error), Toast.LENGTH_SHORT);
            error.show();
        }

        // Initialise Stream
        restService.initialiseStream(mediaElement.getID(), new TextHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                jobID = Long.parseLong(responseString);
                setupPlayer();
                initialised = true;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Toast error = Toast.makeText(getApplicationContext(), getString(R.string.video_player_error), Toast.LENGTH_SHORT);
                error.show();
            }
        });
    }

    private void setupPlayer() {
        // Get settings
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        playButton.setEnabled(false);

        // Initialise player
        player = new SMSPlayer();
        player.setDisplay(holder);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setScreenOnWhilePlaying(true);

        // Configure Player
        player.setID(mediaElement.getID());
        player.setDuration(mediaElement.getDuration());
        player.setQuality(settings.getString("pref_video_quality", "360"));
        player.setOffset(startOffset);
        player.setStreaming(true);

        String streamUrl = restService.getBaseUrl() + "/stream/video" + "?id=" + jobID + "&client=android" + "&quality=" + player.getQuality() + "&offset=" + (int) (startOffset * 0.001);

        try {
            player.setDataSource(streamUrl);
            player.prepareAsync();
        }
        catch(Exception e){ player.release(); player = null; }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // End activity
        finish();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

        // Set timeline
        timeline.setProgress(player.getOffset());
        currentTime.setText(stringForTime(player.getOffset()));

        if(player.isStreaming()) {
            timeline.setMax(player.getStreamDuration() * 1000);
            duration.setText(stringForTime(player.getStreamDuration() * 1000));
        }
        else {
            timeline.setMax(player.getDuration());
            duration.setText(stringForTime(player.getDuration()));
        }

        player.start();

        playButton.setEnabled(true);
        playButton.setImageResource(R.drawable.ic_action_pause_light);

        showController = false;
        surface.postDelayed(updateController, 1000);
        lastActionTime = SystemClock.elapsedRealtime();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(initialised) { setupPlayer(); }
        else { initialiseVideo(); }

        // Surface is ready
        ready = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface is not ready
        ready = false;
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

    private void seekTo(int position) {
        if(player != null) {
            if(player.isStreaming()) {
                try {
                    player.setOffset(position);
                    player.reset();
                    String streamUrl = restService.getBaseUrl() + "/stream/video" + "?id=" + jobID + "&client=android" + "&quality=" + player.getQuality() + "&offset=" + (int) (player.getOffset() * 0.001);
                    player.setDataSource(streamUrl);
                    player.prepare();
                    player.start();

                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            } else { player.seekTo(position); }
        }
    }

    private Runnable updateController = new Runnable() {
        public void run() {
            if (!showController && (lastActionTime > 0) && ((SystemClock.elapsedRealtime() - lastActionTime) > CONTROLLER_TIMEOUT)) {
                lastActionTime = 0;
                surface.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }

            if (player != null && !seekInProgress) {
                if(player.isStreaming()) {
                    timeline.setProgress(player.getCurrentPosition() + player.getOffset());
                    currentTime.setText(stringForTime(player.getCurrentPosition() + player.getOffset()));
                }
                else {
                    timeline.setProgress(player.getCurrentPosition());
                    currentTime.setText(stringForTime(player.getCurrentPosition()));
                }
            }

            surface.postDelayed(updateController, 1000);
        }
    };

    private View.OnClickListener onPlayButtonPressed = new View.OnClickListener() {
        public void onClick(View v) {
            lastActionTime = SystemClock.elapsedRealtime();

            if (player != null) {
                if (player.isPlaying()) {
                    playButton.setImageResource(R.drawable.ic_action_play_light);
                    player.pause();
                    showController = true;
                    surface.removeCallbacks(updateController);
                }
                else {
                    playButton.setImageResource(R.drawable.ic_action_pause_light);
                    player.start();
                    showController = false;
                    surface.postDelayed(updateController, 1000);
                }
            }
        }
    };

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        formatBuilder.setLength(0);
        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }
}
