package com.scooter1556.sms.android.activity.tv;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.scooter1556.sms.android.activity.ActionBarCastActivity;
import com.scooter1556.sms.android.fragment.PlaybackControlsFragment;
import com.scooter1556.sms.android.provider.MediaBrowserProvider;
import com.scooter1556.sms.android.service.MediaService;

public class TvBaseActivity extends Activity implements MediaBrowserProvider {

    private static final String TAG = "BaseActivity";

    public static final int RESULT_CODE_SETTINGS = 101;
    public static final int RESULT_CODE_CONNECTIONS = 102;
    public static final int RESULT_CODE_BROWSE = 103;

    private MediaBrowserCompat mediaBrowser;
    private PlaybackControlsFragment controlsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");

        // Retrieve preferences if they exist
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if(savedInstanceState == null) {
            // Check connection
            long id = sharedPreferences.getLong("Connection", -1);

            if(id < 0) {
                // Open connections activity
                Intent intent = new Intent(getApplicationContext(), TvConnectionActivity.class);
                startActivity(intent);
            }
        }

        // Connect a media browser just to get the media session token.
        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(getApplicationContext(), MediaService.class), connectionCallback, null);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart()");

        mediaBrowser.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop()");

        mediaBrowser.disconnect();
    }

    @Override
    public MediaBrowserCompat getMediaBrowser() {
        return mediaBrowser;
    }

    protected void onMediaControllerConnected() {
        // Empty implementation, Can be overridden by clients.
    }

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        MediaControllerCompat mediaController = new MediaControllerCompat(this, token);
        MediaControllerCompat.setMediaController(this, mediaController);
        mediaController.registerCallback(mediaControllerCallback);

        onMediaControllerConnected();
    }

    // Callback that ensures that we are showing the controls
    private final MediaControllerCompat.Callback mediaControllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
                }

                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                }
            };

    private final MediaBrowserCompat.ConnectionCallback connectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "onConnected()");
                }
            };

}
