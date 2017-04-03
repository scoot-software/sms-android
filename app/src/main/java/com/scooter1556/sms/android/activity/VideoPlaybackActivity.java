package com.scooter1556.sms.android.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
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

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.domain.TranscodeProfile;
import com.scooter1556.sms.android.playback.AudioPlayback;
import com.scooter1556.sms.android.playback.CastPlayback;
import com.scooter1556.sms.android.playback.Playback;
import com.scooter1556.sms.android.playback.PlaybackManager;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.InterfaceUtils;
import com.scooter1556.sms.android.utils.MediaUtils;

import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.scooter1556.sms.android.utils.MediaUtils.EXTRA_MEDIA_ITEM;

public class VideoPlaybackActivity extends AppCompatActivity implements Playback, ExoPlayer.EventListener {
    private static final String TAG = "VideoPlaybackActivity";

    private static final String CLIENT_ID = "android";
    public static final String USER_AGENT = "SMSAndroidPlayer";
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    static final String FORMAT = "hls";
    static final String SUPPORTED_FILES = "mkv,webm,mp4";
    static final String SUPPORTED_CODECS = "h264,vp8,aac,mp3,vorbis,ac3";
    static final String MCH_CODECS = "ac3";

    static final int MAX_SAMPLE_RATE = 48000;

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

    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;

    private volatile long currentPosition = 0;
    private volatile String currentMediaID;
    private MediaElement element;
    private volatile UUID currentJobId;
    private UUID sessionId;
    private int playbackState;

    private Callback callback;
    private PlaybackManager playbackManager;

    private CastContext castContext;
    private CastSession castSession;
    private SessionManagerListener<CastSession> sessionManagerListener;

    private SimpleExoPlayer mediaPlayer;

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

        this.playbackState = PlaybackStateCompat.STATE_NONE;

