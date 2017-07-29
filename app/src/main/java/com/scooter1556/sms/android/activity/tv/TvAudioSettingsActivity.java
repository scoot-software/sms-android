package com.scooter1556.sms.android.activity.tv;

import android.app.Activity;
import android.app.FragmentManager;
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

public class TvAudioSettingsActivity extends Activity {
    // Preferences
    private static SharedPreferences sharedPreferences;

    private static final int AUDIO_QUALITY = 0;
    private static final int AUDIO_MULTICHANNEL = 1;

    private static final int AUDIO_QUALITY_CHECK_SET_ID = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve preferences if they exist
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (null == savedInstanceState) {
            GuidedStepFragment.addAsRoot(this, new AudioSettingsFragment(), android.R.id.content);
        }
    }

    public static class AudioSettingsFragment extends GuidedStepFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
            String title = getString(R.string.preferences_title_audio);
            String breadcrumb = getString(R.string.preferences_title);
            Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_audio_settings);
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
            FragmentManager fm = getFragmentManager();
            if (action.getId() == AUDIO_QUALITY) {
                GuidedStepFragment.add(fm, new AudioQualitySettingsFragment());
            } else if (action.getId() == AUDIO_MULTICHANNEL) {
                boolean enabled = sharedPreferences.getBoolean("pref_audio_multichannel", false);
                sharedPreferences.edit().putBoolean("pref_audio_multichannel", !enabled).apply();
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            setActions(getActions());
        }

        public List<GuidedAction> getActions() {
            List<GuidedAction> actions = new ArrayList<>();

            actions.add(new GuidedAction.Builder(getActivity())
                    .id(AUDIO_QUALITY)
                    .title(getString(R.string.preferences_title_audio_quality))
                    .description(sharedPreferences.getString("pref_audio_quality_name", getString(R.string.preferences_default_audio_quality_value)))
                    .build());
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(AUDIO_MULTICHANNEL)
                    .title(getString(R.string.preferences_title_audio_multichannel))
                    .build());

            actions.get(AUDIO_MULTICHANNEL).setChecked(sharedPreferences.getBoolean("pref_audio_multichannel", false));

            return actions;
        }
    }

    public static class AudioQualitySettingsFragment extends GuidedStepFragment {
        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.preferences_title_audio_quality);
            String breadcrumb = getString(R.string.preferences_title_audio);
            String description = getString(R.string.preferences_summary_audio_quality);
            Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_audio_settings);
            return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            // Get saved preference
            int quality = Integer.parseInt(sharedPreferences.getString("pref_audio_quality", getString(R.string.preferences_default_audio_quality_value)));
            String[] audioQualityValues = getResources().getStringArray(R.array.preferences_audio_quality_values);
            String[] audioQualityNames = getResources().getStringArray(R.array.preferences_audio_quality_names);

            // Add action for each quality option
            for (int i = 0; i < audioQualityValues.length; i++) {
                GuidedAction guidedAction = new GuidedAction.Builder(getActivity())
                        .id(Long.parseLong(audioQualityValues[i]))
                        .title(audioQualityNames[i])
                        .checkSetId(AUDIO_QUALITY_CHECK_SET_ID)
                        .build();
                guidedAction.setChecked(Long.parseLong(audioQualityValues[i]) == quality);
                actions.add(guidedAction);
            }
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            // Update audio quality preference
            sharedPreferences.edit()
                    .putString("pref_audio_quality", String.format("%d", action.getId()))
                    .putString("pref_audio_quality_name", action.getTitle().toString())
                    .apply();
        }
    }
}
