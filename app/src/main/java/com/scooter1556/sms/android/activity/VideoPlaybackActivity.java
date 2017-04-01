package com.scooter1556.sms.android.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.playback.AudioPlayback;
import com.scooter1556.sms.android.playback.CastPlayback;
import com.scooter1556.sms.android.playback.Playback;
import com.scooter1556.sms.android.playback.PlaybackManager;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.InterfaceUtils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.scooter1556.sms.android.utils.MediaUtils.EXTRA_MEDIA_ITEM;

public class VideoPlaybackActivity extends BaseActivity implements Playback, AudioManager.OnAudioFocusChangeListener, ExoPlayer.EventListener {
    private static final String TAG = "VideoPlayerActivity";

    private SimpleExoPlayerView videoView;
    private TextView titleView;
    private TextView subtitleView;
    private TextView descriptionView;
    private TextView startText;
    private TextView endText;
    private SeekBar seekbar;
    private ImageView playPause;
    private ImageButton playCircle;
    private ProgressBar loading;
    private View controllers;
    private View container;
    private ImageView coverArt;

    private Timer seekbarTimer;
    private Timer controllersTimer;
    private final Handler handler = new Handler();
    private final float aspectRatio = 72f / 128;
    private MediaSessionCompat.QueueItem currentMedia;
    private boolean controllersVisible;
    private int duration;

    private volatile UUID currentJobId;
    private UUID sessionId;
    private int playbackState;

    private Callback callback;
    private PlaybackManager playbackManager;

