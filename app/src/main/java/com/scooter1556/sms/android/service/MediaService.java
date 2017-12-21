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
package com.scooter1556.sms.android.service;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.NowPlayingActivity;
import com.scooter1556.sms.android.domain.Playlist;
import com.scooter1556.sms.android.manager.MediaNotificationManager;
import com.scooter1556.sms.android.playback.AudioPlayback;
import com.scooter1556.sms.android.playback.CastPlayback;
import com.scooter1556.sms.android.playback.Playback;
import com.scooter1556.sms.android.playback.PlaybackManager;
import com.scooter1556.sms.android.playback.QueueManager;
import com.scooter1556.sms.android.utils.AutoUtils;
import com.scooter1556.sms.android.utils.NetworkUtils;
import com.scooter1556.sms.android.utils.ResourceUtils;
import com.scooter1556.sms.android.utils.TVUtils;
import com.scooter1556.sms.android.database.ConnectionDatabase;
import com.scooter1556.sms.android.domain.Connection;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.domain.MediaFolder;
import com.scooter1556.sms.android.utils.MediaUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MediaService extends MediaBrowserServiceCompat
                          implements PlaybackManager.PlaybackServiceCallback {

    private static final String TAG = "MediaService";

    // Extra on MediaSession that contains the Cast device name currently connected to
    public static final String EXTRA_CONNECTED_CAST = "com.example.android.uamp.CAST_NAME";
    // The action of the incoming Intent indicating that it contains a command
    // to be executed (see {@link #onStartCommand})
    public static final String ACTION_CMD = "com.example.android.uamp.ACTION_CMD";
    // The key in the extras of the incoming Intent indicating the command that
    // should be executed (see {@link #onStartCommand})
    public static final String CMD_NAME = "CMD_NAME";
    // A value of a CMD_NAME key in the extras of the incoming Intent that
    // indicates that the music playback should be paused (see {@link #onStartCommand})
    public static final String CMD_PAUSE = "CMD_PAUSE";
    // A value of a CMD_NAME key that indicates that the music playback should switch
    // to local playback from cast playback.
    public static final String CMD_STOP_CASTING = "CMD_STOP_CASTING";

    // Shuffle
    public static final String STATE_SHUFFLE_ON = "state_shuffle_on";
    public static final String STATE_SHUFFLE_OFF = "state_shuffle_off";

    // Repeat
    public static final String STATE_REPEAT_NONE = "state_repeat_none";
    public static final String STATE_REPEAT_ALL = "state_repeat_all";
    public static final String STATE_REPEAT_ONE = "state_repeat_one";

    // Actions
    public static final String ACTION_CLEAR_PLAYLIST = "action_clear_playlist";

    // Delay stopSelf by using a handler.
    private static final int STOP_DELAY = 30000;

    // Number of media elements to fetch when populating lists
    public static final int FETCH_LIMIT = 50;

    private PlaybackManager playbackManager;
    private QueueManager queueManager;

    private MediaSessionCompat mediaSession;
    private Bundle mediaSessionExtras;
    private MediaNotificationManager mediaNotificationManager;
    private final DelayedStopHandler delayedStopHandler = new DelayedStopHandler(this);
    private MediaRouter mediaRouter;
    private SessionManager castSessionManager;
    private SessionManagerListener<CastSession> castSessionManagerListener;

    boolean isOnline = false;
    private boolean isConnected = false;
    private boolean isConnectedToCar;
    private BroadcastReceiver carConnectionReceiver;

    // REST Client
    RESTService restService = null;

    private final BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean status = NetworkUtils.isOnline(context);

            if (isOnline != status) {
                isOnline = status;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate()");

        restService = RESTService.getInstance();

        // Retrieve preferences if they exist
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Load default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Initialise database
        ConnectionDatabase db = new ConnectionDatabase(this);

        // Check connection
        long id = sharedPreferences.getLong("Connection", -1);

        if(id >= 0) {
            Connection connection = db.getConnection(id);
            restService.setConnection(connection);
            isConnected = true;
        } else {
            isConnected = false;
        }

        queueManager = new QueueManager(getApplicationContext(), new QueueManager.MetadataUpdateListener() {
                    @Override
                    public void onMetadataChanged(MediaMetadataCompat metadata) {
                        Log.d(TAG, "onMetadataChanged()");

                        mediaSession.setMetadata(metadata);
                    }

                    @Override
                    public void onMetadataRetrieveError() {
                        Log.d(TAG, "onMetadataRetrieveError()");

                        playbackManager.updatePlaybackState(getString(R.string.error_no_metadata));
                    }

                    @Override
                    public void onCurrentQueueIndexUpdated(int queueIndex) {
                        Log.d(TAG, "onCurrentQueueIndexUpdated(" + queueIndex + ")");

                        playbackManager.handlePlayRequest();
                    }

                    @Override
                    public void onQueueUpdated(List<MediaSessionCompat.QueueItem> newQueue) {
                        Log.d(TAG, "onQueueUpdated()");

                        mediaSession.setQueue(newQueue);
                        mediaSession.setQueueTitle("Now Playing");
                    }
                });

        // Initialise playback manager
        playbackManager = PlaybackManager.getInstance();
        playbackManager.initialise(getApplicationContext(), this, queueManager);

        // Start a new Media Session
        mediaSession = new MediaSessionCompat(this, MediaService.class.getSimpleName());
        mediaSession.setCallback(playbackManager.getMediaSessionCallback());
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);

        Context context = getApplicationContext();
        Intent intent = new Intent(context, NowPlayingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 99, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mediaSession.setSessionActivity(pendingIntent);

        mediaSessionExtras = new Bundle();
        AutoUtils.setSlotReservationFlags(mediaSessionExtras, true, true, true);
        mediaSession.setExtras(mediaSessionExtras);

        setSessionToken(mediaSession.getSessionToken());

        try {
            mediaNotificationManager = new MediaNotificationManager(this);
        } catch (RemoteException e) {
            throw new IllegalStateException("Could not create a MediaNotificationManager", e);
        }

        if (!TVUtils.isTvUiMode(this)) {
            castSessionManager = CastContext.getSharedInstance(this).getSessionManager();
            castSessionManagerListener = new CastSessionManagerListener();
            castSessionManager.addSessionManagerListener(castSessionManagerListener, CastSession.class);
        }

        mediaRouter = MediaRouter.getInstance(getApplicationContext());

        registerCarConnectionReceiver();

        // Register connectivity receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        this.registerReceiver(connectivityChangeReceiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");

        if (startIntent != null) {
            String action = startIntent.getAction();
            String command = startIntent.getStringExtra(CMD_NAME);

            if (ACTION_CMD.equals(action)) {
                if (CMD_PAUSE.equals(command)) {
                    playbackManager.handlePauseRequest();
                } else if (CMD_STOP_CASTING.equals(command)) {
                    CastContext.getSharedInstance(this).getSessionManager().endCurrentSession(true);
                }
            } else {
                // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
                MediaButtonReceiver.handleIntent(mediaSession, startIntent);
            }
        }

        // Reset the delay handler to enqueue a message to stop the service if
        // nothing is playing.
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        return START_STICKY;
    }

    /*
     * Handle case when user swipes the app away from the recent apps list by
     * stopping the service (and any ongoing playback).
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        unregisterCarConnectionReceiver();

        // Service is being killed, so make sure we release our resources
        playbackManager.handleStopRequest(null);
        mediaNotificationManager.stopNotification();

        if (castSessionManager != null) {
            castSessionManager.removeSessionManagerListener(castSessionManagerListener, CastSession.class);
        }

        delayedStopHandler.removeCallbacksAndMessages(null);
        mediaSession.release();

        // Unregister receiver
        this.unregisterReceiver(connectivityChangeReceiver);
    }

    /**
     * Callback method called from PlaybackManager whenever the music is about to play.
     */
    @Override
    public void onPlaybackStart() {
        Log.d(TAG, "onPlaybackStart()");

        mediaSession.setActive(true);

        delayedStopHandler.removeCallbacksAndMessages(null);

        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the media playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        startService(new Intent(getApplicationContext(), MediaService.class));
    }


    /**
     * Callback method called from PlaybackManager whenever the music stops playing.
     */
    @Override
    public void onPlaybackStop() {
        Log.d(TAG, "onPlaybackStop()");

        mediaSession.setActive(false);

        // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        stopForeground(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onNotificationRequired() {
        mediaNotificationManager.startNotification();
    }

    @Override
    public void onPlaybackStateUpdated(PlaybackStateCompat newState) {
        mediaSession.setPlaybackState(newState);
    }

    private void registerCarConnectionReceiver() {
        IntentFilter filter = new IntentFilter(AutoUtils.ACTION_MEDIA_STATUS);

        carConnectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String connectionEvent = intent.getStringExtra(AutoUtils.MEDIA_CONNECTION_STATUS);
                isConnectedToCar = AutoUtils.MEDIA_CONNECTED.equals(connectionEvent);
                Log.i(TAG, "Connection event to Android Auto: " + connectionEvent + " isConnectedToCar=" + isConnectedToCar);
            }
        };

        registerReceiver(carConnectionReceiver, filter);
    }

    private void unregisterCarConnectionReceiver() {
        unregisterReceiver(carConnectionReceiver);
    }

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private static class DelayedStopHandler extends Handler {
        private final WeakReference<MediaService> reference;

        private DelayedStopHandler(MediaService service) {
            reference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaService service = reference.get();

            if (service != null && service.playbackManager.getPlayback() != null) {
                if (!service.playbackManager.getPlayback().isPlaying()) {
                    service.stopSelf();
                }
            }
        }
    }

    /**
     * Session Manager Listener responsible for switching the Playback instances
     * depending on whether it is connected to a remote player.
     */
    private class CastSessionManagerListener implements SessionManagerListener<CastSession> {

        @Override
        public void onSessionEnded(CastSession session, int error) {
            Log.d(TAG, "Cast: onSessionEnded()");

            mediaSessionExtras.remove(EXTRA_CONNECTED_CAST);
            mediaSession.setExtras(mediaSessionExtras);
            mediaRouter.setMediaSessionCompat(null);

            Playback playback = new AudioPlayback(MediaService.this);
            playbackManager.switchToPlayback(playback, true);
        }

        @Override
        public void onSessionResumed(CastSession session, boolean wasSuspended) {
            Log.d(TAG, "Cast: onSessionResumed(" + wasSuspended + ")");

            if(!(playbackManager.getPlayback() instanceof CastPlayback)) {
                // In case we are casting, send the device name as an extra on Media Session metadata.
                mediaSessionExtras.putString(EXTRA_CONNECTED_CAST, session.getCastDevice().getFriendlyName());
                mediaSession.setExtras(mediaSessionExtras);

                // Now we can switch to Cast Playback
                Playback playback = new CastPlayback(MediaService.this);
                mediaRouter.setMediaSessionCompat(mediaSession);
                playbackManager.switchToPlayback(playback, !wasSuspended);
            }
        }

        @Override
        public void onSessionStarted(CastSession session, String sessionId) {
            Log.d(TAG, "Cast: onSessionStarted() > sessionId = " + sessionId);

            // Activate session ID on server
            RESTService.getInstance().addSession(UUID.fromString(sessionId));

            // In case we are casting, send the device name as an extra on Media Session metadata.
            mediaSessionExtras.putString(EXTRA_CONNECTED_CAST, session.getCastDevice().getFriendlyName());
            mediaSession.setExtras(mediaSessionExtras);

            // Now we can switch to Cast Playback
            Playback playback = new CastPlayback(MediaService.this);
            mediaRouter.setMediaSessionCompat(mediaSession);
            playbackManager.switchToPlayback(playback, true);
        }

        @Override
        public void onSessionStarting(CastSession session) {
            Log.d(TAG, "Cast: onSessionStarting()");
        }

        @Override
        public void onSessionStartFailed(CastSession session, int error) {
            Log.d(TAG, "Cast: onSessionStartFailed() > error = " + error);
        }

        @Override
        public void onSessionEnding(CastSession session) {
            Log.d(TAG, "Cast: onSessionEnding()");

            // This is our final chance to update the current stream position
            long pos = session.getRemoteMediaClient().getApproximateStreamPosition();
            playbackManager.getPlayback().setCurrentStreamPosition(pos);
        }

        @Override
        public void onSessionResuming(CastSession session, String sessionId) {
            Log.d(TAG, "Cast: onSessionResuming()");
        }

        @Override
        public void onSessionResumeFailed(CastSession session, int error) {
            Log.d(TAG, "Cast: onSessionResumeFailed()");
        }

        @Override
        public void onSessionSuspended(CastSession session, int reason) {
            Log.d(TAG, "Cast: onSessionSuspended()");
        }
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        Log.d(TAG, "onGetRoot()");

        // Return audio menu if connected to car
        if(isConnectedToCar) {
            return new BrowserRoot(MediaUtils.MEDIA_ID_MENU_AUDIO, null);
        }

        return new BrowserRoot(MediaUtils.MEDIA_ID_MENU_AUDIO, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "MEDIA ID: " + parentId);

        List<String> mediaId = MediaUtils.parseMediaId(parentId);

        if(mediaId.isEmpty()) {
            Log.e(TAG, "Unable to parse media ID: " + parentId);
            return;
        }

        String root = mediaId.get(0);

        switch(root) {
            case MediaUtils.MEDIA_ID_COLLECTIONS:
                result.detach();
                getCollections(result);
                break;

            case MediaUtils.MEDIA_ID_COLLECTION:
                if(mediaId.size() > 1) {
                    result.detach();
                    getMediaElementForCollection(mediaId.get(1), result);
                }

                break;

            case MediaUtils.MEDIA_ID_ALBUM:
                if(mediaId.size() > 1) {
                    result.detach();
                    getMediaElementForAlbum(mediaId.get(1), result);
                }

                break;

            case MediaUtils.MEDIA_ID_ARTIST_ALBUM:
                if(mediaId.size() > 2) {
                    result.detach();
                    getMediaElementForAlbumAndArtist(mediaId.get(1), mediaId.get(2), result);
                }

                break;

            case MediaUtils.MEDIA_ID_ALBUM_ARTIST_ALBUM:
                if(mediaId.size() > 2) {
                    result.detach();
                    getMediaElementForAlbumAndAlbumArtist(mediaId.get(1), mediaId.get(2), result);
                }

                break;

            case MediaUtils.MEDIA_ID_ALBUM_ARTIST:
                if(mediaId.size() > 1) {
                    result.detach();
                    getAlbumsByAlbumArtist(mediaId.get(1), result);
                }

                break;

            case MediaUtils.MEDIA_ID_ALBUM_ARTISTS:
                result.detach();
                getAlbumArtists(result);
                break;

            case MediaUtils.MEDIA_ID_ALBUMS:
                result.detach();
                getAlbums(result);
                break;

            case MediaUtils.MEDIA_ID_ARTIST:
                if(mediaId.size() > 1) {
                    result.detach();
                    getAlbumsByArtist(mediaId.get(1), result);
                }

                break;

            case MediaUtils.MEDIA_ID_ARTISTS:
                result.detach();
                getArtists(result);
                break;

            case MediaUtils.MEDIA_ID_AUDIO:
                break;

            case MediaUtils.MEDIA_ID_DIRECTORY: case MediaUtils.MEDIA_ID_DIRECTORY_AUDIO: case MediaUtils.MEDIA_ID_DIRECTORY_VIDEO:
                if(mediaId.size() > 1) {
                    result.detach();
                    getMediaElementContents(UUID.fromString(mediaId.get(1)), result);
                }

                break;

            case MediaUtils.MEDIA_ID_FOLDER: case MediaUtils.MEDIA_ID_FOLDER_AUDIO: case MediaUtils.MEDIA_ID_FOLDER_VIDEO:
                if(mediaId.size() > 1) {
                    result.detach();
                    getMediaFolderContents(UUID.fromString(mediaId.get(1)), result);
                }

                break;

            case MediaUtils.MEDIA_ID_FOLDERS:
                result.detach();
                getMediaFolders(result);
                break;

            case MediaUtils.MEDIA_ID_FOLDERS_AUDIO:
                break;

            case MediaUtils.MEDIA_ID_FOLDERS_VIDEO:
                break;

            case MediaUtils.MEDIA_ID_MENU_AUDIO:
                result.sendResult(getAudioMenu());
                break;

            case MediaUtils.MEDIA_ID_MENU_VIDEO:
                break;

            case MediaUtils.MEDIA_ID_RECENTLY_ADDED:
                result.detach();
                getRecentlyAdded(null, result);
                break;

            case MediaUtils.MEDIA_ID_RECENTLY_PLAYED:
                result.detach();
                getRecentlyPlayed(null, result);
                break;

            case MediaUtils.MEDIA_ID_RECENTLY_ADDED_AUDIO:
                result.detach();
                getRecentlyAdded(MediaElement.DirectoryMediaType.AUDIO, result);
                break;

            case MediaUtils.MEDIA_ID_RECENTLY_ADDED_VIDEO:
                result.detach();
                getRecentlyAdded(MediaElement.DirectoryMediaType.VIDEO, result);
                break;

            case MediaUtils.MEDIA_ID_RECENTLY_PLAYED_AUDIO:
                result.detach();
                getRecentlyPlayed(MediaElement.DirectoryMediaType.AUDIO, result);
                break;

            case MediaUtils.MEDIA_ID_RECENTLY_PLAYED_VIDEO:
                result.detach();
                getRecentlyPlayed(MediaElement.DirectoryMediaType.VIDEO, result);
                break;

            case MediaUtils.MEDIA_ID_VIDEO:
                break;

            case MediaUtils.MEDIA_ID_PLAYLISTS:
                result.detach();
                getPlaylists(result);
                break;

            case MediaUtils.MEDIA_ID_PLAYLIST:
                if(mediaId.size() > 1) {
                    result.detach();
                    getPlaylistContents(UUID.fromString(mediaId.get(1)), result);
                }

                break;

            default:
                Log.w(TAG, "Media ID not recognised: " + parentId);
                break;
        }
    }

    /**
     * Fetch a list of all Media Folders.
     */
    private void getMediaFolders(@NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {

        restService.getMediaFolders(getApplicationContext(), new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> folders = new ArrayList<>();
                MediaFolder folder = parser.fromJson(response.toString(), MediaFolder.class);

                if(folder.getType() == MediaFolder.ContentType.AUDIO || folder.getType() == MediaFolder.ContentType.VIDEO) {
                    folders.add(createMediaItemFromMediaFolder(folder));
                }

                result.sendResult(folders);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> folders = new ArrayList<>();

                for(int i=0; i<response.length(); i++) {
                    try {
                        MediaFolder folder = parser.fromJson(response.getJSONObject(i).toString(), MediaFolder.class);

                        if(folder.getType() == MediaFolder.ContentType.AUDIO || folder.getType() == MediaFolder.ContentType.VIDEO) {
                            folders.add(createMediaItemFromMediaFolder(folder));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(folders);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve media folder list");
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve media folder list");
            }
        });
    }

    /**
     * Fetch the contents of a given Media Folder.
     */
    private void getMediaFolderContents(UUID id, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        restService.getMediaFolderContents(getApplicationContext(), id, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();
                MediaElement element = parser.fromJson(response.toString(), MediaElement.class);
                elements.add(createMediaItemFromMediaElement(element));
                result.sendResult(elements);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);
                        elements.add(createMediaItemFromMediaElement(element));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(elements);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve media folder contents");
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve media folder contents");
            }
        });
    }

    /**
     * Fetch a list of playlists.
     */
    private void getPlaylists(@NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {

        restService.getPlaylists(getApplicationContext(), new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> playlists = new ArrayList<>();

                Playlist playlist = parser.fromJson(response.toString(), Playlist.class);
                MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                        .setMediaId(MediaUtils.MEDIA_ID_PLAYLIST + MediaUtils.SEPARATOR + playlist.getID())
                        .setTitle(playlist.getName())
                        .setSubtitle(playlist.getDescription() == null ? "" : playlist.getDescription())
                        .setIconUri(ResourceUtils.getUriToResource(getApplicationContext(), R.drawable.ic_playlist))
                        .build();

                playlists.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));

                Log.d(TAG, "Playlist: " + playlist.getName());
                result.sendResult(playlists);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> playlists = new ArrayList<>();

                for(int i=0; i<response.length(); i++) {
                    try {
                        Playlist playlist = parser.fromJson(response.getJSONObject(i).toString(), Playlist.class);
                        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                                .setMediaId(MediaUtils.MEDIA_ID_PLAYLIST + MediaUtils.SEPARATOR + playlist.getID())
                                .setTitle(playlist.getName())
                                .setSubtitle(playlist.getDescription() == null ? "" : playlist.getDescription())
                                .setIconUri(ResourceUtils.getUriToResource(getApplicationContext(), R.drawable.ic_playlist))
                                .build();

                        playlists.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(playlists);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve playlists");
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve playlists");
            }
        });
    }

    /**
     * Fetch the contents of a given Playlist.
     */
    private void getPlaylistContents(UUID id, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        restService.getPlaylistContents(getApplicationContext(), id, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();
                MediaElement element = parser.fromJson(response.toString(), MediaElement.class);
                elements.add(createMediaItemFromMediaElement(element));
                result.sendResult(elements);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);
                        elements.add(createMediaItemFromMediaElement(element));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(elements);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve playlist contents");
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve playlist contents");
            }
        });
    }

    /**
     * Fetch the contents of a given Media Folder.
     */
    private void getMediaElementContents(UUID id, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        restService.getMediaElementContents(getApplicationContext(), id, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();
                MediaElement element = parser.fromJson(response.toString(), MediaElement.class);
                elements.add(createMediaItemFromMediaElement(element));
                result.sendResult(elements);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);
                        elements.add(createMediaItemFromMediaElement(element));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(elements);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve media directory contents");
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve media directory contents");
            }
        });
    }

    /**
     * Fetch recently played media elements
     */
    private void getRecentlyPlayed(Byte type, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        restService.getRecentlyPlayed(getApplicationContext(), FETCH_LIMIT, type, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();
                MediaElement element = parser.fromJson(response.toString(), MediaElement.class);
                elements.add(createMediaItemFromMediaElement(element));
                result.sendResult(elements);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);
                        elements.add(createMediaItemFromMediaElement(element));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(elements);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve recently played list");
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve recently played list");
            }
        });
    }

    /**
     * Fetch recently added media elements
     */
    private void getRecentlyAdded(Byte type, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        restService.getRecentlyAdded(getApplicationContext(), FETCH_LIMIT, type, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();
                MediaElement element = parser.fromJson(response.toString(), MediaElement.class);
                elements.add(createMediaItemFromMediaElement(element));
                result.sendResult(elements);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);
                        elements.add(createMediaItemFromMediaElement(element));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(elements);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve recently added list");
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve recently added list");
            }
        });
    }

    /**
     * Fetch a list of collections.
     */
    private void getCollections(@NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {

        restService.getCollections(getApplicationContext(), new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                List<MediaBrowserCompat.MediaItem> collections = new ArrayList<>();
                String collection = response.toString();
                MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                        .setMediaId(MediaUtils.MEDIA_ID_COLLECTION + MediaUtils.SEPARATOR + collection)
                        .setTitle(collection)
                        .setIconUri(ResourceUtils.getUriToResource(getApplicationContext(), R.drawable.ic_movie))
                        .build();

                collections.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));

                result.sendResult(collections);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                List<MediaBrowserCompat.MediaItem> collections = new ArrayList<>();

                for(int i=0; i<response.length(); i++) {
                    try {
                        String collection = response.getString(i);
                        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                                .setMediaId(MediaUtils.MEDIA_ID_COLLECTION + MediaUtils.SEPARATOR + collection)
                                .setTitle(collection)
                                .setIconUri(ResourceUtils.getUriToResource(getApplicationContext(), R.drawable.ic_movie))
                                .build();

                        collections.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(collections);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve collection list");
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve collection list");
            }
        });
    }

    /**
     * Fetch a list of artists.
     */
    private void getArtists(@NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {

        restService.getArtists(getApplicationContext(), new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                List<MediaBrowserCompat.MediaItem> artists = new ArrayList<>();
                String artist = response.toString();
                MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                        .setMediaId(MediaUtils.MEDIA_ID_ARTIST + MediaUtils.SEPARATOR + artist)
                        .setTitle(artist)
                        .setIconUri(ResourceUtils.getUriToResource(getApplicationContext(), R.drawable.ic_artist))
                        .build();

                artists.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));

                result.sendResult(artists);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                List<MediaBrowserCompat.MediaItem> artists = new ArrayList<>();

                for(int i=0; i<response.length(); i++) {
                    try {
                        String artist = response.getString(i);
                        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                                .setMediaId(MediaUtils.MEDIA_ID_ARTIST + MediaUtils.SEPARATOR + artist)
                                .setTitle(artist)
                                .setIconUri(ResourceUtils.getUriToResource(getApplicationContext(), R.drawable.ic_artist))
                                .build();

                        artists.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(artists);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve artist list");
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve artist list");
            }
        });
    }

    /**
     * Fetch a list of album artists.
     */
    private void getAlbumArtists(@NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {

        restService.getAlbumArtists(getApplicationContext(), new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                List<MediaBrowserCompat.MediaItem> artists = new ArrayList<>();
                String artist = response.toString();
                MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                        .setMediaId(MediaUtils.MEDIA_ID_ALBUM_ARTIST + MediaUtils.SEPARATOR + artist)
                        .setTitle(artist)
                        .setIconUri(ResourceUtils.getUriToResource(getApplicationContext(), R.drawable.ic_artist))
                        .build();

                artists.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));

                result.sendResult(artists);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                List<MediaBrowserCompat.MediaItem> artists = new ArrayList<>();

                for(int i=0; i<response.length(); i++) {
                    try {
                        String artist = response.getString(i);
                        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                                .setMediaId(MediaUtils.MEDIA_ID_ALBUM_ARTIST + MediaUtils.SEPARATOR + artist)
                                .setTitle(artist)
                                .setIconUri(ResourceUtils.getUriToResource(getApplicationContext(), R.drawable.ic_artist))
                                .build();

                        artists.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(artists);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve album artist list");
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve album artist list");
            }
        });
    }

    /**
     * Fetch a list of albums.
     */
    private void getAlbums(@NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {

        restService.getAlbums(getApplicationContext(), new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                List<MediaBrowserCompat.MediaItem> albums = new ArrayList<>();
                String album = response.toString();
                MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                        .setMediaId(MediaUtils.MEDIA_ID_ALBUM + MediaUtils.SEPARATOR + album)
                        .setTitle(album)
                        .setIconUri(ResourceUtils.getUriToResource(getApplicationContext(), R.drawable.ic_album))
                        .build();

                albums.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));

                result.sendResult(albums);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                List<MediaBrowserCompat.MediaItem> albums = new ArrayList<>();

                for(int i=0; i<response.length(); i++) {
                    try {
                        String album = response.getString(i);
                        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                                .setMediaId(MediaUtils.MEDIA_ID_ALBUM + MediaUtils.SEPARATOR + album)
                                .setTitle(album)
                                .setIconUri(ResourceUtils.getUriToResource(getApplicationContext(), R.drawable.ic_album))
                                .build();

                        albums.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(albums);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve album list");
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve album list");
            }
        });
    }

    /**
     * Fetch a list of albums for artist.
     */
    private void getAlbumsByArtist(final String artist, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {

        restService.getAlbumsByArtist(getApplicationContext(), artist, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                List<MediaBrowserCompat.MediaItem> albums = new ArrayList<>();
                String album = response.toString();
                Bundle extras = new Bundle();

                // Save artist so we can retrieve media elements
                extras.putString(MediaUtils.MEDIA_ID_ARTIST, artist);

                MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                        .setMediaId(MediaUtils.MEDIA_ID_ARTIST_ALBUM + MediaUtils.SEPARATOR + artist + MediaUtils.SEPARATOR + album)
                        .setTitle(album)
                        .setIconUri(ResourceUtils.getUriToResource(getApplicationContext(), R.drawable.ic_album))
                        .setExtras(extras)
                        .build();

                albums.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));

                result.sendResult(albums);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                List<MediaBrowserCompat.MediaItem> albums = new ArrayList<>();

                for(int i=0; i<response.length(); i++) {
                    try {
                        String album = response.getString(i);
                        Bundle extras = new Bundle();

                        // Save artist so we can retrieve media elements
                        extras.putString(MediaUtils.MEDIA_ID_ARTIST, artist);

                        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                                .setMediaId(MediaUtils.MEDIA_ID_ARTIST_ALBUM + MediaUtils.SEPARATOR + artist + MediaUtils.SEPARATOR + album)
                                .setTitle(album)
                                .setIconUri(ResourceUtils.getUriToResource(getApplicationContext(), R.drawable.ic_album))
                                .build();

                        albums.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(albums);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve album list for artist: " + artist);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve album list for artist: " + artist);
            }
        });
    }

    /**
     * Fetch a list of albums for album artist.
     */
    private void getAlbumsByAlbumArtist(final String artist, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {

        restService.getAlbumsByAlbumArtist(getApplicationContext(), artist, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                List<MediaBrowserCompat.MediaItem> albums = new ArrayList<>();
                String album = response.toString();
                Bundle extras = new Bundle();

                // Save artist so we can retrieve media elements
                extras.putString(MediaUtils.MEDIA_ID_ARTIST, artist);

                MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                        .setMediaId(MediaUtils.MEDIA_ID_ALBUM_ARTIST_ALBUM + MediaUtils.SEPARATOR + artist + MediaUtils.SEPARATOR + album)
                        .setTitle(album)
                        .setIconUri(ResourceUtils.getUriToResource(getApplicationContext(), R.drawable.ic_album))
                        .setExtras(extras)
                        .build();

                albums.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));

                result.sendResult(albums);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                List<MediaBrowserCompat.MediaItem> albums = new ArrayList<>();

                for(int i=0; i<response.length(); i++) {
                    try {
                        String album = response.getString(i);
                        Bundle extras = new Bundle();

                        // Save artist so we can retrieve media elements
                        extras.putString(MediaUtils.MEDIA_ID_ARTIST, artist);

                        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                                .setMediaId(MediaUtils.MEDIA_ID_ALBUM_ARTIST_ALBUM + MediaUtils.SEPARATOR + artist + MediaUtils.SEPARATOR + album)
                                .setTitle(album)
                                .setIconUri(ResourceUtils.getUriToResource(getApplicationContext(), R.drawable.ic_album))
                                .build();

                        albums.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(albums);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve album list for album artist: " + artist);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve album list for album artist: " + artist);
            }
        });
    }

    /**
     * Fetch media elements for album and artist
     */
    private void getMediaElementForAlbumAndArtist(final String artist, final String album, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        restService.getMediaElementsByArtistAndAlbum(getApplicationContext(), artist, album, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();
                MediaElement element = parser.fromJson(response.toString(), MediaElement.class);
                elements.add(createMediaItemFromMediaElement(element));
                result.sendResult(elements);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);
                        elements.add(createMediaItemFromMediaElement(element));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(elements);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve media elements for album '" + album + "' and artist '" + artist + "'");
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve media elements for album '" + album + "' and artist '" + artist + "'");
            }
        });
    }

    /**
     * Fetch media elements for album and album artist
     */
    private void getMediaElementForAlbumAndAlbumArtist(final String artist, final String album, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        restService.getMediaElementsByAlbumArtistAndAlbum(getApplicationContext(), artist, album, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();
                MediaElement element = parser.fromJson(response.toString(), MediaElement.class);
                elements.add(createMediaItemFromMediaElement(element));
                result.sendResult(elements);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);
                        elements.add(createMediaItemFromMediaElement(element));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(elements);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve media elements for album '" + album + "' and album artist '" + artist + "'");
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve media elements for album '" + album + "' and album artist '" + artist + "'");
            }
        });
    }

    /**
     * Fetch media elements for an album
     */
    private void getMediaElementForAlbum(final String album, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        restService.getMediaElementsByAlbum(getApplicationContext(), album, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();
                MediaElement element = parser.fromJson(response.toString(), MediaElement.class);
                elements.add(createMediaItemFromMediaElement(element));
                result.sendResult(elements);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);
                        elements.add(createMediaItemFromMediaElement(element));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(elements);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve media elements for album '" + album + "'");
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve media elements for album '" + album + "'");
            }
        });
    }

    /**
     * Fetch media elements for a collection
     */
    private void getMediaElementForCollection(final String collection, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        restService.getMediaElementsByCollection(getApplicationContext(), collection, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();
                MediaElement element = parser.fromJson(response.toString(), MediaElement.class);
                elements.add(createMediaItemFromMediaElement(element));
                result.sendResult(elements);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                Gson parser = new Gson();
                List<MediaBrowserCompat.MediaItem> elements = new ArrayList<>();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);
                        elements.add(createMediaItemFromMediaElement(element));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                result.sendResult(elements);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Log.w(TAG, "Failed to retrieve media elements for collection '" + collection + "'");
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Log.w(TAG, "Failed to retrieve media elements for collection '" + collection + "'");
            }
        });
    }

    /**
     * Convert Media Folder to a MediaItem for media browser compatibility.
     */
    private MediaBrowserCompat.MediaItem createMediaItemFromMediaFolder(@NonNull MediaFolder folder) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(MediaUtils.MEDIA_ID_FOLDER + MediaUtils.SEPARATOR + String.valueOf(folder.getID()))
                .setTitle(folder.getName())
                .setSubtitle(String.format(getString(R.string.media_folder_subtitle), folder.getFiles(), folder.getFolders()))
                .setIconUri(Uri.parse(RESTService.getInstance().getAddress() + "/image/" + folder.getID() + "/cover" + "?folder=true"))
                .build();

        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    /**
     * Convert Media Element to a Media Item for Media Browser compatibility
     */
    private MediaBrowserCompat.MediaItem createMediaItemFromMediaElement(@NonNull MediaElement element) {
        switch(element.getType()) {
            case MediaElement.MediaElementType.DIRECTORY:
                MediaDescriptionCompat dirDescription = null;

                if(element.getDirectoryType() == MediaElement.DirectoryMediaType.NONE || element.getDirectoryType() == MediaElement.DirectoryMediaType.MIXED) {
                    dirDescription = new MediaDescriptionCompat.Builder()
                            .setMediaId(MediaUtils.MEDIA_ID_DIRECTORY + MediaUtils.SEPARATOR + String.valueOf(element.getID()))
                            .setTitle(element.getTitle())
                            .setIconUri(Uri.parse(RESTService.getInstance().getAddress() + "/image/" + element.getID() + "/cover"))
                            .build();
                } else if(element.getDirectoryType() == MediaElement.DirectoryMediaType.VIDEO) {
                    dirDescription = new MediaDescriptionCompat.Builder()
                            .setMediaId(MediaUtils.MEDIA_ID_DIRECTORY_VIDEO + MediaUtils.SEPARATOR + String.valueOf(element.getID()))
                            .setTitle(element.getTitle())
                            .setSubtitle(element.getCollection() == null ? "" : element.getCollection())
                            .setDescription(element.getDescription() == null ? "" : element.getDescription())
                            .setIconUri(Uri.parse(RESTService.getInstance().getAddress() + "/image/" + element.getID() + "/cover"))
                            .build();
                } else if(element.getDirectoryType() == MediaElement.DirectoryMediaType.AUDIO) {
                    dirDescription = new MediaDescriptionCompat.Builder()
                            .setMediaId(MediaUtils.MEDIA_ID_DIRECTORY_AUDIO + MediaUtils.SEPARATOR + String.valueOf(element.getID()))
                            .setTitle(element.getTitle())
                            .setSubtitle(element.getArtist() == null ? "" : element.getArtist())
                            .setDescription(element.getYear() == null ? "" : element.getYear().toString())
                            .setIconUri(Uri.parse(RESTService.getInstance().getAddress() + "/image/" + element.getID() + "/cover"))
                            .build();
                }

                return new MediaBrowserCompat.MediaItem(dirDescription,
                        MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);

            case MediaElement.MediaElementType.AUDIO:
                return new MediaBrowserCompat.MediaItem(MediaUtils.getMediaDescription(element),
                        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

            case MediaElement.MediaElementType.VIDEO:
                return new MediaBrowserCompat.MediaItem(MediaUtils.getMediaDescription(element),
                        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

            default:
                Log.i(TAG, "Unknown media type for media element with id " + element.getID());
                return null;
        }
    }

    /**
     * Build Audio menu media items
     */
    private List<MediaBrowserCompat.MediaItem> getAudioMenu() {
        List<MediaDescriptionCompat> descriptions = new ArrayList<>();
        List<MediaBrowserCompat.MediaItem> items = new ArrayList<>();

        if(!isOnline || !isConnected) {
            return null;
        }

        // Recently Added
        descriptions.add(new MediaDescriptionCompat.Builder()
                .setMediaId(MediaUtils.MEDIA_ID_RECENTLY_ADDED_AUDIO)
                .setTitle(getString(R.string.heading_recently_added))
                .build());

        // Recently Played
        descriptions.add(new MediaDescriptionCompat.Builder()
                .setMediaId(MediaUtils.MEDIA_ID_RECENTLY_PLAYED_AUDIO)
                .setTitle(getString(R.string.heading_recently_played))
                .build());

        // Playlists
        descriptions.add(new MediaDescriptionCompat.Builder()
                .setMediaId(MediaUtils.MEDIA_ID_PLAYLISTS)
                .setTitle(getString(R.string.heading_playlists))
                .build());

        // Add menu items
        for(MediaDescriptionCompat description : descriptions) {
            items.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
        }

        return items;
    }
}
