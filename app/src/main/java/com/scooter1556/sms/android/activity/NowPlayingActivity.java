package com.scooter1556.sms.android.activity;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import com.scooter1556.sms.android.activity.tv.TvHomeActivity;
import com.scooter1556.sms.android.utils.TVUtils;

/**
 * The activity for the Now Playing Card Pending Intent.
 *
 * This activity determines which activity to launch based on the current UI mode.
 */
public class NowPlayingActivity extends Activity {

    private static final String TAG = "NowPlayingActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent newIntent;

        if (TVUtils.isTvUiMode(this)) {
            Log.d(TAG, "Running on a TV Device");
            newIntent = new Intent(this, TvHomeActivity.class);
        } else {
            Log.d(TAG, "Running on a non-TV Device");
            newIntent = new Intent(this, HomeActivity.class);
        }

        startActivity(newIntent);
        finish();
    }
}