package com.scooter1556.sms.android.playback;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.lib.android.domain.MediaElement;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manage the interactions among the container service, the queue manager and the actual playback.
 */
public class PlaybackManager implements Playback.Callback {

    private static final String TAG = "PlaybackManager";

    private Context ctx;
    private QueueManager queueManager;
    private Playback playback;
    private PlaybackServiceCallback serviceCallback;
    private MediaSessionCallback mediaSessionCallback;

    private boolean randomMode = false;

    public PlaybackManager(Context ctx, PlaybackServiceCallback serviceCallback, QueueManager queueManager, Playback playback) {
        this.ctx = ctx;
        this.serviceCallback = serviceCallback;
        this.queueManager = queueManager;
        this.mediaSessionCallback = new MediaSessionCallback();
        this.playback = playback;
        this.playback.setCallback(this);
    }

    public Playback getPlayback() {
        return playback;
    }

    public MediaSessionCompat.Callback getMediaSessionCallback() {
        return mediaSessionCallback;
    }

    /**
     * Handle a request to play media
     */
    public void handlePlayRequest() {
        Log.d(TAG, "handlePlayRequest()");

        MediaSessionCompat.QueueItem currentMedia = queueManager.getCurrentMedia();

        if (currentMedia != null) {
            serviceCallback.onPlaybackStart();
            playback.play(currentMedia.getDescription().getMediaId(), !randomMode);
        }
    }

    /**
     * Handle a request to play random media
     */
    public void handleRandomRequest() {
        RESTService.getInstance().getRandomAudioElement(ctx, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                MediaElement element;
                Gson parser = new Gson();

                element = parser.fromJson(response.toString(), MediaElement.class);

                if (element == null) {
                    throw new IllegalArgumentException("Failed to fetch random audio element.");
                }

                queueManager.addToQueueFromMediaElement(element);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                throw new IllegalArgumentException("Failed to fetch random audio element.");
            }
        });
    }

    /**
     * Handle a request to pause media
     */
    public void handlePauseRequest() {
        if (playback.isPlaying()) {
            playback.pause();
            serviceCallback.onPlaybackStop();
        }
    }

    /**
     * Handle a request to stop media
     */
    public void handleStopRequest(String error) {
        playback.stop(true);
        serviceCallback.onPlaybackStop();
        updatePlaybackState(error);
    }


    /**
     * Update the current media player state, optionally showing an error message.
     */
    public void updatePlaybackState(String error) {
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;

        if (playback != null && playback.isConnected()) {
            position = playback.getCurrentStreamPosition();
        }

        //noinspection ResourceType
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());

        int state = playback.getState();

        // If there is an error message, send it to the playback state:
        if (error != null) {
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
        }

        //noinspection ResourceType
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the active Queue Item ID if the current index is valid.
        MediaSessionCompat.QueueItem currentMedia = queueManager.getCurrentMedia();

        if (currentMedia != null) {
            stateBuilder.setActiveQueueItemId(currentMedia.getQueueId());
        }

        serviceCallback.onPlaybackStateUpdated(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
            serviceCallback.onNotificationRequired();
        }
    }

    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE |
                       PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                       PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                       PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                       PlaybackStateCompat.ACTION_SKIP_TO_NEXT;

        if (playback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }

        return actions;
    }

    /**
     * Implementation of the Playback.Callback interface
     */
    @Override
    public void onCompletion() {
        Log.d(TAG, "onCompletion()");

        if (queueManager.skipQueuePosition(1)) {
            handlePlayRequest();
            queueManager.updateMetadata();
        } else if(randomMode) {
            handleRandomRequest();
        } else {
            handleStopRequest(null);
        }
    }

    @Override
    public void onPlaybackStatusChanged(int state) {
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error) {
        updatePlaybackState(error);
    }

    @Override
    public void setCurrentMediaID(String mediaID) {
        queueManager.setCurrentQueueItem(mediaID);
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
        playback.destroy();

        Log.d(TAG, "switchToPlayback(" + newPlayback.getClass().toString() +
                " Resume: " + resumePlaying +
                " State: " + oldState +
                " Position: " + pos +
                " Media ID: " + currentMediaID +
                ")");

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
                MediaSessionCompat.QueueItem currentMedia = queueManager.getCurrentMedia();

                if (resumePlaying && currentMedia != null) {
                    playback.play(currentMedia.getDescription().getMediaId(), !randomMode);
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

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            Log.d(TAG, "onPlay()");

            if (queueManager.getCurrentMedia() == null) {
                return;
            }

            handlePlayRequest();
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            Log.d(TAG, "onSkipToQueueItem(" + queueId + ")");

            queueManager.setCurrentQueueItem(queueId);
            queueManager.updateMetadata();
        }

        @Override
        public void onSeekTo(long position) {
            playback.seekTo((int) position);
        }

        @Override
        public void onPause() {
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            handleStopRequest(null);
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "onSkipToNext()");

            if(queueManager.skipQueuePosition(1)) {
                handlePlayRequest();
            } else if(randomMode) {
                handleRandomRequest();
            } else {
                handleStopRequest("Cannot skip");
            }

            queueManager.updateMetadata();
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious()");

            if (queueManager.skipQueuePosition(-1)) {
                handlePlayRequest();
            } else {
                handleStopRequest("Cannot skip");
            }

            queueManager.updateMetadata();
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG, "onPlayFromMediaId(" + mediaId + ")");

            if(MediaUtils.getMediaTypeFromID(mediaId).equals(MediaElement.MediaElementType.AUDIO)) {
                queueManager.setQueueFromMediaId(mediaId);
                randomMode = false;
            } else if(MediaUtils.getMediaTypeFromID(mediaId).equals(MediaElement.MediaElementType.VIDEO)) {
                serviceCallback.onPlaybackStart();
                playback.play(mediaId, !randomMode);
            }
        }

        /**
         * Handle free and contextual searches.
         **/
        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {
            Log.d(TAG, "onPlayFromSearch(" + query + ")");

            if (TextUtils.isEmpty(query)) {
                // The user provided generic string e.g. 'Play music'
                // Play random music
                randomMode = true;
                queueManager.initialiseQueue("Shuffle All");
                handleRandomRequest();
            }
        }
    }


    public interface PlaybackServiceCallback {
        void onPlaybackStart();

        void onNotificationRequired();

        void onPlaybackStop();

        void onPlaybackStateUpdated(PlaybackStateCompat newState);
    }
}
