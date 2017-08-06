package com.scooter1556.sms.android.activity.tv;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.playback.Playback;

public class TvVideoPlaybackActivity extends Activity {

    // Surface
    private SurfaceView surface;
    private SurfaceHolder holder;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tv_video_player);

        surface = (SurfaceView) findViewById(R.id.video_surface);
        holder = surface.getHolder();
    }

    public SurfaceHolder getSurfaceHolder() {
        return holder;
    }

    @Override
    public boolean onSearchRequested() {
        // TODO: Implement search feature
        Toast.makeText(this, getString(R.string.error_search_not_implemented), Toast.LENGTH_SHORT).show();
        return true;
    }
}