package com.scooter1556.sms.android.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

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
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.domain.TranscodeProfile;
import com.scooter1556.sms.android.service.RESTService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cz.msebera.android.httpclient.Header;

import static com.google.android.exoplayer2.C.CONTENT_TYPE_MUSIC;
import static com.google.android.exoplayer2.C.USAGE_MEDIA;

/**
 * A class that implements local media playback
 */
public class AudioPlayback implements Playback, AudioManager.OnAudioFocusChangeListener, ExoPlayer.EventListener {

    private static final String TAG = "AudioPlayback";

    private static final String CLIENT_ID = "android";
    public static final String USER_AGENT = "SMSAndroidPlayer";
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    static final String FORMAT = "hls";
    static final String SUPPORTED_FILES = "mp3,m4a,mp4,oga,ogg,wav";
    static final String SUPPORTED_CODECS = "aac,mp3,vorbis,pcm";
    static final String MCH_CODECS = "ac3";

    static final int MAX_SAMPLE_RATE = 48000;

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
    private volatile UUID currentJobId;
    private UUID sessionId;

    private int audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private final AudioManager audioManager;

    private SimpleExoPlayer mediaPlayer;

    private final IntentFilter audioNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private final BroadcastReceiver audioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                Log.d(TAG, "Headphones disconnected.");

                if (isPlaying()) {
                    Intent i = new Intent(context, MediaService.class);
                    i.setAction(MediaService.ACTION_CMD);
                    i.putExtra(MediaService.CMD_NAME, MediaService.CMD_PAUSE);
                    context.startService(i);
                }
            }
        }
    };

    public AudioPlayback(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.playbackState = PlaybackStateCompat.STATE_NONE;

        // Create Wifi lock
        this.wifiLock = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "sms_lock");

        // Create Wake lock
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sms_lock");
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

        // Give up Audio focus
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();

        // Relax all resources
        relaxResources(true);
    }

    @Override
    public void destroy() {
        // End session if needed
        if(sessionId != null) {
            RESTService.getInstance().endSession(sessionId);
        }
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
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();

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

            if(sessionId == null) {
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

                        initialiseStream();
                    }
                });
            } else {
                initialiseStream();
            }
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

        unregisterAudioNoisyReceiver();
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

        final UUID id = UUID.fromString(mediaID.get(1));

        Log.d(TAG, "Initialising stream for media item with id " + id);

        // Get settings
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        // Get quality
        int quality = Integer.parseInt(settings.getString("pref_audio_quality", "0"));

        // Initialise Stream
        RESTService.getInstance().initialiseStream(context, sessionId, id, CLIENT_ID, SUPPORTED_FILES, SUPPORTED_CODECS, null, FORMAT, quality, MAX_SAMPLE_RATE, null, null, settings.getBoolean("pref_direct_play", false), new JsonHttpResponseHandler() {
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
                    String userAgent = Util.getUserAgent(context, USER_AGENT);
                    DataSource.Factory dataSource = new DefaultDataSourceFactory(context, userAgent, BANDWIDTH_METER);
                    ExtractorsFactory extractor = new DefaultExtractorsFactory();

                    MediaSource sampleSource;
                    if(profile.getType() > TranscodeProfile.StreamType.DIRECT) {
                        sampleSource =
                                new HlsMediaSource.Factory(dataSource)
                                        .createMediaSource(Uri.parse(url), new Handler(), null);
                    } else {
                        sampleSource =
                                new ExtractorMediaSource.Factory(dataSource)
                                        .setExtractorsFactory(extractor)
                                        .createMediaSource(Uri.parse(url));
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

    /**
     * Try to get the system audio focus.
     */
    private void tryToGetAudioFocus() {
        Log.d(TAG, "tryToGetAudioFocus()");

        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AUDIO_FOCUSED;
            } else {
                audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    /**
     * Give up the audio focus.
     */
    private void giveUpAudioFocus() {
        Log.d(TAG, "giveUpAudioFocus()");

        if (audioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    /**
     * Reconfigures Media Player according to audio focus settings and
     * starts/restarts it. This method starts/restarts the Media Player
     * respecting the current audio focus state. So if we have focus, it will
     * play normally. If we don't have focus, it will either leave the
     * Media Player paused or set it to a low volume, depending on what is
     * allowed by the current focus settings.
     */
    private void configMediaPlayerState() {
        String state = "?";
        int oldState = playbackState;

        if (audioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause
            if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
                state = "Paused";
                pause();
            }
        } else {
            // We have audio focus
            if (audioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(VOLUME_DUCK);
                }
            } else {
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(VOLUME_NORMAL);
                }
            }

            // If we were playing when we lost focus, we need to resume playing.
            if (playOnFocusGain) {
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

                playOnFocusGain = false;
            }
        }

        if (callback != null) {
            callback.onPlaybackStatusChanged(playbackState);
        }

        Log.d(TAG, "configMediaPlayerState() > " + state);
    }

    /**
     * Called by AudioManager on audio focus changes.
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "onAudioFocusChange(" + focusChange + ")");

        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // Focus gained
            audioFocus = AUDIO_FOCUSED;
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                   focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                   focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // Focus lost
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            audioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

            // If we are playing, we need to reset the Media Player
            if (playbackState == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                playOnFocusGain = true;
            }
        } else {
            Log.e(TAG, "Ignoring unsupported focus change: " + focusChange);
        }

        configMediaPlayerState();
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
                if(currentJobId != null) {
                    Log.d(TAG, "Ending job with id " + currentJobId);
                    RESTService.getInstance().endJob(currentJobId);
                    currentJobId = null;
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

                if(playWhenReady) {
                    playbackState = PlaybackStateCompat.STATE_PLAYING;
                }

                // The media player is done preparing. That means we can start playing if we have audio focus.
                configMediaPlayerState();

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
    public void onTimelineChanged(Timeline timeline, Object manifest) {

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
        if(currentJobId != null) {
            Log.d(TAG, "Ending job with id " + currentJobId);
            RESTService.getInstance().endJob(currentJobId);
            currentJobId = null;
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

        // Set audio attributes
        final AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(CONTENT_TYPE_MUSIC)
                .setUsage(USAGE_MEDIA)
                .build();

        mediaPlayer.setAudioAttributes(audioAttributes);
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

    private void registerAudioNoisyReceiver() {
        Log.d(TAG, "registerAudioNoisyReceiver()");

        if (!audioNoisyReceiverRegistered) {
            context.registerReceiver(audioNoisyReceiver, audioNoisyIntentFilter);
            audioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        Log.d(TAG, "unregisterAudioNoisyReceiver()");

        if (audioNoisyReceiverRegistered) {
            context.unregisterReceiver(audioNoisyReceiver);
            audioNoisyReceiverRegistered = false;
        }
    }
}