    private CastContext castContext;
    private CastSession castSession;
    private SessionManagerListener<CastSession> sessionManagerListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_player);

        videoView = (SimpleExoPlayerView) findViewById(R.id.videoView);
        titleView = (TextView) findViewById(R.id.titleText);
        descriptionView = (TextView) findViewById(R.id.descriptionText);
        descriptionView.setMovementMethod(new ScrollingMovementMethod());
        subtitleView = (TextView) findViewById(R.id.subtitleText);
        startText = (TextView) findViewById(R.id.startText);
        startText.setText(DateUtils.formatElapsedTime(0));
        endText = (TextView) findViewById(R.id.endText);
        seekbar = (SeekBar) findViewById(R.id.seekBar);
        playPause = (ImageView) findViewById(R.id.playPauseImageView);
        loading = (ProgressBar) findViewById(R.id.progressBar);
        controllers = findViewById(R.id.controllers);
        container = findViewById(R.id.container);
        coverArt = (ImageView) findViewById(R.id.coverArtView);
        playCircle = (ImageButton) findViewById(R.id.play_circle);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            currentMedia = getIntent().getParcelableExtra(EXTRA_MEDIA_ITEM);
        }

        setupActionBar();

        // Setup UI for orientation
        updateConfiguration(getResources().getConfiguration());

        castContext = CastContext.getSharedInstance(this);
        castSession = castContext.getSessionManager().getCurrentCastSession();

        // Set playback instance in our playback manager
        playbackManager = PlaybackManager.getInstance();
        playbackManager.setPlayback(this);

        // Set playback manager callback
        this.setCallback(playbackManager);

        playCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(currentMedia.getDescription().getMediaId(), true);
            }
        });

        setupControlsCallbacks();

        if (titleView != null) {
            updateMetadata(true);
        }
    }

    private void stopTrickplayTimer() {
        Log.d(TAG, "Stopped TrickPlay Timer");
        if (seekbarTimer != null) {
            seekbarTimer.cancel();
        }
    }

    private void restartTrickplayTimer() {
        stopTrickplayTimer();
        seekbarTimer = new Timer();
        seekbarTimer.scheduleAtFixedRate(new UpdateSeekbarTask(), 100, 1000);
        Log.d(TAG, "Restarted TrickPlay Timer");
    }

    private void stopControllersTimer() {
        if (controllersTimer != null) {
            controllersTimer.cancel();
        }
    }

    private void startControllersTimer() {
        if (controllersTimer != null) {
            controllersTimer.cancel();
        }

        controllersTimer = new Timer();
        controllersTimer.schedule(new HideControllersTask(), 5000);
    }

    // should be called from the main thread
    private void updateControllersVisibility(boolean show) {
        Log.d(TAG, "updateControllersVisibility(" + show + ")");

        if (show) {
            getSupportActionBar().show();
            controllers.setVisibility(View.VISIBLE);
        } else {
            if (!InterfaceUtils.isOrientationPortrait(this)) {
                getSupportActionBar().hide();
            }
            controllers.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() was called");

        if (seekbarTimer != null) {
            seekbarTimer.cancel();
            seekbarTimer = null;
        }
        if (controllersTimer != null) {
            controllersTimer.cancel();
        }
        // since we are playing locally, we need to stop the playback of
        // video (if user is not watching, pause it!)
        pause();
        updatePlayButton();

        castContext.getSessionManager().removeSessionManagerListener(
                sessionManagerListener, CastSession.class);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop() was called");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy() is called");
        stopControllersTimer();
        stopTrickplayTimer();

        stop(true);
        destroy();

        // Allow screen to turn off
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart was called");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume() was called");
        castContext.getSessionManager().addSessionManagerListener(sessionManagerListener, CastSession.class);

        if (castSession != null && castSession.isConnected()) {
        } else {
        }

        super.onResume();
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return castContext.onDispatchVolumeKeyEventBeforeJellyBean(event) || super.dispatchKeyEvent(event);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop(boolean notifyListeners) {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void setState(int state) {

    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public long getCurrentStreamPosition() {
        return 0;
    }

    @Override
    public void setCurrentStreamPosition(long pos) {

    }

    @Override
    public void updateLastKnownStreamPosition() {

    }

    @Override
    public void play(String mediaId, boolean update) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void seekTo(long position) {

    }

    @Override
    public void setCurrentMediaId(String mediaId) {

    }

    @Override
    public String getCurrentMediaId() {
        return null;
    }

    @Override
    public void setSessionId(UUID sessionId) {

    }

    @Override
    public UUID getSessionId() {
        return null;
    }

    @Override
    public void setCurrentJobId(UUID jobId) {

    }

    @Override
    public UUID getCurrentJobId() {
        return null;
    }

    @Override
    public MediaElement getCurrentMediaElement() {
        return null;
    }

    @Override
    public SimpleExoPlayer getMediaPlayer() {
        return null;
    }

    @Override
    public void setCallback(Callback callback) {

    }

    @Override
    public void onAudioFocusChange(int focusChange) {

    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int state) {
        switch(state) {
            case ExoPlayer.STATE_BUFFERING:
                Log.d(TAG, "onPlayerStateChanged(BUFFERING)");

                videoView.setPlayer(getMediaPlayer());
                videoView.setUseController(false);
                videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);

                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // Update state
                playbackState = PlaybackStateCompat.STATE_BUFFERING;
                break;

            case ExoPlayer.STATE_ENDED:
                Log.d(TAG, "onPlayerStateChanged(ENDED)");

                // End job if required
                if (currentJobId != null) {
                    Log.d(TAG, "Ending job with id " + currentJobId);
                    RESTService.getInstance().endJob(currentJobId);
                    currentJobId = null;
                }

                // The media player finished playing the current item, so we go ahead and start the next.
                if (callback != null) {
                    callback.onCompletion();
                }

                // Allow screen to turn off
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            case ExoPlayer.STATE_IDLE:
                Log.d(TAG, "onPlayerStateChanged(IDLE)");

                // Allow screen to turn off
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                break;

            case ExoPlayer.STATE_READY:
                Log.d(TAG, "onPlayerStateChanged(READY)");

                if (playWhenReady) {
                    startControllersTimer();
                    restartTrickplayTimer();
                    updatePlayButton();
                    duration = getCurrentMediaElement().getDuration();
                    endText.setText(DateUtils.formatElapsedTime(duration));
                    seekbar.setMax(duration);

                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    playbackState = PlaybackStateCompat.STATE_PLAYING;
                }

                break;

            default:
                break;
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        updatePlayButton();
        InterfaceUtils.showErrorDialog(VideoPlaybackActivity.this, error.toString());

        // Allow screen to turn off
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onPositionDiscontinuity() {

    }

    private class HideControllersTask extends TimerTask {

        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    updateControllersVisibility(false);
                    controllersVisible = false;
                }
            });

        }
    }

    private class UpdateSeekbarTask extends TimerTask {

        @Override
        public void run() {
            handler.post(new Runnable() {

                @Override
                public void run() {
                    int currentPos = (int) (getCurrentStreamPosition() * 0.001);
                    updateSeekbar(currentPos, duration);
                }
            });
        }
    }

    private void setupControlsCallbacks() {
        videoView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(TAG, "onTouch()");

                if (!controllersVisible) {
                    updateControllersVisibility(true);
                }

                startControllersTimer();
                return false;
            }
        });

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStopTrackingTouch()");

                if (isPlaying()) {
                    seekTo(seekBar.getProgress() * 1000);
                }

                startControllersTimer();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStartTrackingTouch()");

                stopTrickplayTimer();
                stopControllersTimer();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                startText.setText(DateUtils.formatElapsedTime(progress));
            }
        });

        playPause.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick(" + v.toString() + ")");

                if(isPlaying()) {
                    pause();
                } else {
                    play(currentMedia.getDescription().getMediaId(), true);
                }
            }
        });
    }

    private void updateSeekbar(int position, int duration) {
        seekbar.setProgress(position);
        seekbar.setMax(duration);
        startText.setText(DateUtils.formatElapsedTime(position));
        endText.setText(DateUtils.formatElapsedTime(duration));
    }

    private void updatePlayButton() {
        String log = "updatePlayButton(): ";

        boolean isConnected = (castSession !=null) && (castSession.isConnected() || castSession.isConnecting());
        controllers.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        playCircle.setVisibility(isConnected ? View.GONE : View.VISIBLE);

        switch (getState()) {

            case PlaybackStateCompat.STATE_PLAYING:
                log += "PLAYING";

                loading.setVisibility(View.INVISIBLE);
                playPause.setVisibility(View.VISIBLE);
                playPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_av_pause_dark));
                playCircle.setVisibility(isConnected ? View.VISIBLE : View.GONE);
                break;

            case PlaybackStateCompat.STATE_NONE:
                log += "NONE";

                playCircle.setVisibility(View.VISIBLE);
                controllers.setVisibility(View.GONE);
                coverArt.setVisibility(View.VISIBLE);
                videoView.setVisibility(View.INVISIBLE);

                break;

            case PlaybackStateCompat.STATE_PAUSED:
                log += "PAUSED";

                loading.setVisibility(View.INVISIBLE);
                playPause.setVisibility(View.VISIBLE);
                playPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_av_play_dark));
                playCircle.setVisibility(isConnected ? View.VISIBLE : View.GONE);

                break;

            case PlaybackStateCompat.STATE_BUFFERING:
                log += "BUFFERING";

                playPause.setVisibility(View.INVISIBLE);
                loading.setVisibility(View.VISIBLE);
                break;

            default:
                log += "DEFAULT";
                break;
        }

        Log.d(TAG, log);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.d(TAG, "onConfigurationChanged()");

        getSupportActionBar().show();
        updateConfiguration(newConfig);
    }

    private void updateConfiguration(Configuration config) {
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            updateMetadata(false);
            container.setBackgroundColor(getResources().getColor(R.color.black));
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            updateMetadata(true);
            container.setBackgroundColor(getResources().getColor(R.color.white));
        }
    }

    private void updateMetadata(boolean visible) {
        Point displaySize;

        if (!visible) {
            descriptionView.setVisibility(View.GONE);
            titleView.setVisibility(View.GONE);
            subtitleView.setVisibility(View.GONE);
            displaySize = InterfaceUtils.getDisplaySize(this);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(displaySize.x, displaySize.y + getSupportActionBar().getHeight());
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
            videoView.setLayoutParams(lp);
            videoView.invalidate();
        } else {
            descriptionView.setText(currentMedia.getDescription().getDescription());
            titleView.setText(currentMedia.getDescription().getTitle());
            subtitleView.setText(currentMedia.getDescription().getSubtitle());
            descriptionView.setVisibility(View.VISIBLE);
            titleView.setVisibility(View.VISIBLE);
            subtitleView.setVisibility(View.VISIBLE);
            displaySize = InterfaceUtils.getDisplaySize(this);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(displaySize.x, (int) (displaySize.x * aspectRatio));
            lp.addRule(RelativeLayout.BELOW, R.id.toolbar);
            videoView.setLayoutParams(lp);
            videoView.invalidate();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu_video_player, menu);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu,
                R.id.media_route_menu_item);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        if (item.getItemId() == android.R.id.home) {
            ActivityCompat.finishAfterTransition(this);
        }
        return true;
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(currentMedia.getDescription().getTitle());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
