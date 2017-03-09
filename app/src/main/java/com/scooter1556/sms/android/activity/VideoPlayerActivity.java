package com.scooter1556.sms.android.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
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

import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.playback.CastPlayback;
import com.scooter1556.sms.android.playback.LocalPlayback;
import com.scooter1556.sms.android.playback.Playback;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.InterfaceUtils;

import java.util.Timer;
import java.util.TimerTask;

import static com.scooter1556.sms.android.utils.MediaUtils.EXTRA_MEDIA_ITEM;

public class VideoPlayerActivity extends AppCompatActivity implements Playback.Callback {
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
    private MediaBrowserCompat.MediaItem currentMedia;
    private boolean controllersVisible;
    private int duration;

    private Playback playback;

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

        setupCastListener();
        castContext = CastContext.getSharedInstance(this);
        castSession = castContext.getSessionManager().getCurrentCastSession();

        if (castSession != null && castSession.isConnected()) {
            playback = new CastPlayback(this);
        } else {
            playback = new LocalPlayback(this);
        }

        playback.setCallback(this);

        playCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playback.play(currentMedia.getMediaId(), true);
            }
        });

        setupControlsCallbacks();

        if (titleView != null) {
            updateMetadata(true);
        }
    }

    private void setupCastListener() {
        sessionManagerListener = new SessionManagerListener<CastSession>() {

            @Override
            public void onSessionEnded(CastSession session, int error) {
                Log.d(TAG, "onSessionEnded()");
                // Setup new playback
                switchToPlayback(new LocalPlayback(getApplicationContext()), true);

                updatePlayButton();
                invalidateOptionsMenu();
            }

            @Override
            public void onSessionResumed(CastSession session, boolean wasSuspended) {
                Log.d(TAG, "onSessionResumed()");
                castSession = castSession;

                if(!(playback instanceof CastPlayback)) {
                    Playback playback = new CastPlayback(getApplicationContext());
                    switchToPlayback(playback, !wasSuspended);
                }
            }

            @Override
            public void onSessionResumeFailed(CastSession session, int error) {
                Log.d(TAG, "onSessionResumeFailed()");
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarted(CastSession session, String sessionId) {
                Log.d(TAG, "onSessionStarted()");
                castSession = castSession;

                // Check device is capable of playing video
                if(castSession.getCastDevice().hasCapability(CastDevice.CAPABILITY_VIDEO_OUT)) {
                    // Setup new playback
                    switchToPlayback(new CastPlayback(getApplicationContext()), true);

                    updatePlayButton();
                    invalidateOptionsMenu();
                }
            }

            @Override
            public void onSessionStartFailed(CastSession session, int error) {
                Log.d(TAG, "onSessionStartFailed()");
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarting(CastSession session) {
                Log.d(TAG, "onSessionStarting()");
            }

            @Override
            public void onSessionEnding(CastSession session) {
                Log.d(TAG, "onSessionEnding()");
                playback.updateLastKnownStreamPosition();
            }

            @Override
            public void onSessionResuming(CastSession session, String sessionId) {
                Log.d(TAG, "onSessionResuming()");
            }

            @Override
            public void onSessionSuspended(CastSession session, int reason) {
                Log.d(TAG, "onSessionSuspended()");
            }

            private void onApplicationDisconnected() {
                updatePlayButton();
                invalidateOptionsMenu();
            }
        };
    }

    /**
     * Switch to a different Playback instance, maintaining all playback state, if possible.
     */
    public void switchToPlayback(Playback newPlayback, boolean resumePlaying) {
        // Suspend the current playback
        int oldState = playback.getState();
        long pos = playback.getCurrentStreamPosition();
        String currentMediaID = playback.getCurrentMediaId();

        playback.stop(false);

        Log.d(TAG, "switchToPlayback(" + newPlayback.getClass().toString() +
                " Resume: " + resumePlaying +
                " State: " + oldState +
                " Position: " + pos +
                " Media ID: " + currentMediaID +
                ")");

        // End old job if needed
        if(playback.getCurrentJobId() != null) {
            RESTService.getInstance().endJob(playback.getCurrentJobId());
        }

        // End session if needed
        if(playback.getSessionId() != null) {
            RESTService.getInstance().endSession(playback.getSessionId());
        }

        // Setup new playback
        newPlayback.setCallback(this);
        newPlayback.setCurrentStreamPosition(pos < 0 ? 0 : pos);
        newPlayback.setCurrentMediaId(currentMediaID);
        newPlayback.start();

        // Finally swap the instance
        playback = newPlayback;


        switch (oldState) {

            case PlaybackStateCompat.STATE_BUFFERING:

            case PlaybackStateCompat.STATE_CONNECTING:

            case PlaybackStateCompat.STATE_PAUSED:
                playback.pause();
                break;

            case PlaybackStateCompat.STATE_PLAYING:
                if (resumePlaying && currentMedia != null) {
                    playback.play(currentMedia.getDescription().getMediaId(), true);
                } else if (!resumePlaying) {
                    playback.pause();
                } else {
                    playback.stop(true);
                }
                break;

            case PlaybackStateCompat.STATE_NONE:
                break;

            default:
                Log.d(TAG, "Default called. Old state is " + oldState);
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
    protected void onPause() {
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
        playback.pause();
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
        playback.stop(true);

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
    protected void onResume() {
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
                    int currentPos = (int) (playback.getCurrentStreamPosition() * 0.001);
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

                if (playback.isPlaying()) {
                    playback.seekTo(seekBar.getProgress() * 1000);
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

                if(playback.isPlaying()) {
                    playback.pause();
                } else {
                    playback.play(currentMedia.getMediaId(), true);
                }
            }
        });
    }

    @Override
    public void onCompletion() {
        Log.d(TAG, "onCompletion()");

        // Allow screen to turn off
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onPlaybackStatusChanged(int state) {
        String log = "onPlaybackStatusChanged(): ";

        switch (state) {
            case PlaybackStateCompat.STATE_BUFFERING:
                log += "BUFFERING";

                videoView.setPlayer(playback.getMediaPlayer());
                videoView.setUseController(false);
                videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);

                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                break;

            case PlaybackStateCompat.STATE_STOPPED:
                log += "STOPPED";

                stopTrickplayTimer();
                updatePlayButton();

                // Allow screen to turn off
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;

            case PlaybackStateCompat.STATE_NONE:
                log += "NONE";

                // Allow screen to turn off
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                break;

            case PlaybackStateCompat.STATE_PLAYING:
                log += "PLAYING";

                startControllersTimer();
                restartTrickplayTimer();
                updatePlayButton();
                duration = playback.getCurrentMediaElement().getDuration();
                endText.setText(DateUtils.formatElapsedTime(duration));
                seekbar.setMax(duration);

                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                break;

            default:
                log += "DEFAULT";
                break;
        }
    }

    @Override
    public void onError(String error) {
        Log.d(TAG, "onError(" + error + ")");

        updatePlayButton();
        InterfaceUtils.showErrorDialog(VideoPlayerActivity.this, error);

        // Allow screen to turn off
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void setCurrentMediaID(String mediaID) {

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

        switch (playback.getState()) {

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
