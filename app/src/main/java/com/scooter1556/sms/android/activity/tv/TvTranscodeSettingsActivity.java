package com.scooter1556.sms.android.activity.tv;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.scooter1556.sms.android.R;

import java.util.List;

public class TvTranscodeSettingsActivity extends FragmentActivity {
    private static final String TAG = "TvTranscodeSettings";

    // Preferences
    private static SharedPreferences sharedPreferences;

    private static final int DIRECT_PLAY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve preferences if they exist
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (null == savedInstanceState) {
            GuidedStepSupportFragment.addAsRoot(this, new TranscodeSettingsFragment(), android.R.id.content);
        }
    }

    public static class TranscodeSettingsFragment extends GuidedStepSupportFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
            String title = getString(R.string.preferences_title_transcode);
            String breadcrumb = getString(R.string.preferences_title);
            Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_transcode_settings);
            return new GuidanceStylist.Guidance(title, null, breadcrumb, icon);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            Log.d(TAG, "onDestroy()");

            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            // Register preferences listener
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);

            // Set actions
            populateActions(actions);
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            Log.d(TAG, "onGuidedActionClicked()");

            if (action.getId() == DIRECT_PLAY) {
                boolean enabled = sharedPreferences.getBoolean("pref_direct_play", false);
                sharedPreferences.edit().putBoolean("pref_direct_play", !enabled).apply();
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d(TAG, "onSharedPreferencesChanged() -> Key=" + key);

            populateActions(getActions());
        }

        private void populateActions(List<GuidedAction> actions) {
            Log.d(TAG, "populateActions()");

            actions.clear();

            actions.add(new GuidedAction.Builder(getActivity())
                    .id(DIRECT_PLAY)
                    .title(getString(R.string.preferences_title_direct_play))
                    .description(sharedPreferences.getBoolean("pref_direct_play", false) ? getString(R.string.state_enabled) : getString(R.string.state_disabled))
                    .build());

            setActions(actions);
        }
    }
}
