package com.scooter1556.sms.android.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
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
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.domain.TranscodeProfile;
import com.scooter1556.sms.android.playback.Playback;
import com.scooter1556.sms.android.playback.PlaybackManager;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.InterfaceUtils;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.utils.TrackSelectionUtils;

import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

import cz.msebera.android.httpclient.Header;

import static com.scooter1556.sms.android.utils.MediaUtils.EXTRA_MEDIA_ITEM;

public class VideoPlaybackActivity extends AppCompatActivity implements View.OnClickListener, Playback, ExoPlayer.EventListener, PlaybackControlView.VisibilityListener {
    private static final String TAG = "VideoPlaybackActivity";

    private static final String CLIENT_ID = "android";
    public static final String USER_AGENT = "SMSAndroidPlayer";
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    static final String FORMAT = "hls";
    static final String SUPPORTED_FILES = "mkv,webm,mp4";
    static final String SUPPORTED_CODECS = "h264,vp8,aac,mp3,vorbis";
    static final String MCH_CODECS = "ac3";

    static final int MAX_SAMPLE_RATE = 48000;

    private SimpleExoPlayerView videoView;
    private LinearLayout settingsRootView;

    private MediaSessionCompat.QueueItem currentMedia;

    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;

    private volatile long currentPosition = 0;
    private volatile String currentMediaID;
    private volatile UUID currentJobId;
    private UUID sessionId;
    private int playbackState;

    private Callback callback;
    private PlaybackManager playbackManager;

    private SimpleExoPlayer mediaPlayer;
    private DefaultTrackSelector trackSelector;
    private TrackSelectionUtils trackSelectionUtils;
    private TrackGroupArray lastSeenTrackGroupArray;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_player);

        settingsRootView = (LinearLayout) findViewById(R.id.settingsRoot);

        videoView = (SimpleExoPlayerView) findViewById(R.id.videoView);
        videoView.setControllerVisibilityListener(this);
        videoView.requestFocus();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            currentMedia = getIntent().getParcelableExtra(EXTRA_MEDIA_ITEM);
        }

        this.playbackState = PlaybackStateCompat.STATE_NONE;

        // Create Wifi lock
        this.wifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "sms_lock");

        // Create Wake lock
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sms_lock");

        // Set playback instance in our playback manager
        playbackManager = PlaybackManager.getInstance();
        playbackManager.setPlayback(this);

        // Set playback manager callback
        this.setCallback(playbackManager);

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

                // Start Playback
                play(currentMedia.getDescription().getMediaId());
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() was called");

        pause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop() was called");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy() is called");

        // Stop playback
        stop(true);

        // Remove reference to playback in our playback manager
        playbackManager.setPlayback(null);

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
        // Show the controls on any key event.
        videoView.showController();

        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || videoView.dispatchMediaKeyEvent(event);
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
    public void play(String mediaId) {
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
            initialiseStream();
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
        updateButtonVisibilities();

        if (trackGroups != lastSeenTrackGroupArray) {
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
            if (mappedTrackInfo != null) {
                if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO) == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                    Toast.makeText(getApplicationContext(), R.string.error_unsupported_video, Toast.LENGTH_LONG).show();
                }
                if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO) == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                    Toast.makeText(getApplicationContext(), R.string.error_unsupported_audio, Toast.LENGTH_LONG).show();
                }
            }
            lastSeenTrackGroupArray = trackGroups;
        }
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
                    playbackState = PlaybackStateCompat.STATE_PLAYING;
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }

                break;

            default:
                break;
        }

        updateButtonVisibilities();
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        InterfaceUtils.showErrorDialog(VideoPlaybackActivity.this, error.toString());

        // Allow screen to turn off
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onPositionDiscontinuity() {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onVisibilityChange(int visibility) {
        Log.d(TAG, "onVisibilityChange(" + visibility + ")");

        if(visibility == View.GONE) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }

        settingsRootView.setVisibility(visibility);
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
            trackSelector = null;
            trackSelectionUtils = null;
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

    private void initialiseStream() {
        // Check session ID
        if(sessionId == null) {
            Log.d(TAG, "Session ID not set, unable to initialise stream!");
            return;
        }

        // Get Media Element ID from Media ID
        List<String> mediaID = MediaUtils.parseMediaId(currentMediaID);

        if(mediaID.size() <= 1) {
            error("Error initialising stream", null);
            return;
        }

        // Get media element ID
        final long id = Long.parseLong(mediaID.get(1));

        Log.d(TAG, "Initialising stream for media item with id " + id);

        // Get settings
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // Get quality
        int quality = Integer.parseInt(settings.getString("pref_video_quality", "0"));

        // Initialise Stream
        RESTService.getInstance().initialiseStream(getApplicationContext(), sessionId, id, CLIENT_ID, SUPPORTED_FILES, SUPPORTED_CODECS, null, FORMAT, quality, MAX_SAMPLE_RATE, null, null, settings.getBoolean("pref_direct_play", false), new JsonHttpResponseHandler() {
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

                    MediaSource sampleSource = new HlsMediaSource(Uri.parse(url),dataSource, new Handler(), null);

                    playbackState = PlaybackStateCompat.STATE_BUFFERING;

                    mediaPlayer.prepare(sampleSource);

                    updateButtonVisibilities();

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

            @Override
            public void onFailure(int statusCode, Header[] headers, String error, Throwable throwable) {
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

        TrackSelection.Factory adaptiveTrackSelectionFactory = new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
        trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
        trackSelectionUtils = new TrackSelectionUtils(trackSelector, adaptiveTrackSelectionFactory);
        lastSeenTrackGroupArray = null;

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
    public void onClick(View view) {
        if (view.getParent() == settingsRootView) {
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

            if (mappedTrackInfo != null) {
                trackSelectionUtils.showSelectionDialog(this, ((Button) view).getText(),
                        trackSelector.getCurrentMappedTrackInfo(), (int) view.getTag());
            }
        }
    }

    private void updateButtonVisibilities() {
        settingsRootView.removeAllViews();

        if (mediaPlayer == null) {
            return;
        }

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

        if (mappedTrackInfo == null) {
            return;
        }

        for (int i = 0; i < mappedTrackInfo.length; i++) {
            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);

            if (trackGroups.length != 0) {
                Button button = new Button(this);
                int label;
                switch (mediaPlayer.getRendererType(i)) {
                    case C.TRACK_TYPE_AUDIO:
                        label = R.string.audio;
                        break;
                    case C.TRACK_TYPE_VIDEO:
                        label = R.string.video;
                        break;
                    case C.TRACK_TYPE_TEXT:
                        label = R.string.text;
                        break;
                    default:
                        continue;
                }

                button.setText(label);
                button.setTag(i);
                button.setOnClickListener(this);
                settingsRootView.addView(button, settingsRootView.getChildCount() - 1);
            }
        }
    }

    private void showControls() {
        settingsRootView.setVisibility(View.VISIBLE);
    }
}
