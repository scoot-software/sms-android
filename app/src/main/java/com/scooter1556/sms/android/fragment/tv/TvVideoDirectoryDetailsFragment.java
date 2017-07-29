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
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
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
import com.scooter1556.sms.android.presenter.DetailsDescriptionPresenter;
import com.scooter1556.sms.android.presenter.MediaElementPresenter;
import com.scooter1556.sms.android.service.RESTService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TvVideoDirectoryDetailsFragment extends DetailsFragment {
    private static final String TAG = "VideoDirectoryDetailsFragment";

    private static final int ACTION_PLAY = 0;

    private static final int DETAIL_THUMB_WIDTH = 274;
    private static final int DETAIL_THUMB_HEIGHT = 274;

    private MediaElement mediaElement;
    List<MediaElement> mediaElements;

    private ArrayObjectAdapter adapter;
    private ClassPresenterSelector presenterSelector;

    private BackgroundManager backgroundManager;
    private Drawable defaultBackground;
    private DisplayMetrics displayMetrics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prepareBackgroundManager();

        mediaElement = ((TvDirectoryDetailsActivity) getActivity()).getMediaElement();

        setupAdapter();
        setupDetailsOverview();
        getContents();
        setBackground();

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.primary_dark));

        // When a Related Movie item is clicked.
        setOnItemViewClickedListener(new ItemViewClickedListener());

        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ToDo: Implement search feature
                Toast.makeText(getActivity(), getString(R.string.error_search_not_implemented), Toast.LENGTH_SHORT).show();
            }
        });
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

    private void setBackground() {
        Glide.with(getActivity())
                .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + mediaElement.getID() + "/fanart/" + displayMetrics.widthPixels)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>(displayMetrics.widthPixels, displayMetrics.heightPixels) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap>
                            glideAnimation) {
                        backgroundManager.setBitmap(resource);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        backgroundManager.setDrawable(defaultBackground);
                    }
                });
    }

    private void setupAdapter() {
        // Set detail background and style.
        FullWidthDetailsOverviewRowPresenter detailsPresenter =
                new FullWidthDetailsOverviewRowPresenter(new DetailsDescriptionPresenter());

        detailsPresenter.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.primary_dark));
        detailsPresenter.setInitialState(FullWidthDetailsOverviewRowPresenter.STATE_FULL);

        prepareEntranceTransition();

        detailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if(action.getId() == ACTION_PLAY) {
                    if(mediaElements == null || mediaElements.isEmpty()) {
                        Toast.makeText(getActivity(), getString(R.string.error_media_unavailable), Toast.LENGTH_SHORT);
                        return;
                    }

                    // Find the first video element and play it
                    for(MediaElement element : mediaElements) {
                        if(element.getType().equals(MediaElement.MediaElementType.VIDEO)) {
                            //Intent intent = new Intent(getActivity(), VideoPlayerActivity.class);
                            //intent.putExtra("MediaElement", element);
                            //getActivity().startActivity(intent);
                            break;
                        }
                    }
                }
            }
        });

        presenterSelector = new ClassPresenterSelector();
        presenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
        presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        adapter = new ArrayObjectAdapter(presenterSelector);
        setAdapter(adapter);
    }

    private void setupDetailsOverview() {
        final DetailsOverviewRow row = new DetailsOverviewRow(mediaElement);

        startEntranceTransition();

        Glide.with(getActivity())
                .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + mediaElement.getID() + "/cover/" + DETAIL_THUMB_HEIGHT)
                .asBitmap()
                .dontAnimate()
                .error(defaultBackground)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(final Bitmap resource, GlideAnimation glideAnimation) {
                        row.setImageBitmap(getActivity(), resource);
                    }
                });

        SparseArrayObjectAdapter actionsAdapter = new SparseArrayObjectAdapter();

        actionsAdapter.set(ACTION_PLAY, new Action(ACTION_PLAY,
                getResources().getString(R.string.label_play), null));

        row.setActionsAdapter(actionsAdapter);

        adapter.add(row);
    }

    private void setupMediaList() {
        List<MediaElement> directories = new ArrayList<>();
        List<MediaElement> videos = new ArrayList<>();

        // Determine directory contents
        for(MediaElement element : mediaElements) {
            if(element.getType().equals(MediaElement.MediaElementType.DIRECTORY)) {
                directories.add(element);
            } else if(element.getType().equals(MediaElement.MediaElementType.VIDEO)) {
                videos.add(element);
            }
        }

        // Generate contents if necessary
        if(videos.size() > 1) {
            ArrayObjectAdapter contentsRowAdapter = new ArrayObjectAdapter(new MediaElementPresenter());
            HeaderItem header = new HeaderItem(0, getString(R.string.heading_contents));

            for (MediaElement element : videos) {
                contentsRowAdapter.add(element);
            }

            adapter.add(new ListRow(header, contentsRowAdapter));
        }

        // Generate row for sub-directories
        if(!directories.isEmpty()) {
            for(MediaElement directory : directories) {
                final ArrayObjectAdapter directoryRowAdapter = new ArrayObjectAdapter(new MediaElementPresenter());
                final HeaderItem header = new HeaderItem(directory.getID(), directory.getTitle());

                // Fetch directory contents
                RESTService.getInstance().getMediaElementContents(getActivity(), directory.getID(), new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                        Gson parser = new Gson();

                        MediaElement element = parser.fromJson(response.toString(), MediaElement.class);
                        directoryRowAdapter.add(element);
                        adapter.add(new ListRow(header, directoryRowAdapter));
                    }

                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                        Gson parser = new Gson();

                        for (int i = 0; i < response.length(); i++) {
                            try {
                                MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);
                                directoryRowAdapter.add(element);
                            } catch (JSONException e) {
                                Toast.makeText(getActivity(), getString(R.string.error_parsing_media), Toast.LENGTH_SHORT).show();
                            }
                        }

                        adapter.add(new ListRow(header, directoryRowAdapter));
                    }

                    @Override
                    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                        // Do nothing...
                    }

                    @Override
                    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {
                        // Do nothing...
                    }
                });
            }
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof MediaElement) {
                MediaElement element = (MediaElement) item;

                if(element.getType().equals(MediaElement.MediaElementType.DIRECTORY)) {
                    Intent intent = new Intent(getActivity(), TvMediaElementGridActivity.class);
                    intent.putExtra("Directory", element);
                    getActivity().startActivity(intent);
                } else if(element.getType().equals(MediaElement.MediaElementType.VIDEO)) {
                    //Intent intent = new Intent(getActivity(), VideoPlayerActivity.class);
                    //intent.putExtra("MediaElement", element);
                    //getActivity().startActivity(intent);
                }
            }
        }
    }

    private void getContents() {
        RESTService.getInstance().getMediaElementContents(getActivity(), mediaElement.getID(), new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                mediaElements = new ArrayList<>();

                mediaElements.add(parser.fromJson(response.toString(), MediaElement.class));

                setupMediaList();
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

                setupMediaList();
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
}
