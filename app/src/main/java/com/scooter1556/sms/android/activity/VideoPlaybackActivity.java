package com.scooter1556.sms.android.activity;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.dialog.TrackSelectionDialog;
import com.scooter1556.sms.android.playback.PlaybackManager;
import com.scooter1556.sms.android.service.MediaService;

import static com.google.android.exoplayer2.trackselection.MappingTrackSelector.*;

public class VideoPlaybackActivity extends AppCompatActivity implements View.OnClickListener, PlayerControlView.VisibilityListener, Player.EventListener {
    private static final String TAG = "VideoPlaybackActivity";

    // Saved instance state keys.
    private static final String KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters";

    private Player player;

    private PlayerView playerView;
    private LinearLayout controlsRootView;
    private Button trackSelectionButton;

    private boolean isShowingTrackSelectionDialog;

    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private TrackGroupArray lastSeenTrackGroupArray;

    private PowerManager.WakeLock wakeLock;

    private MediaBrowserCompat mediaBrowser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_player);

        controlsRootView = (LinearLayout) findViewById(R.id.controls_root);

        trackSelectionButton = findViewById(R.id.select_tracks_button);
        trackSelectionButton.setOnClickListener(this);

        playerView = findViewById(R.id.player);
        playerView.setControllerVisibilityListener(this);

        // Create Wake lock
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.scooter1556.sms.android: video_playback_wake_lock");

        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(getApplicationContext(), MediaService.class), connectionCallback, null);

        if (savedInstanceState != null) {
            trackSelectorParameters = savedInstanceState.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS);
        } else {
            trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder().build();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");

        if(!mediaBrowser.isConnected()) {
            mediaBrowser.connect();
        }

        player = PlaybackManager.getInstance().getCurrentPlayer();
        trackSelector = PlaybackManager.getInstance().getCurrentTrackSelector();

        playerView.setPlayer(player);
        player.addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        if(player != null) {
            player.stop(true);
            player.removeListener(this);
            player = null;
            trackSelector = null;
        }

        mediaBrowser.disconnect();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        updateTrackSelectorParameters();
        outState.putParcelable(KEY_TRACK_SELECTOR_PARAMETERS, trackSelectorParameters);
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        // Show the controls on any key event.
        playerView.showController();

        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || playerView.dispatchMediaKeyEvent(event);
    }

    @Override
    public void onClick(View view) {
        if(view == trackSelectionButton && !isShowingTrackSelectionDialog && TrackSelectionDialog.isContentAvailable(trackSelector)) {
            isShowingTrackSelectionDialog = true;
            TrackSelectionDialog trackSelectionDialog =
                    TrackSelectionDialog.createForTrackSelector(
                            trackSelector,
                            dismissedDialog -> isShowingTrackSelectionDialog = false);
            trackSelectionDialog.show(getSupportFragmentManager(), null);
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == Player.STATE_ENDED) {
            finish();
        }

        updateButtonVisibility();
    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {
        updateButtonVisibility();
        showControls();
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        updateButtonVisibility();
        if (trackGroups != lastSeenTrackGroupArray) {
            MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
            lastSeenTrackGroupArray = trackGroups;
        }
    }

    private void updateTrackSelectorParameters() {
        if(trackSelector != null) {
            trackSelectorParameters = trackSelector.getParameters();
        }
    }

    private void updateButtonVisibility() {
        trackSelectionButton.setEnabled(player != null && TrackSelectionDialog.isContentAvailable(trackSelector));
    }

    @Override
    public void onVisibilityChange(int visibility) {
        controlsRootView.setVisibility(visibility);
    }

    private void showControls() {
        controlsRootView.setVisibility(View.VISIBLE);
    }

    private final MediaBrowserCompat.ConnectionCallback connectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "onConnected()");
                }
            };
}
