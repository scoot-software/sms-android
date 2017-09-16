package com.scooter1556.sms.android.fragment.tv;

import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.tv.TvMediaBrowserActivity;
import com.scooter1556.sms.android.activity.tv.TvMusicActivity;
import com.scooter1556.sms.android.activity.tv.TvSettingsActivity;
import com.scooter1556.sms.android.activity.tv.TvVideoActivity;
import com.scooter1556.sms.android.presenter.SettingsItemPresenter;

public class TvHomeFragment extends BrowseFragment {
    private static final String TAG = "TvHomeFragment";

    private ArrayObjectAdapter rowsAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupUI();
        setupRowAdapter();
        setupEventListeners();
    }

    private void setupRowAdapter() {
        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        createRows();
        setAdapter(rowsAdapter);
    }

    private void createRows() {
        SettingsItemPresenter settingsPresenter = new SettingsItemPresenter();
        ArrayObjectAdapter rowAdapter = new ArrayObjectAdapter(settingsPresenter);
        rowAdapter.add(getString(R.string.heading_media_browser));
        rowAdapter.add(getString(R.string.heading_music));
        rowAdapter.add(getString(R.string.heading_video));
        rowAdapter.add(getString(R.string.heading_settings));
        rowsAdapter.add(new ListRow(rowAdapter));
    }

    private void setupUI() {
        setTitle(getString(R.string.app_name));
        setBadgeDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.app_icon));
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.primary));
        setHeadersState(HEADERS_DISABLED);
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {

        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
            Intent intent = null;

            if (item instanceof String) {
                if (((String) item).contains(getString(R.string.heading_music))) {
                    intent = new Intent(getActivity(), TvMusicActivity.class);
                } else if (((String) item).contains(getString(R.string.heading_video))) {
                    intent = new Intent(getActivity(), TvVideoActivity.class);
                } else if (((String) item).contains(getString(R.string.heading_settings))) {
                    intent = new Intent(getActivity(), TvSettingsActivity.class);
                } else if (((String) item).contains(getString(R.string.heading_media_browser))) {
                    intent = new Intent(getActivity(), TvMediaBrowserActivity.class);
                }
            }

            if (intent != null) {
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
                startActivity(intent, bundle);
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {

        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        }
    }
}
