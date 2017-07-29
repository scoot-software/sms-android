package com.scooter1556.sms.android.activity.tv;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;

import com.scooter1556.sms.android.R;

import java.util.ArrayList;
import java.util.List;

public class TvTranscodeSettingsActivity extends Activity {
    // Preferences
    private static SharedPreferences sharedPreferences;

    private static final int DIRECT_PLAY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve preferences if they exist
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (null == savedInstanceState) {
            GuidedStepFragment.addAsRoot(this, new TranscodeSettingsFragment(), android.R.id.content);
        }
    }

    public static class TranscodeSettingsFragment extends GuidedStepFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
            String title = getString(R.string.preferences_title_transcode);
            String breadcrumb = getString(R.string.preferences_title);
            Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_transcode_settings);
            return new GuidanceStylist.Guidance(title, null, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            // Register preferences listener
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);

            // Set actions
            setActions(getActions());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == DIRECT_PLAY) {
                boolean enabled = sharedPreferences.getBoolean("pref_direct_play", false);
                sharedPreferences.edit().putBoolean("pref_direct_play", !enabled).apply();
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            setActions(getActions());
        }

        public List<GuidedAction> getActions() {
            List<GuidedAction> actions = new ArrayList<>();

            actions.add(new GuidedAction.Builder(getActivity())
                    .id(DIRECT_PLAY)
                    .title(getString(R.string.preferences_title_direct_play))
                    .build());

            actions.get(DIRECT_PLAY).setChecked(sharedPreferences.getBoolean("pref_direct_play", false));

            return actions;
        }
    }
}
