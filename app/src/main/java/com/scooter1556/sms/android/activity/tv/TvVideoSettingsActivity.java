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

public class TvVideoSettingsActivity extends Activity {
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
            GuidedStepFragment.addAsRoot(this, new VideoSettingsFragment(), android.R.id.content);
        }
    }

    public static class VideoSettingsFragment extends GuidedStepFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
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
            setActions(getActions());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            FragmentManager fm = getFragmentManager();
            if (action.getId() == VIDEO_QUALITY) {
                GuidedStepFragment.add(fm, new VideoQualitySettingsFragment());
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            setActions(getActions());
        }

        public List<GuidedAction> getActions() {
            List<GuidedAction> actions = new ArrayList<>();

            actions.add(new GuidedAction.Builder(getActivity())
                    .id(VIDEO_QUALITY)
                    .title(getString(R.string.preferences_title_video_quality))
                    .description(sharedPreferences.getString("pref_video_quality_name", getString(R.string.preferences_default_video_quality_value)))
                    .build());

            return actions;
        }
    }

    public static class VideoQualitySettingsFragment extends GuidedStepFragment {
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
                GuidedAction guidedAction = new GuidedAction.Builder()
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
