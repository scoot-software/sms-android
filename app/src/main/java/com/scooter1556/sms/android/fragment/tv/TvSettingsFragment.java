package com.scooter1556.sms.android.fragment.tv;

import android.content.Intent;
import android.os.Bundle;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.tv.TvAudioSettingsActivity;
import com.scooter1556.sms.android.activity.tv.TvConnectionActivity;
import com.scooter1556.sms.android.activity.tv.TvTranscodeSettingsActivity;
import com.scooter1556.sms.android.activity.tv.TvVideoSettingsActivity;
import com.scooter1556.sms.android.presenter.SettingsItemPresenter;

public class TvSettingsFragment extends BrowseSupportFragment {
    private static final String TAG = "TvSettingsFragment";

    private ArrayObjectAdapter rowsAdapter;
    private BackgroundManager backgroundManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onActivityCreated()");

        setupUI();
        setupRowAdapter();
        setupEventListeners();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");

        backgroundManager.setDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.default_background));
    }

    private void setupRowAdapter() {
        Log.d(TAG, "setupRowAdapter()");

        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        createRows();
        setAdapter(rowsAdapter);
    }

    private void createRows() {
        Log.d(TAG, "createRows()");

        SettingsItemPresenter settingsPresenter = new SettingsItemPresenter();
        ArrayObjectAdapter settingsRowAdapter = new ArrayObjectAdapter(settingsPresenter);
        settingsRowAdapter.add(getString(R.string.preferences_title_connections));
        settingsRowAdapter.add(getString(R.string.preferences_title_audio));
        settingsRowAdapter.add(getString(R.string.preferences_title_video));
        settingsRowAdapter.add(getString(R.string.preferences_title_transcode));
        rowsAdapter.add(new ListRow(settingsRowAdapter));
    }

    private void setupUI() {
        Log.d(TAG, "setupUI()");

        setTitle(getString(R.string.heading_settings));
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.primary));
        setHeadersState(HEADERS_DISABLED);

        // Setup background manager
        backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());

        // Get and set default background
        backgroundManager.setDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.default_background));
    }

    private void setupEventListeners() {
        Log.d(TAG, "setupEventListeners()");

        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {

        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
            Intent intent = null;

            if (item instanceof String) {
                if (((String) item).contains(getString(R.string.preferences_title_connections))) {
                    intent = new Intent(getActivity(), TvConnectionActivity.class);
                } else if (((String) item).contains(getString(R.string.preferences_title_audio))) {
                    intent = new Intent(getActivity(), TvAudioSettingsActivity.class);
                } else if (((String) item).contains(getString(R.string.preferences_title_video))) {
                    intent = new Intent(getActivity(), TvVideoSettingsActivity.class);
                } else if (((String) item).contains(getString(R.string.preferences_title_transcode))) {
                    intent = new Intent(getActivity(), TvTranscodeSettingsActivity.class);
                }
            }

            if (intent != null) {
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
                startActivity(intent, bundle);
            }
        }
    }
}
