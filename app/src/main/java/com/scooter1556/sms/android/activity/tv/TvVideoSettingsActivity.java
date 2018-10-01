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

import com.scooter1556.sms.android.R;

import java.util.List;

public class TvVideoSettingsActivity extends FragmentActivity {
    private static final String TAG = "TvVideoSettingsActivity";

    // Preferences
    private static SharedPreferences sharedPreferences;

    private static final int VIDEO_QUALITY = 0;

    private static final int VIDEO_QUALITY_CHECK_SET_ID = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve preferences if they exist
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (null == savedInstanceState) {
            GuidedStepSupportFragment.addAsRoot(this, new VideoSettingsFragment(), android.R.id.content);
        }
    }

    public static class VideoSettingsFragment extends GuidedStepSupportFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
            String title = getString(R.string.preferences_title_video);
            String breadcrumb = getString(R.string.preferences_title);
            Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_video_settings);
            return new GuidanceStylist.Guidance(title, null, breadcrumb, icon);
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
            if (action.getId() == VIDEO_QUALITY) {
                GuidedStepSupportFragment.add(getFragmentManager(), new VideoQualitySettingsFragment());
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            populateActions(getActions());
        }

        public void populateActions(List<GuidedAction> actions) {
            actions.clear();

            String[] videoQualityNames = getResources().getStringArray(R.array.preferences_video_quality_names);
            int quality = Integer.parseInt(sharedPreferences.getString("pref_video_quality", getResources().getString(R.string.preferences_default_video_quality_value)));

            // Check quality
            if(quality >= getResources().getStringArray(R.array.preferences_video_quality_values).length) {
                quality = Integer.parseInt(getResources().getString(R.string.preferences_default_video_quality_value));
            }

            actions.add(new GuidedAction.Builder(getActivity())
                    .id(VIDEO_QUALITY)
                    .title(getString(R.string.preferences_title_video_quality))
                    .description(videoQualityNames[quality])
                    .build());

            setActions(actions);
        }
    }

    public static class VideoQualitySettingsFragment extends GuidedStepSupportFragment {
        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.preferences_title_video_quality);
            String breadcrumb = getString(R.string.preferences_title_video);
            String description = getString(R.string.preferences_summary_video_quality);
            Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_video_settings);
            return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            // Get saved preference
            long quality = Long.parseLong(sharedPreferences.getString("pref_video_quality", getString(R.string.preferences_default_video_quality_value)));
            String[] videoQualityValues = getResources().getStringArray(R.array.preferences_video_quality_values);
            String[] videoQualityNames = getResources().getStringArray(R.array.preferences_video_quality_names);

            // Add action for each quality option
            for (int i = 0; i < videoQualityValues.length; i++) {
                GuidedAction guidedAction = new GuidedAction.Builder(getActivity())
                        .id(Long.parseLong(videoQualityValues[i]))
                        .title(videoQualityNames[i])
                        .checkSetId(VIDEO_QUALITY_CHECK_SET_ID)
                        .build();
                guidedAction.setChecked(Long.parseLong(videoQualityValues[i]) == quality);
                actions.add(guidedAction);
            }
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            // Update audio quality preference
            sharedPreferences.edit()
                    .putString("pref_video_quality", String.format("%d", action.getId()))
                    .putString("pref_video_quality_name", action.getTitle().toString())
                    .apply();
        }
    }
}
