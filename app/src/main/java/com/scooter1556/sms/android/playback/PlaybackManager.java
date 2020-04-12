package com.scooter1556.sms.android.playback;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.msebera.android.httpclient.Header;

import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ControlDispatcher;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.ext.mediasession.RepeatModeActionProvider;
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ShuffleOrder;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.HomeActivity;
import com.scooter1556.sms.android.provider.ShuffleActionProvider;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.service.SessionService;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.domain.MediaElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Manage the interactions among the container service, the queue manager and the actual playback.
 */
public class PlaybackManager implements Player.EventListener {

    private static final String TAG = "PlaybackManager";

    private static final PlaybackManager instance = new PlaybackManager();

    private static final String USER_AGENT = "SMSAndroidPlayer";
    private static final DefaultHttpDataSourceFactory DATA_SOURCE_FACTORY = new DefaultHttpDataSourceFactory(USER_AGENT);

    private static final String CHANNEL_ID = "com.scooter1556.sms.android.CHANNEL_ID";
    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    private Context ctx;
    private MediaSessionConnector mediaSessionConnector;
    private ArrayList<PlaybackListener> listeners;
    private PlayerNotificationManager playerNotificationManager;

    private List<MediaElement> queue;
    private SimpleExoPlayer localPlayer;
    private ConcatenatingMediaSource concatenatingMediaSource;
    private DefaultTrackSelector trackSelector;

    private int currentItemIndex;
    private Player currentPlayer;

    private boolean videoMode = false;

    /**
     * Listener for playback changes.
     */
    public interface PlaybackListener {

        /**
         * Called when the currently played item of the media queue changes.
         */
        void onQueuePositionChanged(int previousIndex, int newIndex);

        /**
         * Called when the current player changes.
         */
        void onPlayerChanged(Player player);
    }

    private PlaybackManager() {}

    public static PlaybackManager getInstance() {
        return instance;
    }

    public void initialise(Context ctx, MediaSessionCompat mediaSession, PlayerNotificationManager.NotificationListener nListener) {
        Log.d(TAG, "initialise()");

        this.ctx = ctx;

        this.currentItemIndex = C.INDEX_UNSET;

        this.queue = new ArrayList<>();
        listeners = new ArrayList<>();

        concatenatingMediaSource =  new ConcatenatingMediaSource();

        trackSelector = new DefaultTrackSelector();
        RenderersFactory renderersFactory = new DefaultRenderersFactory(ctx);
        localPlayer = ExoPlayerFactory.newSimpleInstance(ctx, renderersFactory, trackSelector);
        localPlayer.addListener(this);

        TimelineQueueNavigator queueNavigator = new TimelineQueueNavigator(mediaSession) {
            @Override
            public MediaDescriptionCompat getMediaDescription(Player player, int windowIndex) {
                // Get media element from queue
                MediaElement element = queue.get(windowIndex);

                if (element == null) {
                    return null;
                }

                return MediaUtils.getMediaDescription(element);
            }
        };
        
        mediaSessionConnector = new MediaSessionConnector(mediaSession);
        mediaSessionConnector.setQueueNavigator(queueNavigator);

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                ctx,
                CHANNEL_ID,
                R.string.notification_channel,
                NOTIFICATION_ID,
                new DescriptionAdapter(),
                nListener);

