package com.scooter1556.sms.android.activity.tv;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import androidx.fragment.app.FragmentActivity;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.fragment.PlaybackControlsFragment;
import com.scooter1556.sms.android.provider.MediaBrowserProvider;
import com.scooter1556.sms.android.service.MediaService;

public class TvBaseActivity extends FragmentActivity {

    private static final String TAG = "TVBaseActivity";

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
    }
}
