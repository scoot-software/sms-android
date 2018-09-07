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
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadOptions;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.domain.MediaElement;

import java.util.List;
import java.util.UUID;

/**
 * An implementation of Playback that talks to Cast devices.
 */
public class CastPlayback implements Playback {

    private static final String TAG = "CastPlayback";

    private final Context appContext;
    private final RemoteMediaClient remoteMediaClient;
    private final RemoteMediaClient.Callback remoteMediaClientCallback;

    private int playbackState;

    private Callback callback;
    private volatile long currentPosition;
    private volatile String currentMediaId;

    private boolean finished = false;

    public CastPlayback(Context context) {
        appContext = context.getApplicationContext();

        CastSession castSession = CastContext.getSharedInstance(appContext).getSessionManager().getCurrentCastSession();
        remoteMediaClient = castSession.getRemoteMediaClient();
        remoteMediaClientCallback = new RemoteMediaClient.Callback() {
            @Override
            public void onMetadataUpdated() {
                Log.d(TAG, "RemoteMediaClient.onMetadataUpdated");
            }

            @Override
            public void onStatusUpdated() {
                Log.d(TAG, "RemoteMediaClient.onStatusUpdated");
                updatePlaybackState();
            }
        };
    }

    @Override
    public void start() {
        Log.d(TAG, "start()");

        remoteMediaClient.registerCallback(remoteMediaClientCallback);
    }

    @Override
    public void stop(boolean notifyListeners) {
        Log.d(TAG, "stop()");

        playbackState = PlaybackStateCompat.STATE_STOPPED;

        if (notifyListeners && callback != null) {
            callback.onPlaybackStatusChanged(playbackState);
        }
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
    public void setSessionId(UUID sessionId) {}

    @Override
    public UUID getSessionId() { return null; }

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

        byte type = MediaUtils.getMediaTypeFromID(currentMediaId);
        UUID id = UUID.fromString(mediaID.get(1));

        MediaInfo media = new MediaInfo.Builder(id.toString())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(MediaUtils.getMediaMetadataFromMediaDescription(item.getDescription()))
                .setContentType(type == MediaElement.MediaElementType.AUDIO ? MimeTypes.AUDIO_UNKNOWN : MimeTypes.VIDEO_UNKNOWN)
                .build();

        MediaLoadOptions loadOptions = new MediaLoadOptions.Builder()
                .setAutoplay(autoPlay)
                .setPlayPosition(currentPosition)
                .build();

        remoteMediaClient.load(media, loadOptions);
    }

    private void setMetadataFromRemote() {
        Log.d(TAG, "setMetadataFromRemote()");

        // Get the custom data from the remote media information and update the local
        // metadata if it's different from the one we are currently using.
        // This can happen when the app was either reconnected, or if the
        // app joins an existing session while the cast device is playing a queue.
        MediaInfo mediaInfo = remoteMediaClient.getMediaInfo();

        if (mediaInfo == null) {
            return;
        }

        // TODO
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

        if (callback != null) {
            callback.onError(message);
        }
    }
}
