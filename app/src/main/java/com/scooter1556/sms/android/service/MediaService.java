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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.support.v4.media.MediaBrowserCompat;
import androidx.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.gson.Gson;
import com.loopj.android.http.BlackholeHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.SMS;
import com.scooter1556.sms.android.activity.NowPlayingActivity;
import com.scooter1556.sms.android.domain.ClientProfile;
import com.scooter1556.sms.android.domain.Playlist;
import com.scooter1556.sms.android.playback.PlaybackManager;
import com.scooter1556.sms.android.utils.AutoUtils;
import com.scooter1556.sms.android.utils.CodecUtils;
import com.scooter1556.sms.android.utils.NetworkUtils;
import com.scooter1556.sms.android.utils.ResourceUtils;
import com.scooter1556.sms.android.database.ConnectionDatabase;
import com.scooter1556.sms.android.domain.Connection;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.domain.MediaFolder;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.utils.TVUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MediaService extends MediaBrowserServiceCompat
                          implements PlayerNotificationManager.NotificationListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "MediaService";

    // Android
    static final int FORMAT = SMS.Format.HLS;
    static final Integer[] SUPPORTED_FORMATS = {SMS.Format.HLS};
    static final int MAX_SAMPLE_RATE = 48000;

    // Chromecast channels
    public static final String CC_CONFIG_CHANNEL = "urn:x-cast:com.scooter1556.sms.config";

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

    // Number of media elements to fetch when populating lists
    public static final int FETCH_LIMIT = 50;

    private PlaybackManager playbackManager;

    private MediaSessionCompat mediaSession;
    private Bundle mediaSessionExtras;

    boolean isOnline = false;
    private boolean isConnected = false;
    private boolean isConnectedToCar;
    private BroadcastReceiver carConnectionReceiver;

    // Client Profile
    ClientProfile clientProfile = null;

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

        // Populate default client profile
        updateClientProfile();

        // Start a new SMS session
        SessionService.getInstance().newSession(getApplicationContext(), null, clientProfile);

        // Start a new Media Session
        mediaSession = new MediaSessionCompat(this, MediaService.class.getSimpleName());
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);

        Context context = getApplicationContext();
        Intent intent = new Intent(context, NowPlayingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 99, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mediaSession.setSessionActivity(pendingIntent);

        mediaSessionExtras = new Bundle();
        AutoUtils.setSlotReservationFlags(mediaSessionExtras, true, true, true);
        mediaSession.setExtras(mediaSessionExtras);

        setSessionToken(mediaSession.getSessionToken());

        registerCarConnectionReceiver();

        // Register connectivity receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        this.registerReceiver(connectivityChangeReceiver, intentFilter);

        // Register shared preferences listener
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        // Initialise playback manager
        playbackManager = PlaybackManager.getInstance();
        playbackManager.initialise(getApplicationContext(), mediaSession, this);

        // Enable Cast
        if(!TVUtils.isTvUiMode(context)) {
            playbackManager.initialiseCast(CastContext.getSharedInstance(this));
        }
    }

    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");

        if (startIntent != null) {
            String action = startIntent.getAction();
            String command = startIntent.getStringExtra(CMD_NAME);

            // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
            MediaButtonReceiver.handleIntent(mediaSession, startIntent);
        }

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

        // Release PlaybackManager
        if(playbackManager != null) {
            playbackManager.release();
            playbackManager = null;
        }

        // End SMS session
        SessionService.getInstance().endCurrentSession();

        mediaSession.release();

        // Unregister shared preferences listener
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

        // Unregister receiver
        this.unregisterReceiver(connectivityChangeReceiver);
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "Preference Changed: " + key);

        // Update client profile if necessary
        if(clientProfile == null) {
            return;
        }

        switch(key) {
            case "pref_video_quality": case "pref_audio_quality": case "pref_direct_play":
                updateClientProfile();
                break;

            case "pref_cast_video_quality": case "pref_cast_audio_quality":
                if(playbackManager.isCastSessionAvailable()) {
                    updateCastProfile(playbackManager.getCastSession(), playbackManager.getCastSession().getSessionId());
                }
                break;

            default:
                return;
        }

        // Update client profile
        RESTService.getInstance().updateClientProfile(getApplicationContext(), SessionService.getInstance().getSessionId(), clientProfile, new BlackholeHttpResponseHandler());
    }

    @Override
    public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
        Log.d(TAG, "onNotificationPosted() > id=" + notificationId + ", ongoing=" + ongoing);

        if(ongoing) {
            startService(new Intent(getApplicationContext(), MediaService.class));
            startForeground(notificationId, notification);
        } else {
            stopForeground(false);
        }
    }

    @Override
    public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
        Log.d(TAG, "onNotificationCancelled()");

        stopForeground(true);
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

            if (service != null) {
                service.stopSelf();
            }
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
        restService.getPlaylistContents(getApplicationContext(), id, false, new JsonHttpResponseHandler() {

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

    private void updateClientProfile() {
        // Initialise new client profile instance
        clientProfile = new ClientProfile();

        // Get settings
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        // Get quality
        int audioQuality = Integer.parseInt(settings.getString("pref_audio_quality", "0"));
        int videoQuality = Integer.parseInt(settings.getString("pref_video_quality", "0"));

        // Determine what device we are running on
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            clientProfile.setClient(SMS.Client.ANDROID_TV);
        } else {
            clientProfile.setClient(SMS.Client.ANDROID);
        }

        clientProfile.setFormats(SUPPORTED_FORMATS);
        clientProfile.setCodecs(CodecUtils.getSupportedCodecs(getApplicationContext()));
        clientProfile.setMchCodecs(CodecUtils.getSupportedMchAudioCodecs(getApplicationContext()));
        clientProfile.setAudioQuality(audioQuality);
        clientProfile.setVideoQuality(videoQuality);
        clientProfile.setFormat(FORMAT);
        clientProfile.setMaxSampleRate(MAX_SAMPLE_RATE);
        clientProfile.setDirectPlay(settings.getBoolean("pref_direct_play", false));
    }

    private void updateCastProfile(CastSession session, String sessionId) {
        // Get settings
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        // Get quality
        String audioQuality = settings.getString("pref_cast_audio_quality", "0");
        String videoQuality = settings.getString("pref_cast_video_quality", "0");

        // Generate and send JSON message containing settings
        JSONObject message = new JSONObject();
        try {
            message.put("videoQuality", videoQuality);
            message.put("audioQuality", audioQuality);
            message.put("sessionId", sessionId);
            session.sendMessage(CC_CONFIG_CHANNEL, message.toString());
        } catch (JSONException e) {
            Log.d(TAG, "Failed to send settings to Chromecast receiver.", e);
        }

    }
}
