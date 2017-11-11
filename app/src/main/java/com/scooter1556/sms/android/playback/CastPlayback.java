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
package com.scooter1556.sms.android.playback;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.domain.TranscodeProfile;
import com.scooter1556.sms.android.service.RESTService;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

/**
 * An implementation of Playback that talks to Cast devices.
 */
public class CastPlayback implements Playback {

    private static final String TAG = "CastPlayback";

    private static final String CLIENT_ID = "chromecast";

    static final String FORMAT = "hls";
    static final String SUPPORTED_FILES = "mkv,webm,aac,m4a,mp3,oga,ogg,mp4";
    static final String SUPPORTED_CODECS = "h264,vp8,aac,mp3";
    static final String MCH_CODECS = "ac3";

    static final int MAX_SAMPLE_RATE = 48000;

    private static final String SERVER_URL = "serverUrl";
    private static final String MEDIA_ID = "mediaId";
    private static final String QUALITY = "quality";

    private final Context appContext;
    private final RemoteMediaClient remoteMediaClient;
    private final RemoteMediaClient.Listener remoteMediaClientListener;

    private int playbackState;

    private Callback callback;
    private volatile long currentPosition;
    private volatile String currentMediaId;
    private volatile UUID currentJobId;

    private boolean finished = false;

    public CastPlayback(Context context) {
        appContext = context.getApplicationContext();

        CastSession castSession = CastContext.getSharedInstance(appContext).getSessionManager().getCurrentCastSession();
        remoteMediaClient = castSession.getRemoteMediaClient();
        remoteMediaClientListener = new CastMediaClientListener();
    }

    @Override
    public void start() {
        Log.d(TAG, "start()");

        remoteMediaClient.addListener(remoteMediaClientListener);
    }

