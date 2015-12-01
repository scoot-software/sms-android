package com.scooter1556.sms.android.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.scooter1556.sms.android.R;

/**
 * Settings
 *
 * Created by scott2ware.
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
