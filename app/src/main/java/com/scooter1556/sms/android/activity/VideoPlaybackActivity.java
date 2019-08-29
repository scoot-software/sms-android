package com.scooter1556.sms.android.activity;

import android.content.ComponentName;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.mediarouter.app.MediaRouteButton;

import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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

public class VideoPlaybackActivity extends AppCompatActivity implements PlayerControlView.VisibilityListener, Player.EventListener, PlaybackManager.PlaybackListener {
    private static final String TAG = "VideoPlaybackActivity";

    // Saved instance state keys.
    private static final String KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters";

    private Player player;

    private PlayerView playerView;

    private boolean isShowingTrackSelectionDialog;

    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private TrackGroupArray lastSeenTrackGroupArray;

    private Toolbar toolbar;

    private MediaBrowserCompat mediaBrowser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_player);

        toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_video_player);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setHomeButtonEnabled(false);
        }

        playerView = findViewById(R.id.player);
        playerView.setControllerVisibilityListener(this);

        if (savedInstanceState != null) {
            trackSelectorParameters = savedInstanceState.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS);
        } else {
            trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder().build();
        }

        // Connect a media browser to keep media service alive
        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(getApplicationContext(), MediaService.class), new MediaBrowserCompat.ConnectionCallback(), null);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");

        PlaybackManager.getInstance().addListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(TAG, "onStart()");

        player = PlaybackManager.getInstance().getCurrentPlayer();
        trackSelector = PlaybackManager.getInstance().getCurrentTrackSelector();

        playerView.setPlayer(player);
        player.addListener(this);

        if(!mediaBrowser.isConnected()) {
            mediaBrowser.connect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "onPause()");

        if(player != null && player.getPlayWhenReady()) {
            player.setPlayWhenReady(false);
        }

        PlaybackManager.getInstance().removeListener(this);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu_video_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.track_selection_menu_item:
                if(!isShowingTrackSelectionDialog && TrackSelectionDialog.isContentAvailable(trackSelector)) {
                    isShowingTrackSelectionDialog = true;
                    TrackSelectionDialog trackSelectionDialog =
                            TrackSelectionDialog.createForTrackSelector(
                                    trackSelector,
                                    dismissedDialog -> isShowingTrackSelectionDialog = false);
                    trackSelectionDialog.show(getSupportFragmentManager(), null);
                }

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        // Show the controls on any key event.
        playerView.showController();

        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || playerView.dispatchMediaKeyEvent(event);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(TAG, "onPlaybackStateChanged() -> " + playWhenReady + ", " + playbackState);

        // Set screen state
        if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED || !playWhenReady) {
            playerView.setKeepScreenOn(false);
        } else {
            playerView.setKeepScreenOn(true);
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {
        showControls();
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        if (trackGroups != lastSeenTrackGroupArray) {
            lastSeenTrackGroupArray = trackGroups;
        }
    }

    private void updateTrackSelectorParameters() {
        if(trackSelector != null) {
            trackSelectorParameters = trackSelector.getParameters();
        }
    }

    @Override
    public void onVisibilityChange(int visibility) {
        if(visibility == View.VISIBLE) {
            if(getSupportActionBar() != null) {
                getSupportActionBar().show();
            }
        } else {
            if(getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }

            // Remove navigation buttons and status bar
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }


    }

    private void showControls() {
        if(getSupportActionBar() != null) {
            getSupportActionBar().show();
        }
    }

    @Override
    public void onQueuePositionChanged(int previousIndex, int newIndex) {
        // Do nothing...
    }

    @Override
    public void onPlayerChanged(Player player) {
        Log.d(TAG, "onPlayerChanged()");

        finish();
    }
}