    @Override
    public void stop(boolean notifyListeners) {
        Log.d(TAG, "stop()");

        playbackState = PlaybackStateCompat.STATE_STOPPED;

        if (notifyListeners && callback != null) {
            callback.onPlaybackStatusChanged(playbackState);
        }

        currentJobId = null;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void setState(int state) {
        Log.d(TAG, "setState(" + state + ")");

        this.playbackState = state;
    }

    @Override
    public long getCurrentStreamPosition() {
        long pos;

        if (!isConnected()) {
            pos = currentPosition;
        } else {
            pos = remoteMediaClient.getApproximateStreamPosition();
        }

        Log.d(TAG, "getCurrentStreamPosition() < " + pos);

        return pos;
    }

    @Override
    public void setCurrentStreamPosition(long pos) {
        Log.d(TAG, "setCurrentStreamPosition(" + pos + ")");

        this.currentPosition = pos;
    }

    @Override
    public void updateLastKnownStreamPosition() {
        Log.d(TAG, "updateLastKnownStreamPosition()");

        currentPosition = getCurrentStreamPosition();
    }

    @Override
    public void play(MediaSessionCompat.QueueItem item) {
        Log.d(TAG, "play(" + item.getDescription().getMediaId() + ")");

        boolean mediaHasChanged = !TextUtils.equals(item.getDescription().getMediaId(), currentMediaId);

        if (mediaHasChanged) {
            currentMediaId = item.getDescription().getMediaId();
            currentPosition = 0;
        }

        if (playbackState == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged && remoteMediaClient.hasMediaSession()) {
            remoteMediaClient.play();
        } else {
            loadMedia(item, true);
            playbackState = PlaybackStateCompat.STATE_BUFFERING;
        }

        if (callback != null) {
            callback.onPlaybackStatusChanged(playbackState);
        }
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause()");

        if (remoteMediaClient.hasMediaSession()) {
            remoteMediaClient.pause();
            currentPosition = (int) remoteMediaClient.getApproximateStreamPosition();
        }
    }

    @Override
    public void seekTo(long position) {
        Log.d(TAG, "seekTo(" + position + ")");

        if (currentMediaId == null) {
            if (callback != null) {
                callback.onError("Cannot seek if media ID is unknown.");
            }

            return;
        }

        if (remoteMediaClient.hasMediaSession()) {
            remoteMediaClient.seek(position);
            currentPosition = position;
        }
    }

    @Override
    public void setCurrentMediaId(String mediaId) {
        Log.d(TAG, "setCurrentMediaId(" + mediaId + ")");

        this.currentMediaId = mediaId;
    }

    @Override
    public String getCurrentMediaId() {
        Log.d(TAG, "getCurrentMediaId()");

        return currentMediaId;
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
        Log.d(TAG, "setCurrentJobId(" + jobId + ")");

        this.currentJobId = jobId;
    }

    @Override
    public UUID getCurrentJobId() {
        Log.d(TAG, "getCurrentJobId()");

        return currentJobId;
    }

    @Override
    public SimpleExoPlayer getMediaPlayer() {
        return null;
    }

    @Override
    public void setCallback(Callback callback) {
        Log.d(TAG, "setCallback()");

        this.callback = callback;
    }

    @Override
    public boolean isConnected() {
        CastSession castSession = CastContext.getSharedInstance(appContext).getSessionManager().getCurrentCastSession();
        boolean connected = (castSession != null && castSession.isConnected());

        Log.d(TAG, "isConnected() < " + connected);

        return connected;
    }

    @Override
    public boolean isPlaying() {
        boolean playing = isConnected() && remoteMediaClient.isPlaying();

        Log.d(TAG, "isPlaying() < " + playing);

        return playing;
    }

    @Override
    public int getState() {
        Log.d(TAG, "getState() < " + playbackState);

        return playbackState;
    }

    private void loadMedia(final MediaSessionCompat.QueueItem item, final boolean autoPlay) {
        Log.d(TAG, "loadMedia(" + item.getDescription().getMediaId() + ", " + autoPlay + ")");

        // Get Media Element ID from Media ID
        List<String> mediaID = MediaUtils.parseMediaId(currentMediaId);

        if(mediaID.size() <= 1) {
            error("Error initialising stream", null);
            return;
        }

        initialiseStream(item, autoPlay);
    }

    private void initialiseStream(final MediaSessionCompat.QueueItem item, final boolean autoPlay) {
        Log.d(TAG, "initialiseStream(" + item.getDescription().getMediaId() + ", " + autoPlay + ")");

        // Get Media Element ID from Media ID
        List<String> mediaID = MediaUtils.parseMediaId(currentMediaId);

        if(mediaID.size() <= 1) {
            error("Error initialising stream", null);
            return;
        }

        Long id = Long.parseLong(mediaID.get(1));

        // Get settings
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(appContext);

        // Get quality
        int quality = 0;

        if(MediaUtils.getMediaTypeFromID(currentMediaId) == MediaElement.MediaElementType.AUDIO) {
            quality = Integer.parseInt(settings.getString("pref_audio_quality", "0"));
        } else if(MediaUtils.getMediaTypeFromID(currentMediaId) == MediaElement.MediaElementType.VIDEO) {
            quality = Integer.parseInt(settings.getString("pref_video_quality", "0"));
        }

        // Set custom data
        JSONObject customData = new JSONObject();

        try {
            customData.put(MEDIA_ID, currentMediaId);
            customData.put(QUALITY, quality);
            customData.put(SERVER_URL, RESTService.getInstance().getAddress());
        } catch (JSONException e) {
            error("Error setting custom parameters for stream", e);
        }

        MediaInfo media = new MediaInfo.Builder(id.toString())
                .setContentType("media")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(MediaUtils.getMediaMetadataFromMediaDescription(item.getDescription()))
                .setCustomData(customData)
                .build();

        remoteMediaClient.load(media, autoPlay, currentPosition, customData);
    }

    private void setMetadataFromRemote() {
        Log.d(TAG, "setMetadataFromRemote()");

        // Get the custom data from the remote media information and update the local
        // metadata if it's different from the one we are currently using.
        // This can happen when the app was either reconnected, or if the
        // app joins an existing session while the cast device is playing a queue.
        try {
            MediaInfo mediaInfo = remoteMediaClient.getMediaInfo();

            if (mediaInfo == null) {
                return;
            }

            JSONObject customData = mediaInfo.getCustomData();

            if (customData != null && customData.has(MEDIA_ID)) {
                String remoteMediaId = customData.getString(MEDIA_ID);

                if (!TextUtils.equals(currentMediaId, remoteMediaId)) {
                    currentMediaId = remoteMediaId;

                    if (callback != null) {
                        callback.setCurrentMediaID(remoteMediaId);
                    }

                    updateLastKnownStreamPosition();
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Exception processing metadata from remote client", e);
        }

    }

    private void updatePlaybackState() {
        String log = "updatePlaybackState() > ";

        int status = remoteMediaClient.getPlayerState();
        int idleReason = remoteMediaClient.getIdleReason();

        // Convert the remote playback states to media playback states
        switch (status) {
            case MediaStatus.PLAYER_STATE_IDLE:
                log += "IDLE";

                if (idleReason == MediaStatus.IDLE_REASON_FINISHED && !finished) {
                    log += ":FINISHED";

                    finished = true;
                    currentJobId = null;

                    if (callback != null) {
                        callback.onCompletion();
                    }
                }

                break;

            case MediaStatus.PLAYER_STATE_BUFFERING:
                log += "BUFFERING";

                finished = false;

                playbackState = PlaybackStateCompat.STATE_BUFFERING;

                if (callback != null) {
                    callback.onPlaybackStatusChanged(playbackState);
                }

                break;

            case MediaStatus.PLAYER_STATE_PLAYING:
                log += "PLAYING";

                finished = false;

                playbackState = PlaybackStateCompat.STATE_PLAYING;
                setMetadataFromRemote();

                if (callback != null) {
                    callback.onPlaybackStatusChanged(playbackState);
                }

                break;

            case MediaStatus.PLAYER_STATE_PAUSED:
                log += "PAUSED";

                playbackState = PlaybackStateCompat.STATE_PAUSED;
                setMetadataFromRemote();

                if (callback != null) {
                    callback.onPlaybackStatusChanged(playbackState);
                }
                break;

            default:
                log += "UNKNOWN:" + status;
                break;
        }

        Log.d(TAG, log);
    }

    private void error(String message, Exception e) {
        Log.e(TAG, "error(" + message + ")", e);

        currentJobId = null;

        if (callback != null) {
            callback.onError(message);
        }
    }

    private class CastMediaClientListener implements RemoteMediaClient.Listener {
        @Override
        public void onMetadataUpdated() {
            Log.d(TAG, "RemoteMediaClient.onMetadataUpdated");
        }

        @Override
        public void onStatusUpdated() {
            Log.d(TAG, "RemoteMediaClient.onStatusUpdated");
            updatePlaybackState();
        }

        @Override
        public void onSendingRemoteMediaRequest() {
            Log.d(TAG, "RemoteMediaClient.onSendingRemoteMediaRequest");
        }

        @Override
        public void onAdBreakStatusUpdated() {
            Log.d(TAG, "RemoteMediaClient.onAdBreakStatusUpdated");
        }

        @Override
        public void onQueueStatusUpdated() {
            Log.d(TAG, "RemoteMediaClient.onQueueStatusUpdated");
        }

        @Override
        public void onPreloadStatusUpdated() {
            Log.d(TAG, "RemoteMediaClient.onPreloadStatusUpdated");
        }
    }
}
