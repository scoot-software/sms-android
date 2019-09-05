package com.scooter1556.sms.android.activity.tv;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.util.Log;

import com.scooter1556.sms.android.R;

import java.util.List;

public class TvAudioSettingsActivity extends FragmentActivity {
    private static final String TAG = "TvAudioSettings";

    // Preferences
    private static SharedPreferences sharedPreferences;

    private static final int AUDIO_QUALITY = 0;
    private static final int AUDIO_MULTICHANNEL = 1;
    private static final int REPLAYGAIN_MODE = 2;

    private static final int AUDIO_QUALITY_CHECK_SET_ID = 10;
    private static final int REPLAYGAIN_CHECK_SET_ID = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve preferences if they exist
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (null == savedInstanceState) {
            GuidedStepSupportFragment.addAsRoot(this, new AudioSettingsFragment(), android.R.id.content);
        }
    }

    public static class AudioSettingsFragment extends GuidedStepSupportFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
            String title = getString(R.string.preferences_title_audio);
            String breadcrumb = getString(R.string.preferences_title);
            Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_audio_settings);
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
            Log.d(TAG, "onGuidedActionClicked(" + action.getId() + ")");

            if (action.getId() == AUDIO_QUALITY) {
                GuidedStepSupportFragment.add(getFragmentManager(), new AudioQualitySettingsFragment());
            } else if (action.getId() == AUDIO_MULTICHANNEL) {
                boolean enabled = sharedPreferences.getBoolean("pref_audio_multichannel", false);
                sharedPreferences.edit().putBoolean("pref_audio_multichannel", !enabled).apply();
            } else if (action.getId() == REPLAYGAIN_MODE) {
                GuidedStepSupportFragment.add(getFragmentManager(), new ReplaygainSettingsFragment());
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d(TAG, "onSharedPreferenceChanged(" + key + ")");

            populateActions(getActions());
        }

        public void populateActions(List<GuidedAction> actions) {
            Log.d(TAG, "populateActions()");

            // Audio Quality
            String[] audioQualityNames = getResources().getStringArray(R.array.preferences_audio_quality_names);
            int quality = Integer.parseInt(sharedPreferences.getString("pref_audio_quality", getString(R.string.preferences_default_audio_quality_value)));

            // Check quality
            if(quality >= getResources().getStringArray(R.array.preferences_audio_quality_values).length) {
                quality = Integer.parseInt(getString(R.string.preferences_default_audio_quality_value));
            }

            // Replaygain
            String[] replaygainModeNames = getResources().getStringArray(R.array.preferences_replaygain_names);
            String[] replaygainModeValues = getResources().getStringArray(R.array.preferences_replaygain_values);
            String replaygainMode = sharedPreferences.getString("pref_replaygain", getString(R.string.preferences_default_replaygain_value));

            // Get replaygain mode name
            String replaygainModeName = "";

            for(int i=0; i<replaygainModeValues.length; i++) {
                if(replaygainModeValues[i].equals(replaygainMode)) {
                    replaygainModeName = replaygainModeNames[i];
                    break;
                }
            }

            actions.clear();

            actions.add(new GuidedAction.Builder(getActivity())
                    .id(AUDIO_QUALITY)
                    .title(getString(R.string.preferences_title_audio_quality))
                    .description(audioQualityNames[quality])
                    .build());

            actions.add(new GuidedAction.Builder(getActivity())
                    .id(REPLAYGAIN_MODE)
                    .title(getString(R.string.preferences_title_replaygain))
                    .description(replaygainModeName)
                    .build());

            actions.add(new GuidedAction.Builder(getActivity())
                    .id(AUDIO_MULTICHANNEL)
                    .title(getString(R.string.preferences_title_audio_multichannel))
                    .description(sharedPreferences.getBoolean("pref_audio_multichannel", false) ? getString(R.string.state_enabled) : getString(R.string.state_disabled))
                    .build());

            setActions(actions);
        }
    }

    public static class AudioQualitySettingsFragment extends GuidedStepSupportFragment {
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

    public static class ReplaygainSettingsFragment extends GuidedStepSupportFragment {
        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.preferences_title_replaygain);
            String breadcrumb = getString(R.string.preferences_title_audio);
            String description = getString(R.string.preferences_summary_replaygain);
            Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_audio_settings);
            return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            // Get saved preference
            String replaygainMode = sharedPreferences.getString("pref_replaygain", getString(R.string.preferences_default_replaygain_value));
            String[] replaygainModeValues = getResources().getStringArray(R.array.preferences_replaygain_values);
            String[] replaygainModeNames = getResources().getStringArray(R.array.preferences_replaygain_names);

            // Add action for each replaygain option
            for (int i = 0; i < replaygainModeValues.length; i++) {
                GuidedAction guidedAction = new GuidedAction.Builder(getActivity())
                        .id(Long.parseLong(replaygainModeValues[i]))
                        .title(replaygainModeNames[i])
                        .checkSetId(REPLAYGAIN_CHECK_SET_ID)
                        .build();
                guidedAction.setChecked(replaygainModeValues[i].equals(replaygainMode));
                actions.add(guidedAction);
            }
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            // Update replaygain preference
            sharedPreferences.edit()
                    .putString("pref_replaygain", String.format("%d", action.getId()))
                    .putString("pref_replaygain_name", action.getTitle().toString())
                    .apply();
        }
    }
}
