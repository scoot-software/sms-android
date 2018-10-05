package com.scooter1556.sms.android.playback;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
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
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.loopj.android.http.TextHttpResponseHandler;
import com.scooter1556.sms.android.service.SessionService;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.service.RESTService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import cz.msebera.android.httpclient.Header;

import static com.google.android.exoplayer2.C.CONTENT_TYPE_MUSIC;
import static com.google.android.exoplayer2.C.USAGE_MEDIA;

/**
 * A class that implements local media playback
 */
public class AudioPlayback implements Playback, ExoPlayer.EventListener {

    private static final String TAG = "AudioPlayback";

    private static final String CLIENT_ID = "android";
    public static final String USER_AGENT = "SMSAndroidPlayer";
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;
    // The volume we set the media player when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    // No audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // No audio focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // Full audio focus
    private static final int AUDIO_FOCUSED  = 2;

    private final Context context;
    private final WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;
    private int playbackState;
    private boolean playOnFocusGain;
    private Callback callback;
    private volatile boolean audioNoisyReceiverRegistered;
    private volatile long currentPosition = 0;
    private volatile String currentMediaID;
    private UUID sessionId;

    private SimpleExoPlayer mediaPlayer;

    public AudioPlayback(Context context) {
        this.context = context;
        this.playbackState = PlaybackStateCompat.STATE_NONE;

        // Create Wifi lock
        this.wifiLock = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "sms_lock");

        // Create Wake lock
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.scooter1556.sms.android: audio_playback_wake_lock");
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
    public void destroy() {}

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
        return playOnFocusGain || (mediaPlayer != null && mediaPlayer.getPlayWhenReady());
    }

    @Override
    public long getCurrentStreamPosition() {
        return mediaPlayer != null ?
                mediaPlayer.getCurrentPosition() : currentPosition;
    }

    @Override
    public void updateLastKnownStreamPosition() {
        if (mediaPlayer != null) {
            currentPosition = mediaPlayer.getCurrentPosition();
        }
    }

    @Override
    public void play(MediaSessionCompat.QueueItem item) {
        Log.d(TAG, "Play media item with id " + item.getDescription().getMediaId());

        playOnFocusGain = true;

        boolean mediaHasChanged = !TextUtils.equals(item.getDescription().getMediaId(), currentMediaID);

        if (mediaHasChanged) {
            currentPosition = 0;
            currentMediaID = item.getDescription().getMediaId();
        }

        if (playbackState == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged && mediaPlayer != null) {
            configMediaPlayerState();
        } else {
            playbackState = PlaybackStateCompat.STATE_STOPPED;
            relaxResources(false);

            // Retrieve session ID
            sessionId = SessionService.getInstance().getSessionId();

            if(sessionId == null) {
                error("Failed to get session ID", null);
                return;
            }

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
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void setCurrentStreamPosition(long pos) {
        this.currentPosition = pos;
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
    public SimpleExoPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public UUID getSessionId() {
        return this.sessionId;
    }

    private void initialiseStream() {
        // Get Media Element ID from Media ID
        List<String> mediaID = MediaUtils.parseMediaId(currentMediaID);

        if(mediaID.size() <= 1) {
            error("Error initialising stream", null);
            return;
        }

        // Get media element ID
        final UUID id = UUID.fromString(mediaID.get(1));

        Log.d(TAG, "Initialising stream for media item with id " + id);

        // Build stream URL
        final String url = RESTService.getInstance().getConnection().getUrl() + "/stream/" + sessionId + "/" + id;

        RESTService.getInstance().getStream(context, sessionId, id, new TextHttpResponseHandler() {

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                error("Failed to initialise stream", null);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                Log.d(TAG, Arrays.toString(headers));

                String type = null;

                createMediaPlayerIfRequired();

                // Get stream
                String userAgent = Util.getUserAgent(context, USER_AGENT);
                DataSource.Factory dataSource = new DefaultDataSourceFactory(context, userAgent, BANDWIDTH_METER);
                ExtractorsFactory extractor = new DefaultExtractorsFactory();
                MediaSource sampleSource;

                for(Header header : headers) {
                    if(header.getName().equals("Content-Type")) {
                        type = header.getValue().split(";")[0];
                        break;
                    }
                }

                if(type == null) {
                    error("Failed to initialise stream", null);
                    return;
                }

                switch (type) {
                    case "application/x-mpegurl":
                        sampleSource = new HlsMediaSource.Factory(dataSource)
                                .createMediaSource(Uri.parse(url));
                        break;
                    default: {
                        sampleSource = new ExtractorMediaSource.Factory(dataSource)
                                .setExtractorsFactory(extractor)
                                .createMediaSource(Uri.parse(url));
                    }
                }

                playbackState = PlaybackStateCompat.STATE_BUFFERING;
                mediaPlayer.prepare(sampleSource);

                if (callback != null) {
                    callback.onPlaybackStatusChanged(playbackState);
                }
            }
        });
    }

    private void error(String message, Exception e) {
        Log.e(TAG, message, e);

        if (callback != null) {
            callback.onError(message);
        }
    }

    private void configMediaPlayerState() {
        String state = "?";

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

    //
    // Player Listener
    //

    @Override
    public void onLoadingChanged(boolean isLoading) {
        Log.d(TAG, "onLoadingChanged(" + isLoading + ")");

        if(!isLoading) {
            // The media player is done preparing. That means we can start playing if we have audio focus.
            configMediaPlayerState();
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int state) {
        switch(state) {
            case ExoPlayer.STATE_BUFFERING:
                Log.d(TAG, "onPlayerStateChanged(BUFFERING)");

                // Update state
                playbackState = PlaybackStateCompat.STATE_BUFFERING;
                break;

            case ExoPlayer.STATE_ENDED:
                Log.d(TAG, "onPlayerStateChanged(ENDED)");

                // End job if required
                if(sessionId != null && !currentMediaID.isEmpty()) {
                    Log.d(TAG, "Ending job for media with id: " + currentMediaID);
                    RESTService.getInstance().endJob(sessionId, UUID.fromString(MediaUtils.parseMediaId(currentMediaID).get(1)));
                }

                // The media player finished playing the current item, so we go ahead and start the next.
                if (callback != null) {
                    callback.onCompletion();
                }

            case ExoPlayer.STATE_IDLE:
                Log.d(TAG, "onPlayerStateChanged(IDLE)");

                break;

            case ExoPlayer.STATE_READY:
                Log.d(TAG, "onPlayerStateChanged(READY)");

                if(mediaPlayer.getPlayWhenReady()) {
                    playbackState = PlaybackStateCompat.STATE_PLAYING;
                    configMediaPlayerState();
                }

                break;

            default:
                break;
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    @Override
    public void onPlayerError(ExoPlaybackException exception) {
        Log.e(TAG, "Media player error", exception);

        // End job if required
        if(sessionId != null && !currentMediaID.isEmpty()) {
            Log.d(TAG, "Ending job for media with id: " + currentMediaID);
            RESTService.getInstance().endJob(sessionId, UUID.fromString(MediaUtils.parseMediaId(currentMediaID).get(1)));
        }

        if (callback != null) {
            callback.onError(exception.getMessage());
        }
    }

    @Override
    public void onPositionDiscontinuity(int reason) {

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
        mediaPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl);
        mediaPlayer.addListener(this);

        // Set audio attributes so audio focus can be handled correctly
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build();

        mediaPlayer.setAudioAttributes(audioAttributes, true);
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

        // We can also release the Wifi and wake lock, if we're holding it
        if (wifiLock.isHeld()) {
            wifiLock.release();
        }

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}