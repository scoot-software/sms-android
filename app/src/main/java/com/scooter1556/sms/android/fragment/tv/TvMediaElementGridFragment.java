/*
 * Author: Scott Ware <scoot.software@gmail.com>
 * Copyright (c) 2015 Scott Ware
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.scooter1556.sms.android.fragment.tv;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.support.v4.app.ActivityCompat;
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
import com.scooter1556.sms.android.activity.tv.TvDirectoryDetailsActivity;
import com.scooter1556.sms.android.activity.tv.TvMediaElementGridActivity;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.presenter.MediaElementPresenter;
import com.scooter1556.sms.android.service.RESTService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TvMediaElementGridFragment extends android.support.v17.leanback.app.VerticalGridFragment {
    private static final String TAG = "MediaElementGridFragment";

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int NUM_COLUMNS = 5;

    private ArrayObjectAdapter adapter;
    private MediaElement mediaElement;
    private List<MediaElement> mediaElements;

    // Background
    private final Handler handler = new Handler();
    private DisplayMetrics displayMetrics;
    private Drawable defaultBackground;
    private Timer backgroundTimer;
    private String backgroundURI;
    private BackgroundManager backgroundManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set root media element
        mediaElement = (MediaElement) getActivity().getIntent().getSerializableExtra("Directory");

        if(mediaElement == null) {
            Toast.makeText(getActivity(), getString(R.string.error_loading_media), Toast.LENGTH_LONG).show();
            ActivityCompat.finishAfterTransition(getActivity());
        }

        if (savedInstanceState == null) {
            setTitle(mediaElement.getTitle());
            prepareEntranceTransition();
        }

        // Initialise interface
        prepareBackgroundManager();
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ToDo: Implement search feature
                Toast.makeText(getActivity(), getString(R.string.error_search_not_implemented), Toast.LENGTH_SHORT).show();
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.primary_dark));

        getMediaElements();
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
    public void onStop() {
        backgroundManager.release();

        super.onStop();
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

    private void getMediaElements() {
        adapter = new ArrayObjectAdapter(new MediaElementPresenter());

        // Fetch directory contents
        RESTService.getInstance().getMediaElementContents(getActivity(), mediaElement.getID(), new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                mediaElements = new ArrayList<>();

                mediaElements.add(parser.fromJson(response.toString(), MediaElement.class));
                setGrid();
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                Gson parser = new Gson();
                mediaElements = new ArrayList<>();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        mediaElements.add(parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class));
                    } catch (JSONException e) {
                        Toast.makeText(getActivity(), getString(R.string.error_parsing_media), Toast.LENGTH_SHORT).show();
                    }
                }

                setGrid();
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                Toast.makeText(getActivity(), getString(R.string.error_fetching_recently_added), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                Toast.makeText(getActivity(), getString(R.string.error_fetching_recently_added), Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void setGrid() {
        if(!mediaElements.isEmpty()) {
            // Add media elements to grid
            for (MediaElement element : mediaElements) {
                adapter.add(element);
            }
        }
        setAdapter(adapter);
        startEntranceTransition();
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof MediaElement) {
                MediaElement element = (MediaElement) item;

                if(element.getType().equals(MediaElement.MediaElementType.DIRECTORY)) {
                    Intent intent;

                    switch(element.getDirectoryType()) {
                        case MediaElement.DirectoryMediaType.NONE: case MediaElement.DirectoryMediaType.MIXED:
                            intent = new Intent(getActivity(), TvMediaElementGridActivity.class);
                            intent.putExtra("Directory", element);
                            getActivity().startActivity(intent);
                            break;

                        case MediaElement.DirectoryMediaType.AUDIO: case MediaElement.DirectoryMediaType.VIDEO:
                            intent = new Intent(getActivity(), TvDirectoryDetailsActivity.class);
                            intent.putExtra("Directory", element);
                            getActivity().startActivity(intent);
                            break;
                    }
                } else if(element.getType().equals(MediaElement.MediaElementType.VIDEO)) {
                    //Intent intent = new Intent(getActivity(), VideoPlayerActivity.class);
                    //intent.putExtra("MediaElement", element);
                    //getActivity().startActivity(intent);
                }
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof MediaElement) {
                MediaElement element = (MediaElement) item;
                backgroundURI = RESTService.getInstance().getConnection().getUrl() + "/image/" + element.getID() + "/fanart/" + displayMetrics.widthPixels;
                startBackgroundTimer();
            }
        }
    }

    //
    // Background
    //
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
}
