package com.scooter1556.sms.android.activity.tv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

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