        // Create Wifi lock
        this.wifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "sms_lock");

        // Create Wake lock
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sms_lock");

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

        // Get session ID
        RESTService.getInstance().createSession(new TextHttpResponseHandler()  {

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String response, Throwable throwable) {
                Log.e(TAG, "Failed to initialise session");
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String response) {
                // Parse result
                sessionId = UUID.fromString(response);
                Log.d(TAG, "New session ID: " + sessionId);
            }
        });
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

        // Allow screen to turn off
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // End session if needed
        if(sessionId != null) {
            RESTService.getInstance().endSession(sessionId);
        }

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

        super.onResume();
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return castContext.onDispatchVolumeKeyEventBeforeJellyBean(event) || super.dispatchKeyEvent(event);
    }

    @Override
    public void start() {
        Log.d(TAG, "start()");
    }

    @Override
    public void stop(boolean notifyListeners) {
        Log.d(TAG, "stop()");

        playbackState = PlaybackStateCompat.STATE_STOPPED;

        if (notifyListeners && callback != null) {
            callback.onPlaybackStatusChanged(playbackState);
        }

        currentPosition = getCurrentStreamPosition();

        // Relax all resources
        relaxResources(true);
    }

    @Override
    public void destroy() {
        finish();
    }

    @Override
    public void setState(int state) {
        this.playbackState = state;
    }

    @Override
    public int getState() {
        return playbackState;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.getPlayWhenReady();
    }

    @Override
    public long getCurrentStreamPosition() {
        return mediaPlayer != null ?
                mediaPlayer.getCurrentPosition() : currentPosition;
    }

    @Override
    public void setCurrentStreamPosition(long pos) {
        this.currentPosition = pos;
    }

    @Override
    public void updateLastKnownStreamPosition() {
        if (mediaPlayer != null) {
            currentPosition = mediaPlayer.getCurrentPosition();
        }
    }

    @Override
    public void play(String mediaId, boolean update) {
        Log.d(TAG, "Play media item with id " + mediaId);

        boolean mediaHasChanged = !TextUtils.equals(mediaId, currentMediaID);

        if (mediaHasChanged) {
            currentPosition = 0;
            currentMediaID = mediaId;
        }

        if (playbackState == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged && mediaPlayer != null) {
            configMediaPlayerState();
        } else {
            playbackState = PlaybackStateCompat.STATE_STOPPED;
            relaxResources(false);
            loadMedia(mediaId, update);
        }
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause()");

        if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            if (mediaPlayer != null && mediaPlayer.getPlayWhenReady()) {
                mediaPlayer.setPlayWhenReady(false);
                currentPosition = mediaPlayer.getCurrentPosition();
            }

            // While paused, retain the Media Player but give up audio focus
            relaxResources(false);
        }

        playbackState = PlaybackStateCompat.STATE_PAUSED;

        if (callback != null) {
            callback.onPlaybackStatusChanged(playbackState);
        }
    }

    @Override
    public void seekTo(long position) {
        Log.d(TAG, "seek(" + position + ")");

        if (mediaPlayer == null) {
            // If we do not have a current media player, simply update the current position
            currentPosition = position;
        } else {
            if (mediaPlayer.getPlayWhenReady()) {
                playbackState = PlaybackStateCompat.STATE_BUFFERING;
            }

            mediaPlayer.seekTo(position);

            if (callback != null) {
                callback.onPlaybackStatusChanged(playbackState);
            }
        }
    }

    @Override
    public void setCurrentMediaId(String mediaID) {
        this.currentMediaID = mediaID;
    }

    @Override
    public String getCurrentMediaId() {
        return currentMediaID;
    }

    @Override
    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public UUID getSessionId() {
        return this.sessionId;
    }

    @Override
    public void setCurrentJobId(UUID jobId) {
        this.currentJobId = jobId;
    }

    @Override
    public UUID getCurrentJobId() {
        return this.currentJobId;
    }

    @Override
    public MediaElement getCurrentMediaElement() {
        return element;
    }

    @Override
    public SimpleExoPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        Log.d(TAG, "onLoadingChanged(" + isLoading + ")");

        if(!isLoading) {
            // The media player is done preparing. That means we can start playing if we have audio focus.
            configMediaPlayerState();
        }
    }

    private void configMediaPlayerState() {
        String state = "?";
        int oldState = playbackState;

        if (mediaPlayer != null && !mediaPlayer.getPlayWhenReady()) {
            mediaPlayer.setPlayWhenReady(true);

            if (currentPosition == mediaPlayer.getCurrentPosition()) {
                playbackState = PlaybackStateCompat.STATE_PLAYING;

                wakeLock.acquire();
                wifiLock.acquire();

                state = "Playing";
            } else {
                mediaPlayer.seekTo(currentPosition);
                playbackState = PlaybackStateCompat.STATE_BUFFERING;

                state = "Seeking to " + currentPosition;
            }
        }

        if (callback != null) {
            callback.onPlaybackStatusChanged(playbackState);
        }

        Log.d(TAG, "configMediaPlayerState() > " + state);
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

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the Media Player.

     */
    private void relaxResources(boolean releaseMediaPlayer) {
        Log.d(TAG, "relaxResources()");

        // Stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // End job if required
        if(releaseMediaPlayer && currentJobId != null) {
            Log.d(TAG, "Ending job with id " + currentJobId);
            RESTService.getInstance().endJob(currentJobId);
            currentJobId = null;
        }

        // We can also release the Wifi and wake lock, if we're holding it
        if (wifiLock.isHeld()) {
            wifiLock.release();
        }

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void loadMedia(final String parentID, final boolean update) {
        Log.d(TAG, "loadMedia(" + parentID + ")");

        // Get Media Element ID from Media ID
        List<String> mediaID = MediaUtils.parseMediaId(parentID);

        if(mediaID.size() <= 1) {
            error("Error initialising stream", null);
            return;
        }

        final long id = Long.parseLong(mediaID.get(1));

        RESTService.getInstance().getMediaElement(getApplicationContext(), id, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();

                element = parser.fromJson(response.toString(), MediaElement.class);

                if(element != null) {
                    initialiseStream(update);
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                error("Exception loading media", null);
            }
        });
    }

    private void initialiseStream(boolean update) {
        Log.d(TAG, "Initialising stream for media item with id " + element.getID());

        // Check session ID
        if(sessionId == null) {
            Log.d(TAG, "Session ID not set, unable to initialise stream!");
            return;
        }

        // Get settings
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // Get quality
        int quality = Integer.parseInt(settings.getString("pref_video_quality", "0"));

        // Initialise Stream
        RESTService.getInstance().initialiseStream(getApplicationContext(), sessionId, element.getID(), CLIENT_ID, SUPPORTED_FILES, SUPPORTED_CODECS, null, FORMAT, quality, MAX_SAMPLE_RATE, null, null, settings.getBoolean("pref_direct_play", false), update, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                try {
                    // Parse profile
                    Gson parser = new Gson();
                    TranscodeProfile profile = parser.fromJson(response.toString(), TranscodeProfile.class);

                    currentJobId = profile.getID();

                    // Build stream URL
                    String url = RESTService.getInstance().getConnection().getUrl() + "/stream/" + profile.getID();

                    createMediaPlayerIfRequired();

                    // Get stream
                    String userAgent = Util.getUserAgent(getApplicationContext(), USER_AGENT);
                    DataSource.Factory dataSource = new DefaultDataSourceFactory(getApplicationContext(), userAgent, BANDWIDTH_METER);
                    ExtractorsFactory extractor = new DefaultExtractorsFactory();

                    MediaSource sampleSource;

                    if(profile.getType() == TranscodeProfile.StreamType.ADAPTIVE) {
                        sampleSource = new HlsMediaSource(Uri.parse(url),dataSource, new Handler(), null);
                    } else {
                        sampleSource = new ExtractorMediaSource(Uri.parse(url), dataSource, extractor, null, null);
                    }

                    playbackState = PlaybackStateCompat.STATE_BUFFERING;

                    mediaPlayer.prepare(sampleSource);

                    if (callback != null) {
                        callback.onPlaybackStatusChanged(playbackState);
                    }
                } catch (Exception e) {
                    error("Error initialising stream", e);
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                error("Error initialising stream", null);
            }
        });
    }

    private void error(String message, Exception e) {
        Log.e(TAG, message, e);

        if (callback != null) {
            callback.onError(message);
        }
    }

    public void createMediaPlayerIfRequired() {
        Log.d(TAG, "createMediaPlayerIfRequired()");

        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        LoadControl loadControl = new DefaultLoadControl();

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Create player
        mediaPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
        mediaPlayer.addListener(this);
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
