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
package com.scooter1556.sms.android.activity;


import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.fragment.PlaybackControlsFragment;
import com.scooter1556.sms.android.provider.MediaBrowserProvider;
import com.scooter1556.sms.android.service.MediaService;

/**
 * Base activity for activities that need to show a playback control fragment when media is playing.
 */
public abstract class BaseActivity extends ActionBarCastActivity implements MediaBrowserProvider {

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
                Intent connectionsIntent = new Intent(this, ConnectionActivity.class);
                startActivityForResult(connectionsIntent, RESULT_CODE_SETTINGS);
            }
        }

        // Connect a media browser just to get the media session token.
        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(getApplicationContext(), MediaService.class), connectionCallback, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy()");

        mediaBrowser.disconnect();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart()");

        controlsFragment = (PlaybackControlsFragment) getFragmentManager().findFragmentById(R.id.fragment_playback_controls);

        if (controlsFragment == null) {
            throw new IllegalStateException("Missing fragment with id 'controls'. Cannot continue.");
        }

        if(mediaBrowser.isConnected()) {
            if (MediaControllerCompat.getMediaController(this) != null) {
                MediaControllerCompat.getMediaController(this).registerCallback(mediaControllerCallback);
            }

            updateControls();
        } else {
            mediaBrowser.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop()");

        if (MediaControllerCompat.getMediaController(this) != null) {
            MediaControllerCompat.getMediaController(this).unregisterCallback(mediaControllerCallback);
        }
    }

    @Override
    public MediaBrowserCompat getMediaBrowser() {
        return mediaBrowser;
    }

    protected void onMediaControllerConnected() {
        // Empty implementation, Can be overridden by clients.
    }

    protected void showPlaybackControls() {
        Log.d(TAG, "showPlaybackControls()");

        getFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom,
                        R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom)
                .show(controlsFragment)
                .commit();
    }

    protected void hidePlaybackControls() {
        Log.d(TAG, "hidePlaybackControls()");

        getFragmentManager().beginTransaction()
                .hide(controlsFragment)
                .commit();
    }

    /**
     * Check if the Media Session is active
     */
    protected void updateControls() {
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);

        if (mediaController == null || mediaController.getMetadata() == null || mediaController.getPlaybackState() == null) {
            hidePlaybackControls();
        }

        Log.d(TAG, "shouldShowControls() -> PlaybackState: " + mediaController.getPlaybackState().getState());

        switch(mediaController.getPlaybackState().getState()) {
            case PlaybackStateCompat.STATE_ERROR:
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
                hidePlaybackControls();
                break;
            default:
                showPlaybackControls();
        }
    }

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        Log.d(TAG, "connectToSession()");

        MediaControllerCompat mediaController = new MediaControllerCompat(this, token);
        MediaControllerCompat.setMediaController(this, mediaController);
        mediaController.registerCallback(mediaControllerCallback);

        updateControls();

        if (controlsFragment != null) {
            controlsFragment.onConnected();
        }

        onMediaControllerConnected();
    }

    // Callback that ensures that we are showing the controls
    private final MediaControllerCompat.Callback mediaControllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
                    Log.d(TAG, "onPlaybackStateChanged() -> State: " + state);

                    updateControls();
                }

                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    Log.d(TAG, "onMetadataChanged()");

                    updateControls();
                }
            };

    private final MediaBrowserCompat.ConnectionCallback connectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "onConnected()");

                    try {
                        connectToSession(mediaBrowser.getSessionToken());
                    } catch (RemoteException e) {
                        Log.e(TAG, "Could not connect media controller", e);

                        hidePlaybackControls();
                    }
                }
            };

}