        playerNotificationManager.setSmallIcon(R.drawable.ic_notification);
        playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());
        playerNotificationManager.setFastForwardIncrementMs(0);
        playerNotificationManager.setRewindIncrementMs(0);

        setCurrentPlayer(localPlayer);
    }

    /**
     * Releases the manager and the players that it holds.
     */
    public void release() {
        Log.d(TAG, "release()");

        currentItemIndex = C.INDEX_UNSET;
        queue.clear();
        concatenatingMediaSource.clear();

        playerNotificationManager.setPlayer(null);

        localPlayer.stop(true);
        localPlayer.release();
        localPlayer = null;
    }

    // Queue manipulation methods.

    /**
     * Plays a specified queue item in the current player.
     *
     * @param itemIndex The index of the item to play.
     */
    public void selectQueueItem(int itemIndex) {
        Log.d(TAG, "SelectQueueItem() -> " + itemIndex);

        if(itemIndex < queue.size()) {
            setCurrentItem(itemIndex, C.TIME_UNSET, true);
        }
    }

    /**
     * Returns the index of the currently played item.
     */
    public int getCurrentItemIndex() {
        return currentItemIndex;
    }

    /**
     * Appends {@code sample} to the media queue.
     *
     * @param mediaElement The {@link MediaElement} to append.
     */
    public void addItem(MediaElement mediaElement) {
        Log.d(TAG, "addItem() > " + mediaElement.getID());

        queue.add(mediaElement);
        concatenatingMediaSource.addMediaSource(buildMediaSource(mediaElement));
    }

    /**
     * Returns the media queue.
     */
    public List<MediaElement> getQueue() {
        return this.queue;
    }

    /**
     * Returns the item at the given index in the media queue.
     *
     * @param position The index of the item.
     * @return The item at the given index in the media queue.
     */
    public MediaElement getItem(int position) {
        return queue.get(position);
    }

    public MediaDescriptionCompat getMediaDescription() {
        // Checks
        if(currentPlayer == null || queue.isEmpty() || currentItemIndex == C.INDEX_UNSET) {
            return null;
        }

        MediaElement element = queue.get(currentItemIndex);

        if(element == null) {
            return null;
        }

        return MediaUtils.getMediaDescription(element);
    }

    /**
     * Removes the item at the given index from the media queue.
     *
     * @param itemIndex The index of the item to remove.
     * @return Whether the removal was successful.
     */
    public boolean removeItem(int itemIndex) {
        Log.d(TAG, "removeItem(" + itemIndex + ")");

        concatenatingMediaSource.removeMediaSource(itemIndex);

        queue.remove(itemIndex);

        if (itemIndex == currentItemIndex && itemIndex == queue.size()) {
            maybeSetCurrentItemAndNotify(C.INDEX_UNSET);
        } else if (itemIndex < currentItemIndex) {
            maybeSetCurrentItemAndNotify(currentItemIndex - 1);
        }

        return true;
    }

    /**
     * Moves an item within the queue.
     *
     * @param fromIndex The index of the item to move.
     * @param toIndex The target index of the item in the queue.
     * @return Whether the item move was successful.
     */
    public boolean moveItem(int fromIndex, int toIndex) {
        Log.d(TAG, "moveitem() -> from=" + fromIndex + " to=" + toIndex);

        // Player update.
        concatenatingMediaSource.moveMediaSource(fromIndex, toIndex);

        queue.add(toIndex, queue.remove(fromIndex));

        // Index update.
        if (fromIndex == currentItemIndex) {
            maybeSetCurrentItemAndNotify(toIndex);
        } else if (fromIndex < currentItemIndex && toIndex >= currentItemIndex) {
            maybeSetCurrentItemAndNotify(currentItemIndex - 1);
        } else if (fromIndex > currentItemIndex && toIndex <= currentItemIndex) {
            maybeSetCurrentItemAndNotify(currentItemIndex + 1);
        }

        return true;
    }

    // Player.EventListener implementation.

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(TAG, "onPlayerStateChanged(" + playWhenReady + ", " + playbackState +  ")");

        updateCurrentItemIndex();
    }

    @Override
    public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
        Log.d(TAG, "onPositionDiscontinuity(" + reason + ")");

        updateCurrentItemIndex();
    }

    @Override
    public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, @Player.TimelineChangeReason int reason) {
        Log.d(TAG, "onTimelineChanged(" + reason + ")");

        updateCurrentItemIndex();

        // Timeline reset
        if (reason == Player.TIMELINE_CHANGE_REASON_RESET) {
            // Cancel notification
            playerNotificationManager.setPlayer(null);
        }
    }

    private void updateCurrentItemIndex() {
        Log.d(TAG, "updateCurrentItemIndex()");

        int playbackState = currentPlayer.getPlaybackState();
        maybeSetCurrentItemAndNotify(
                playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED
                        ? currentPlayer.getCurrentWindowIndex() : C.INDEX_UNSET);
    }

    private void setCurrentPlayer(Player currentPlayer) {
        Log.d(TAG, "setCurrentPlayer() > " + currentPlayer.getClass().getSimpleName());

        if (this.currentPlayer == currentPlayer) {
            return;
        }

        // Player state management.
        long playbackPositionMs = C.TIME_UNSET;
        int windowIndex = C.INDEX_UNSET;
        boolean playWhenReady = false;

        if (this.currentPlayer != null) {
            int playbackState = this.currentPlayer.getPlaybackState();

            if (playbackState != Player.STATE_ENDED) {
                playbackPositionMs = this.currentPlayer.getCurrentPosition();
                playWhenReady = this.currentPlayer.getPlayWhenReady();
                windowIndex = this.currentPlayer.getCurrentWindowIndex();
                if (windowIndex != currentItemIndex) {
                    playbackPositionMs = C.TIME_UNSET;
                    windowIndex = currentItemIndex;
                }
            }

            this.currentPlayer.stop(true);
        }

        this.currentPlayer = currentPlayer;

        // Setup media session connector
        RepeatModeActionProvider repeatProvider = new RepeatModeActionProvider(ctx, RepeatModeActionProvider.DEFAULT_REPEAT_TOGGLE_MODES);
        ShuffleActionProvider shuffleProvider = new ShuffleActionProvider(ctx);
        mediaSessionConnector.setPlayer(currentPlayer);
        mediaSessionConnector.setPlaybackPreparer(new SMSPlaybackPreparer());
        mediaSessionConnector.setCustomActionProviders(repeatProvider, shuffleProvider);

        playerNotificationManager.setPlayer(currentPlayer);

        // Initialise local player
        if (currentPlayer == localPlayer && concatenatingMediaSource.getSize() > 0) {
            // Set audio attributes so audio focus can be handled correctly
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(videoMode ? C.CONTENT_TYPE_MOVIE : C.CONTENT_TYPE_MUSIC)
                    .build();

            localPlayer.setAudioAttributes(audioAttributes, true);
            localPlayer.prepare(concatenatingMediaSource);
        }

        // Playback transition.
        if (windowIndex != C.INDEX_UNSET) {
            setCurrentItem(windowIndex, playbackPositionMs, playWhenReady);
        }

        // Update listeners
        onPlayerChanged();
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public DefaultTrackSelector getCurrentTrackSelector() {
        return trackSelector;
    }

    /**
     * Starts playback of the item at the given position.
     *
     * @param itemIndex The index of the item to play.
     * @param positionMs The position at which playback should start.
     * @param playWhenReady Whether the player should proceed when ready to do so.
     */
    private void setCurrentItem(int itemIndex, long positionMs, boolean playWhenReady) {
        Log.d(TAG, "setCurrentItem() > index=" + itemIndex + " position=" + positionMs + " playWhenReady=" + playWhenReady);

        maybeSetCurrentItemAndNotify(itemIndex);

        currentPlayer.seekTo(itemIndex, positionMs);
        currentPlayer.setPlayWhenReady(playWhenReady);
    }

    private void maybeSetCurrentItemAndNotify(int currentItemIndex) {
        Log.d(TAG, "maybeSetCurrentItemAndNotify(" + currentItemIndex + ")");

        if (this.currentItemIndex != currentItemIndex) {
            int oldIndex = this.currentItemIndex;
            this.currentItemIndex = currentItemIndex;

            // Trigger listener events
            onQueuePositionChanged(oldIndex, currentItemIndex);

            // End job
            if(currentPlayer == localPlayer && oldIndex != C.INDEX_UNSET && queue.size() > oldIndex) {
                UUID id = queue.get(oldIndex).getID();

                Log.d(TAG, "End job: " + id + ")");
                RESTService.getInstance().endJob(SessionService.getInstance().getSessionId(), id);
            }
        }
    }

    private void updateQueueFromMediaId(final String id, int extra) {
        Log.d(TAG, "updateQueueFromMediaId() > " + id);

        final List<MediaElement> newQueue = Collections.synchronizedList(new ArrayList<MediaElement>());
        List<String> parsedMediaId = MediaUtils.parseMediaId(id);

        if(parsedMediaId.isEmpty()) {
            return;
        }

        // Handle Playlist
        switch (parsedMediaId.get(0)) {
            case MediaUtils.MEDIA_ID_PLAYLIST:
                if (parsedMediaId.size() < 2) {
                    return;
                }

                final UUID playlistId = UUID.fromString(parsedMediaId.get(1));

                // Handle extra options
                boolean random = extra == MediaUtils.MEDIA_MENU_SHUFFLE;

                RESTService.getInstance().getPlaylistContents(ctx, playlistId, random, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        MediaElement element;
                        Gson parser = new Gson();

                        element = parser.fromJson(response.toString(), MediaElement.class);

                        if (element == null) {
                            throw new IllegalArgumentException("Failed to fetch contents of playlist with ID: " + playlistId);
                        }

                        newQueue.add(element);
                        updateQueue(newQueue);
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                        Gson parser = new Gson();

                        for (int i = 0; i < response.length(); i++) {
                            try {
                                MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);

                                if (element == null) {
                                    throw new IllegalArgumentException("Failed to fetch contents of playlist with ID: " + playlistId);
                                }

                                newQueue.add(element);
                            } catch (JSONException e) {
                                Log.e(TAG, "Failed to process JSON", e);
                            }
                        }

                        if (!newQueue.isEmpty()) {
                            updateQueue(newQueue);
                        } else {
                            Log.e(TAG, "No media items to add to queue after processing playlist with ID: " + playlistId);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject response) {
                        throw new IllegalArgumentException("Failed to fetch contents for playlist with ID: " + playlistId);
                    }
                });
                break;

            case MediaUtils.MEDIA_ID_RANDOM_AUDIO:
                RESTService.getInstance().getRandomMediaElements(ctx, 200, MediaElement.MediaElementType.AUDIO, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        MediaElement element;
                        Gson parser = new Gson();

                        element = parser.fromJson(response.toString(), MediaElement.class);

                        if (element == null) {
                            throw new IllegalArgumentException("Failed to fetch random media elements.");
                        }

                        newQueue.add(element);

                        if (!newQueue.isEmpty()) {
                            updateQueue(newQueue);
                        } else {
                            Log.e(TAG, "No media items to add to queue.");
                        }
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                        Gson parser = new Gson();

                        for (int i = 0; i < response.length(); i++) {
                            try {
                                MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);

                                if (element == null) {
                                    throw new IllegalArgumentException("Failed to fetch random media elements.");
                                }

                                newQueue.add(element);
                            } catch (JSONException e) {
                                Log.e(TAG, "Failed to process JSON", e);
                            }
                        }

                        if (!newQueue.isEmpty()) {
                            updateQueue(newQueue);
                        } else {
                            Log.e(TAG, "No media items to add to queue");
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject response) {
                        throw new IllegalArgumentException("Failed to fetch random media elements.");
                    }
                });
                break;

            default:
                if (parsedMediaId.size() < 2) {
                    return;
                }

                final String mediaType = parsedMediaId.get(0);
                final UUID elementId = UUID.fromString(parsedMediaId.get(1));

                switch (mediaType) {
                    case MediaUtils.MEDIA_ID_DIRECTORY_AUDIO:
                    case MediaUtils.MEDIA_ID_DIRECTORY_VIDEO:
                    case MediaUtils.MEDIA_ID_AUDIO:
                    case MediaUtils.MEDIA_ID_VIDEO:
                        RESTService.getInstance().getMediaElementContents(ctx, elementId, new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                MediaElement element;
                                Gson parser = new Gson();

                                element = parser.fromJson(response.toString(), MediaElement.class);

                                if (element == null) {
                                    throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
                                }

                                newQueue.add(element);

                                if (!newQueue.isEmpty()) {
                                    updateQueue(newQueue);
                                } else {
                                    Log.e(TAG, "No media items to add to queue after processing media ID: " + id);
                                }
                            }

                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                                boolean initQueue = (mediaType.equals(MediaUtils.MEDIA_ID_DIRECTORY_AUDIO) || mediaType.equals(MediaUtils.MEDIA_ID_DIRECTORY_VIDEO));
                                Gson parser = new Gson();

                                for (int i = 0; i < response.length(); i++) {
                                    try {
                                        MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);

                                        if (element == null) {
                                            throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
                                        }

                                        if (!initQueue && element.getID().equals(elementId)) {
                                            initQueue = true;
                                        }

                                        if (initQueue) {
                                            newQueue.add(element);
                                        }
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Failed to process JSON", e);
                                    }
                                }

                                if (!newQueue.isEmpty()) {
                                    updateQueue(newQueue);
                                } else {
                                    Log.e(TAG, "No media items to add to queue after processing media ID: " + id);
                                }
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject response) {
                                throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
                            }
                        });

                        break;
                }

                break;
        }
    }

    private void updateQueue(@NonNull List<MediaElement> elements) {
        Log.d(TAG, "updateQueue()");

        if(elements.isEmpty()) {
            return;
        }

        // Determine type from first element
        byte type = elements.get(0).getType();

        //  Reset player & end current job
        if(currentPlayer != null) {
            // Store current queue index so we can end the job
            int oldIndex = currentItemIndex;

            // Stop and reset the current player
            currentPlayer.stop(true);

            // End job
            if(currentPlayer == localPlayer && oldIndex != C.INDEX_UNSET && queue.size() > oldIndex) {
                UUID id = queue.get(oldIndex).getID();

                Log.d(TAG, "End job: " + id + ")");
                RESTService.getInstance().endJob(SessionService.getInstance().getSessionId(), id);
            }
        }

        // Reset queue and create media source array
        List<MediaSource> mediaSources = new ArrayList<>();
        queue = new ArrayList<>();

        // Process each media element
        for(MediaElement element : elements) {
            // Ensure all elements are of the same type
            if(type == element.getType()) {
                queue.add(element);
                mediaSources.add(buildMediaSource(element));
            }
        }

        // Make sure our media queue isn't empty
        if(queue.isEmpty()) {
            return;
        }

        // Create a new media source
        concatenatingMediaSource = new ConcatenatingMediaSource(false, true, new ShuffleOrder.DefaultShuffleOrder(0), mediaSources.toArray(new MediaSource[0]));

        // Update current mode
        videoMode = type == MediaElement.MediaElementType.VIDEO;

        // Handle differences between audio and video
        if(videoMode) {
            currentPlayer.setShuffleModeEnabled(false);
            currentPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
        }

         playerNotificationManager.setPlayer(currentPlayer);

        // Initialise local player
        if (currentPlayer == localPlayer) {
            // Set audio attributes so audio focus can be handled correctly
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(videoMode ? C.CONTENT_TYPE_MOVIE : C.CONTENT_TYPE_MUSIC)
                    .build();

            localPlayer.setAudioAttributes(audioAttributes, true);
            localPlayer.prepare(concatenatingMediaSource, true, true);
        }

        setCurrentItem(0, 0L, true);
    }

    private static MediaSource buildMediaSource(MediaElement mediaElement) {
        String url = RESTService.getInstance().getConnection().getUrl() + "/stream/" + SessionService.getInstance().getSessionId() + "/" + mediaElement.getID();

        return new HlsMediaSource.Factory(DATA_SOURCE_FACTORY)
                .setLoadErrorHandlingPolicy(new ErrorHandlingPolicy())
                .setTag(mediaElement.getID().toString())
                .createMediaSource(Uri.parse(url));
    }

    private class SMSPlaybackPreparer implements MediaSessionConnector.PlaybackPreparer {
        long ACTIONS = PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH
                | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH;

        @Override
        public long getSupportedPrepareActions() {
            return ACTIONS;
        }

        @Override
        public void onPrepare(boolean playWhenReady) {
            // Do nothing...
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, boolean playWhenReady, Bundle extras) {
            Log.d(TAG, "onPrepareFromMediaId(" + mediaId + ")");

            // Handle extra options
            int extra = MediaUtils.MEDIA_MENU_NONE;

            if (extras != null &&extras.containsKey(MediaUtils.EXTRA_MEDIA_OPTION)) {
                extra = extras.getInt(MediaUtils.EXTRA_MEDIA_OPTION);
            }

            updateQueueFromMediaId(mediaId, extra);
        }

        @Override
        public void onPrepareFromSearch(String query, boolean playWhenReady, Bundle extras) {
            Log.d(TAG, "onPrepareFromSearch(" + query + ")");

            // Handle extra options
            int extra = MediaUtils.MEDIA_MENU_NONE;

            if(extras != null) {
                if (extras.containsKey(MediaUtils.EXTRA_MEDIA_OPTION)) {
                    extra = extras.getInt(MediaUtils.EXTRA_MEDIA_OPTION);
                }
            }

            if (TextUtils.isEmpty(query)) {
                // Play random music
                updateQueueFromMediaId(MediaUtils.MEDIA_ID_RANDOM_AUDIO, extra);
            }
        }

        @Override
        public void onPrepareFromUri(Uri uri, boolean playWhenReady, Bundle extras) {
            // Do nothing...
        }

        @Override
        public boolean onCommand(Player player, ControlDispatcher controlDispatcher, String command, Bundle extras, ResultReceiver cb) {
            return false;
        }
    }

    private class DescriptionAdapter implements PlayerNotificationManager.MediaDescriptionAdapter {

        @Override
        public String getCurrentContentTitle(Player player) {
            MediaDescriptionCompat media = getMediaDescription();

            if(media == null) {
                return "";
            }

            return media.getTitle().toString();
        }

        @Nullable
        @Override
        public String getCurrentContentText(Player player) {
            MediaDescriptionCompat media = getMediaDescription();

            if(media == null) {
                return "";
            }

            return media.getSubtitle().toString();
        }

        @Nullable
        @Override
        public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
            Bitmap placeholder = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_placeholder_audio);
            MediaDescriptionCompat media = getMediaDescription();

            if(media != null) {
                Glide.with(ctx)
                        .asBitmap()
                        .load(media.getIconUri())
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                callback.onBitmap(resource);
                            }
                        });
            }

            return placeholder;
        }

        @Nullable
        @Override
        public PendingIntent createCurrentContentIntent(Player player) {
            Intent intent = new Intent(ctx, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            if(player == localPlayer && videoMode) {
                intent.putExtra(HomeActivity.EXTRA_START_VIDEO_FULLSCREEN, true);
            } else {
                intent.putExtra(HomeActivity.EXTRA_START_FULLSCREEN, true);
            }

            return PendingIntent.getActivity(ctx, REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
    }

    /**
     * Listener
     */

    public void addListener(@NonNull PlaybackListener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull  PlaybackListener listener) {
        listeners.remove(listener);
    }

    private void onQueuePositionChanged(int previousIndex, int newIndex) {
        for (PlaybackListener listener : listeners) {
            listener.onQueuePositionChanged(previousIndex, newIndex);
        }
    }

    private void onPlayerChanged() {
        for (PlaybackListener listener : listeners) {
            listener.onPlayerChanged(currentPlayer);
        }
    }

}
