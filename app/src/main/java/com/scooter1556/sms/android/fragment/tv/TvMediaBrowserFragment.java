package com.scooter1556.sms.android.fragment.tv;

import android.app.Fragment;
import android.content.ComponentName;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.PageRow;
import android.support.v17.leanback.widget.Row;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.domain.MediaFolder;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.views.row.MediaBrowserRow;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TvMediaBrowserFragment extends BrowseFragment {
    private static final String TAG = "TvMediaBrowserFragment";

    private BackgroundManager backgroundManager;

    private ArrayObjectAdapter rowsAdapter;

    private MediaBrowserCompat mediaBrowser;

    // Media Lists
    List<MediaBrowserCompat.MediaItem> mediaFolders;

    private final MediaBrowserCompat.SubscriptionCallback subscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(@NonNull String parentId,
                                             @NonNull List<MediaBrowserCompat.MediaItem> children) {
                    Log.d(TAG, "onChildrenLoaded() parentId=" + parentId);

                    if(children.isEmpty()) {
                        Log.d(TAG, "Result for " + parentId + " is empty");
                        return;
                    }

                    String id = MediaUtils.parseMediaId(parentId).get(0);

                    switch(id) {
                        case MediaUtils.MEDIA_ID_FOLDERS:
                            mediaFolders.clear();
                            mediaFolders.addAll(children);
                            mediaBrowser.unsubscribe(MediaUtils.MEDIA_ID_FOLDERS);
                            break;
                    }

                    createRows();
                }

                @Override
                public void onError(@NonNull String id) {
                    Log.e(TAG, "Media subscription error: " + id);
                }
            };

    private MediaBrowserCompat.ConnectionCallback connectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "onConnected()");

                    if (isDetached()) {
                        return;
                    }

                    // Subscribe to media browser event
                    if(rowsAdapter.size() == 0) {
                        mediaBrowser.subscribe(MediaUtils.MEDIA_ID_FOLDERS, subscriptionCallback);
                    }
                }

                @Override
                public void onConnectionFailed() {
                    Log.d(TAG, "onConnectionFailed");
                }

                @Override
                public void onConnectionSuspended() {
                    Log.d(TAG, "onConnectionSuspended");
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupUi();

        backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());

        getMainFragmentRegistry().registerFragment(MediaBrowserRow.class, new MediaBrowserRowFragmentFactory());

        // Initialisation
        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        mediaFolders = new ArrayList<>();

        setAdapter(rowsAdapter);

        // Subscribe to relevant media service callbacks
        mediaBrowser = new MediaBrowserCompat(getActivity(),
                new ComponentName(getActivity(), MediaService.class),
                connectionCallback, null);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart()");

        super.onStart();

        if (mediaBrowser != null) {
            mediaBrowser.connect();
        }
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop()");

        super.onStop();

        if (mediaBrowser != null) {
            mediaBrowser.disconnect();
        }

        BackgroundManager.getInstance(getActivity()).release();
    }

    private void setupUi() {
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.primary));
        setTitle(getString(R.string.heading_media_browser));

        prepareEntranceTransition();
    }

    private static class MediaBrowserRowFragmentFactory extends BrowseFragment.FragmentFactory {
        @Override
        public Fragment createFragment(Object rowObj) {
            MediaBrowserRow row = (MediaBrowserRow)rowObj;
            Fragment fragment = new TvMediaFolderFragment();
            Bundle bundle = new Bundle();
            bundle.putString(MediaUtils.EXTRA_MEDIA_ID, row.getMediaId().toString());
            fragment.setArguments(bundle);

            return fragment;
        }
    }

    private void createRows() {
        Log.d(TAG, "createRows()");

        if(mediaFolders.isEmpty()) {
            return;
        }

        for(MediaBrowserCompat.MediaItem folder : mediaFolders) {
            List<String> parsedMedia = MediaUtils.parseMediaId(folder.getMediaId());

            if(parsedMedia.size() == 2) {
                HeaderItem headerItem = new HeaderItem(UUID.fromString(parsedMedia.get(1)).hashCode(), folder.getDescription().getTitle().toString());
                MediaBrowserRow row = new MediaBrowserRow(headerItem);
                row.setMediaId(UUID.fromString(parsedMedia.get(1)));
                rowsAdapter.add(row);
            }
        }

        startEntranceTransition();
    }

}
