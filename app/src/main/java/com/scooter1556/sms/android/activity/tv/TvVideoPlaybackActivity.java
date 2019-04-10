package com.scooter1556.sms.android.activity.tv;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.widget.Toast;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.fragment.tv.TvVideoPlayerFragment;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.PlaybackFragment;

public class TvVideoPlaybackActivity extends FragmentActivity {
    private static final String TAG = "TvVideoPlaybackActivity";

    private static final String PLAYER_FRAGMENT_TAG = "TV_VIDEO_PLAYER_FRAGMENT";

    private TvVideoPlayerFragment playerFragment;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tv_video_player);

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(PLAYER_FRAGMENT_TAG);
        if (fragment instanceof TvVideoPlayerFragment) {
            playerFragment = (TvVideoPlayerFragment) fragment;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        finish();
    }

    @Override
    public boolean onSearchRequested() {
        // TODO: Implement search feature
        Toast.makeText(this, getString(R.string.error_search_not_implemented), Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BUTTON_R1) {
            playerFragment.skipToNext();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_L1) {
            playerFragment.skipToPrevious();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
            playerFragment.rewind();
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
            playerFragment.fastForward();
        }

        return super.onKeyDown(keyCode, event);
    }
}