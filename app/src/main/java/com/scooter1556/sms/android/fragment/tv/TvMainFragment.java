package com.scooter1556.sms.android.fragment.tv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.tv.TvAudioSettingsActivity;
import com.scooter1556.sms.android.activity.tv.TvConnectionActivity;
import com.scooter1556.sms.android.activity.tv.TvTranscodeSettingsActivity;
import com.scooter1556.sms.android.activity.tv.TvVideoSettingsActivity;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.domain.MediaFolder;
import com.scooter1556.sms.android.presenter.MediaElementPresenter;
import com.scooter1556.sms.android.presenter.MediaFolderPresenter;
import com.scooter1556.sms.android.presenter.SettingsItemPresenter;
import com.scooter1556.sms.android.service.RESTService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TvMainFragment extends BrowseFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "TvMainFragment";

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int ELEMENTS_TO_LOAD = 10;

    private static final int ROW_NOW_PLAYING = 0;
    private static final int ROW_MEDIA_BROWSER = 1;
    private static final int ROW_RECENTLY_ADDED = 2;
    private static final int ROW_RECENTLY_PLAYED = 3;
    private static final int ROW_SETTINGS = 4;

    // REST Client
    RESTService restService = null;

    private ArrayObjectAdapter rowsAdapter;

    private boolean update = false;

    // Media Lists
    List<MediaFolder> mediaFolders;
    List<MediaElement> recentlyAdded;
    List<MediaElement> recentlyPlayed;

    // Background
    private final Handler handler = new Handler();
    private DisplayMetrics displayMetrics;
    private Drawable defaultBackground;
    private Timer backgroundTimer;
    private String backgroundURI;
    private BackgroundManager backgroundManager;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialisation
        restService = RESTService.getInstance();
        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        // Initialise interface
        prepareBackgroundManager();
        initialiseInterfaceElements();
        initialiseEventListeners();
        prepareEntranceTransition();

        // Fetch media lists from server
        setRows();
        getMediaFolders();
        getRecentlyAdded();
        getRecentlyPlayed();

        // Set adapter
        setAdapter(rowsAdapter);
        startEntranceTransition();
    }

    @Override
    public void onResume() {
        super.onResume();

        setRows();

        // Update rows if necessary
        if(update) {
            // Fetch media lists from server
            getMediaFolders();
            getRecentlyAdded();
            getRecentlyPlayed();

            update = false;
        }
    }

    @Override
    public void onDestroy() {
        if (backgroundTimer != null) {
            backgroundTimer.cancel();
            backgroundTimer = null;
        }
        backgroundManager = null;

        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        backgroundManager.release();

        super.onStop();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals("Connection")) {
            update = true;
        }
    }

    private void prepareBackgroundManager() {
        // Setup background manager
        backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());

        // Get default background
        defaultBackground = ContextCompat.getDrawable(getActivity(), R.drawable.default_background);
        backgroundManager.setDrawable(defaultBackground);

        // Get screen size
        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    }

    private void initialiseInterfaceElements() {
        setBadgeDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.app_icon));
        setTitle(getString(R.string.app_name));
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // Set fastLane background color
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.primary));

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.primary_dark));
    }

    private void initialiseEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ToDo: Implement search feature
                Toast.makeText(getActivity(), getString(R.string.error_search_not_implemented), Toast.LENGTH_SHORT).show();
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private void setRows() {
        rowsAdapter.clear();

        /*
        HeaderItem nowPlayingHeader = new HeaderItem(ROW_NOW_PLAYING, getString(R.string.browser_now_playing));
        MediaElementPresenter mediaElementPresenter = new MediaElementPresenter();
        ArrayObjectAdapter nowPlayingRowAdapter = new ArrayObjectAdapter(mediaElementPresenter);

        // Add current playlist item
        nowPlayingRowAdapter.add(audioPlayerService.getMediaElement());

        rowsAdapter.add(new ListRow(nowPlayingHeader, nowPlayingRowAdapter));
        */

        // Media Browser
        if(mediaFolders != null && !mediaFolders.isEmpty()) {
            HeaderItem mediaBrowserHeader = new HeaderItem(ROW_MEDIA_BROWSER, getString(R.string.heading_media_folders));
            MediaFolderPresenter mediaFolderPresenter = new MediaFolderPresenter();
            ArrayObjectAdapter mediaBrowserRowAdapter = new ArrayObjectAdapter(mediaFolderPresenter);

            // Add media folders to row
            for (MediaFolder mediaFolder : mediaFolders) {
                mediaBrowserRowAdapter.add(mediaFolder);
            }

            rowsAdapter.add(new ListRow(mediaBrowserHeader, mediaBrowserRowAdapter));
        }

        // Recently Added
        if(recentlyAdded != null && !recentlyAdded.isEmpty()) {
            HeaderItem recentlyAddedHeader = new HeaderItem(ROW_RECENTLY_ADDED, getString(R.string.heading_recently_added));
            MediaElementPresenter recentlyAddedPresenter = new MediaElementPresenter();
            ArrayObjectAdapter recentlyAddedRowAdapter = new ArrayObjectAdapter(recentlyAddedPresenter);

            // Add media elements to row
            for (MediaElement element : recentlyAdded) {
                recentlyAddedRowAdapter.add(element);
            }

            rowsAdapter.add(new ListRow(recentlyAddedHeader, recentlyAddedRowAdapter));
        }

        // Recently Played
        if(recentlyPlayed != null && !recentlyPlayed.isEmpty()) {
            HeaderItem recentlyPlayedHeader = new HeaderItem(ROW_RECENTLY_PLAYED, getString(R.string.heading_recently_played));
            MediaElementPresenter recentlyPlayedPresenter = new MediaElementPresenter();
            ArrayObjectAdapter recentlyPlayedRowAdapter = new ArrayObjectAdapter(recentlyPlayedPresenter);

            // Add media elements to row
            for (MediaElement element : recentlyPlayed) {
                recentlyPlayedRowAdapter.add(element);
            }

            rowsAdapter.add(new ListRow(recentlyPlayedHeader, recentlyPlayedRowAdapter));
        }

        // Settings
        HeaderItem settingsHeader = new HeaderItem(ROW_SETTINGS, getString(R.string.preferences_title));
        SettingsItemPresenter settingsPresenter = new SettingsItemPresenter();
        ArrayObjectAdapter settingsRowAdapter = new ArrayObjectAdapter(settingsPresenter);
        settingsRowAdapter.add(getString(R.string.preferences_title_connections));
        settingsRowAdapter.add(getString(R.string.preferences_title_audio));
        settingsRowAdapter.add(getString(R.string.preferences_title_video));
        settingsRowAdapter.add(getString(R.string.preferences_title_transcode));
        rowsAdapter.add(new ListRow(settingsHeader, settingsRowAdapter));
    }

    private void updateBackground() {
        // Get screen size
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        Glide.with(getActivity())
                .load(backgroundURI)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap>
                            glideAnimation) {
                        backgroundManager.setBitmap(resource);
                    }

                    @Override public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        backgroundManager.setDrawable(defaultBackground);
                    }
                });

        backgroundTimer.cancel();
    }

    private void startBackgroundTimer() {
        if (backgroundTimer != null) {
            backgroundTimer.cancel();
        }

        backgroundTimer = new Timer();
        backgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (backgroundURI != null) {
                        updateBackground();
                    }
                }
            });
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof String) {
                if (((String) item).contains(getString(R.string.preferences_title_audio))) {
                    Intent intent = new Intent(getActivity(), TvAudioSettingsActivity.class);
                    startActivity(intent);
                } else if (((String) item).contains(getString(R.string.preferences_title_video))) {
                    Intent intent = new Intent(getActivity(), TvVideoSettingsActivity.class);
                    startActivity(intent);
                } else if (((String) item).contains(getString(R.string.preferences_title_transcode))) {
                    Intent intent = new Intent(getActivity(), TvTranscodeSettingsActivity.class);
                    startActivity(intent);
                } else if (((String) item).contains(getString(R.string.preferences_title_connections))) {
                    Intent intent = new Intent(getActivity(), TvConnectionActivity.class);
                    startActivity(intent);
                }
            } else if (item instanceof MediaFolder) {
                MediaFolder folder = (MediaFolder) item;

                // Start grid activity
                //Intent intent = new Intent(getActivity(), MediaFolderGridActivity.class);
                //intent.putExtra("Folder", folder);
                //getActivity().startActivity(intent);
            } else if (item instanceof MediaElement) {
                MediaElement element = (MediaElement) item;
                Intent intent;

                if(element.getType().equals(MediaElement.MediaElementType.DIRECTORY)) {
                    switch(element.getDirectoryType()) {
                        case MediaElement.DirectoryMediaType.NONE:case MediaElement.DirectoryMediaType.MIXED:
                            //intent = new Intent(getActivity(), MediaElementGridActivity.class);
                            //intent.putExtra("Directory", element);
                            //getActivity().startActivity(intent);
                            break;

                        case MediaElement.DirectoryMediaType.AUDIO: case MediaElement.DirectoryMediaType.VIDEO:
                            //intent = new Intent(getActivity(), DirectoryDetailsActivity.class);
                            //intent.putExtra("Directory", element);
                            //getActivity().startActivity(intent);
                            break;
                    }
                } else if(element.getType().equals(MediaElement.MediaElementType.AUDIO)) {
                    // Now Playing
                    //intent = new Intent(getActivity(), AudioPlayerActivity.class);
                    //getActivity().startActivity(intent);
                }
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof String) {
                backgroundManager.setDrawable(defaultBackground);
            } else if (item instanceof MediaFolder) {
                backgroundManager.setDrawable(defaultBackground);
            } else if (item instanceof MediaElement) {
                MediaElement element = (MediaElement) item;
                backgroundURI = RESTService.getInstance().getConnection().getUrl() + "/image/" + element.getID() + "/fanart/" + displayMetrics.widthPixels;
                startBackgroundTimer();
            }
        }
    }

    //
    // Media Lists
    //

    private void getMediaFolders() {

        // Fetch media folders
        restService.getMediaFolders(getActivity(), new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                mediaFolders = new ArrayList<>();

                mediaFolders.add(parser.fromJson(response.toString(), MediaFolder.class));
                setRows();
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                Gson parser = new Gson();
                mediaFolders = new ArrayList<>();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        mediaFolders.add(parser.fromJson(response.getJSONObject(i).toString(), MediaFolder.class));
                    } catch (JSONException e) {
                        Toast.makeText(getActivity(), getString(R.string.error_parsing_media), Toast.LENGTH_SHORT).show();
                    }
                }

                setRows();
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                // Initialise media folders list so it can be ignored
                mediaFolders = new ArrayList<>();
                setRows();

                Toast.makeText(getActivity(), getString(R.string.error_fetching_media_folders), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                // Initialise media folders list so it can be ignored
                mediaFolders = new ArrayList<>();
                setRows();

                Toast.makeText(getActivity(), getString(R.string.error_fetching_media_folders), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getRecentlyAdded() {

        // Fetch recently added media
        restService.getRecentlyAdded(getActivity(), ELEMENTS_TO_LOAD, null, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                recentlyAdded = new ArrayList<>();

                recentlyAdded.add(parser.fromJson(response.toString(), MediaElement.class));
                setRows();
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                Gson parser = new Gson();
                recentlyAdded = new ArrayList<>();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        recentlyAdded.add(parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class));
                    } catch (JSONException e) {
                        Toast.makeText(getActivity(), getString(R.string.error_parsing_media), Toast.LENGTH_SHORT).show();
                    }
                }

                setRows();
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                // Initialise recently added list so it can be ignored
                recentlyAdded = new ArrayList<>();
                setRows();

                Toast.makeText(getActivity(), getString(R.string.error_fetching_recently_added), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                // Initialise recently added list so it can be ignored
                recentlyAdded = new ArrayList<>();
                setRows();

                Toast.makeText(getActivity(), getString(R.string.error_fetching_recently_added), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getRecentlyPlayed() {

        // Fetch recently played media
        restService.getRecentlyPlayed(getActivity(), ELEMENTS_TO_LOAD, null, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                recentlyPlayed = new ArrayList<>();

                recentlyPlayed.add(parser.fromJson(response.toString(), MediaElement.class));
                setRows();
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                Gson parser = new Gson();
                recentlyPlayed = new ArrayList<>();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        recentlyPlayed.add(parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class));
                    } catch (JSONException e) {
                        Toast.makeText(getActivity(), getString(R.string.error_parsing_media), Toast.LENGTH_SHORT).show();
                    }
                }

                setRows();
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                // Initialise recently played list so it can be ignored
                recentlyPlayed = new ArrayList<>();
                setRows();

                Toast.makeText(getActivity(), getString(R.string.error_fetching_recently_played), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                // Initialise recently added list so it can be ignored
                recentlyAdded = new ArrayList<>();
                setRows();

                Toast.makeText(getActivity(), getString(R.string.error_fetching_recently_played), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
