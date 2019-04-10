package com.scooter1556.sms.android.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.playback.PlaybackManager;
import com.scooter1556.sms.android.utils.TrackSelectionUtils;

public class VideoPlaybackActivity extends AppCompatActivity implements View.OnClickListener, PlayerControlView.VisibilityListener {
    private static final String TAG = "VideoPlaybackActivity";

    public static final String USER_AGENT = "SMSAndroidPlayer";

    static final int CONTROLLER_TIMEOUT = 4000;

    private PlayerView playerView;
    private LinearLayout settingsRootView;

    private PowerManager.WakeLock wakeLock;

    private DefaultTrackSelector trackSelector;
    private TrackSelectionUtils trackSelectionUtils;
    private TrackGroupArray lastSeenTrackGroupArray;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_player);

        settingsRootView = (LinearLayout) findViewById(R.id.settingsRoot);

        playerView = findViewById(R.id.player);
        playerView.setControllerVisibilityListener(this);

        // Create Wake lock
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.scooter1556.sms.android: video_playback_wake_lock");
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(TAG, "onStart()");

        playerView.setPlayer(PlaybackManager.getInstance().getCurrentPlayer());
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

        PlaybackManager.getInstance().getCurrentPlayer().stop(true);
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
        if (view.getParent() == settingsRootView) {
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

            if (mappedTrackInfo != null) {
                trackSelectionUtils.showSelectionDialog(this, trackSelector.getCurrentMappedTrackInfo(), (int) view.getTag());
            }
        }
    }

    private void updateButtonVisibilities() {
        settingsRootView.removeAllViews();

        Player player = PlaybackManager.getInstance().getCurrentPlayer();

        if (PlaybackManager.getInstance() == null) {
            return;
        }

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

        if (mappedTrackInfo == null) {
            return;
        }

        for (int i = 0; i < mappedTrackInfo.length; i++) {
            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);

            if (trackGroups.length != 0) {
                ImageButton button = new ImageButton(this);
                switch (player.getRendererType(i)) {

                    case C.TRACK_TYPE_AUDIO:
                        button.setImageResource(R.drawable.ic_surround_sound_white_36dp);
                        break;
                    case C.TRACK_TYPE_VIDEO:
                        button.setImageResource(R.drawable.ic_settings_white_36dp);
                        break;
                    case C.TRACK_TYPE_TEXT:
                        button.setImageResource(R.drawable.ic_subtitles_white_36dp);
                        break;
                    default:
                        continue;
                }

                button.setTag(i);
                button.setBackgroundColor(Color.TRANSPARENT);
                button.setOnClickListener(this);
                settingsRootView.addView(button, settingsRootView.getChildCount() - 1);
            }
        }
    }

    @Override
    public void onVisibilityChange(int visibility) {

    }
}
