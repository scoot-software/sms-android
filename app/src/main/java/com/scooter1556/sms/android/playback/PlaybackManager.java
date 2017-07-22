package com.scooter1556.sms.android.playback;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.VideoPlaybackActivity;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.domain.MediaElement;

import org.json.JSONObject;

/**
 * Manage the interactions among the container service, the queue manager and the actual playback.
 */
public class PlaybackManager implements Playback.Callback {

    private static final String TAG = "PlaybackManager";

    private static final PlaybackManager instance = new PlaybackManager();

    private Context ctx;
    private QueueManager queueManager;
    private Playback playback;
    private PlaybackServiceCallback serviceCallback;
    private MediaSessionCallback mediaSessionCallback;

    private int repeatMode = PlaybackStateCompat.REPEAT_MODE_NONE;
    private boolean shuffleMode = false;

    public PlaybackManager() {}

    public static PlaybackManager getInstance() {
        return instance;
    }

    public void initialise(Context ctx, PlaybackServiceCallback serviceCallback, QueueManager queueManager) {
        this.ctx = ctx;
        this.serviceCallback = serviceCallback;
        this.queueManager = queueManager;
        this.mediaSessionCallback = new MediaSessionCallback();
    }

    public void setPlayback(Playback playback) {
        if(this.playback != null) {
            this.playback.stop(true);
            this.playback.destroy();
        }

        this.playback = playback;
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

        if (currentMedia == null || currentMedia.getDescription() == null) {
            return;
        }

        // Check current playback
        if (MediaUtils.getMediaTypeFromID(currentMedia.getDescription().getMediaId()) == MediaElement.MediaElementType.AUDIO) {
            if (playback instanceof VideoPlaybackActivity) {
                playback.destroy();
                playback = null;
            }

            if (playback == null) {
                playback = new AudioPlayback(ctx);
                playback.setCallback(this);
            }

            playback.play(currentMedia);

        } else if (MediaUtils.getMediaTypeFromID(currentMedia.getDescription().getMediaId()) == MediaElement.MediaElementType.VIDEO) {
            if (playback instanceof AudioPlayback) {
                playback.destroy();
                playback = null;
            }

            if (playback == null) {
                // Start video playback activity
                Intent intent = new Intent(ctx, VideoPlaybackActivity.class)
                        .putExtra(MediaUtils.EXTRA_MEDIA_ITEM, currentMedia);
                ctx.startActivity(intent);
            } else {
                playback.play(currentMedia);
            }
        }

        // Notify service callback
        serviceCallback.onPlaybackStart();
    }

    /**
     * Handle a request to pause media
     */
    public void handlePauseRequest() {
        if (playback != null && playback.isPlaying()) {
            playback.pause();
            serviceCallback.onPlaybackStop();
        }
    }

    /**
     * Handle a request to stop media
     */
    public void handleStopRequest(String error) {
        if(playback != null) {
            playback.stop(true);
            updatePlaybackState(error);
        }

        serviceCallback.onPlaybackStop();
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
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(getAvailableActions());

        // Shuffle
        stateBuilder.addCustomAction(shuffleMode ? MediaService.STATE_SHUFFLE_ON : MediaService.STATE_SHUFFLE_OFF,
                                     shuffleMode ? ctx.getResources().getString(R.string.description_shuffle_on) : ctx.getResources().getString(R.string.description_shuffle_off),
                                     shuffleMode ?  R.drawable.ic_shuffle_enabled_white_48dp : R.drawable.ic_shuffle_white_48dp);

        // Repeat
        switch(repeatMode) {
            case PlaybackStateCompat.REPEAT_MODE_NONE:
                stateBuilder.addCustomAction(MediaService.STATE_REPEAT_NONE, ctx.getResources().getString(R.string.description_repeat_none), R.drawable.ic_repeat_white_48dp);
                break;

            case PlaybackStateCompat.REPEAT_MODE_ALL:
                stateBuilder.addCustomAction(MediaService.STATE_REPEAT_ALL, ctx.getResources().getString(R.string.description_repeat_all), R.drawable.ic_repeat_enable_white_48dp);
                break;

            case PlaybackStateCompat.REPEAT_MODE_ONE:
                stateBuilder.addCustomAction(MediaService.STATE_REPEAT_ONE, ctx.getResources().getString(R.string.description_repeat_one), R.drawable.ic_repeat_one_white_48dp);
                break;
        }


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
                       PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                       PlaybackStateCompat.ACTION_SET_REPEAT_MODE |
                       PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE_ENABLED;

        if (playback != null && playback.isPlaying()) {
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

        if(repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE) {
            handlePlayRequest();
        } else if(queueManager.skipQueuePosition(1)) {
            handlePlayRequest();
        } else if(repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL) {
            queueManager.setCurrentQueueIndex(0);
            handlePlayRequest();
        } else {
            handleStopRequest(null);
        }

        queueManager.updateMetadata();
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
        int oldState = playback == null ? PlaybackStateCompat.STATE_NONE : playback.getState();
        long pos = playback == null ? 0 : playback.getCurrentStreamPosition();
        String currentMediaID = playback == null ? "" : playback.getCurrentMediaId();

        if(playback != null) {
            playback.stop(false);
            playback.destroy();
        }

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
                    playback.play(currentMedia);
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
            } else if(repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL) {
                queueManager.setCurrentQueueIndex(0);
                handlePlayRequest();
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

            queueManager.setQueueFromMediaId(mediaId);
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
                queueManager.setQueueFromMediaId(MediaUtils.MEDIA_ID_RANDOM_AUDIO);
            }
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            Log.d(TAG, "onCustomAction(" + action + ")");

            switch(action) {

                case MediaService.STATE_SHUFFLE_ON:
                    shuffleMode = false;
                    queueManager.setShuffleMode(false);
                    break;

                case MediaService.STATE_SHUFFLE_OFF:
                    shuffleMode = true;
                    queueManager.setShuffleMode(true);
                    break;

                case MediaService.STATE_REPEAT_NONE:
                    repeatMode = PlaybackStateCompat.REPEAT_MODE_ALL;
                    break;

                case MediaService.STATE_REPEAT_ALL:
                    repeatMode = PlaybackStateCompat.REPEAT_MODE_ONE;
                    break;

                case MediaService.STATE_REPEAT_ONE:
                    repeatMode = PlaybackStateCompat.REPEAT_MODE_NONE;
                    break;

            }

            updatePlaybackState(null);
        }
    }


    public interface PlaybackServiceCallback {
        void onPlaybackStart();
        void onNotificationRequired();
        void onPlaybackStop();
        void onPlaybackStateUpdated(PlaybackStateCompat newState);
    }
}
