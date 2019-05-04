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
package com.scooter1556.sms.android.fragment;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.adapter.MediaElementListAdapter;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.service.SessionService;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.UUID;

public class MediaElementFragment extends ListFragment {

    private static final String TAG = "MediaElementFragment";

    // Callback
    private MediaElementListener mediaElementListener;

    // Flags
    boolean isReady = false;
    boolean isRunning = false;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private MediaElementListAdapter mediaElementListAdapter;

    private ArrayList<MediaElement> mediaElements;

    // REST Client
    RESTService restService = null;

    // Information we need to retrieve contents
    UUID id = null;
    String title = null;
    Byte directoryType = null;
    Boolean folder = null;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MediaElementFragment() {}


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set fragment running
        isRunning = true;

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        // Get REST service instance
        restService = RESTService.getInstance();

        // Retrieve arguments from main activity
        id = UUID.fromString(this.getArguments().getString("id"));
        title = this.getArguments().getString("title");
        directoryType = this.getArguments().getByte("directoryType");
        folder = this.getArguments().getBoolean("folder");

        // Action Bar
        setHasOptionsMenu(true);

        // Retrieve Media Elements from server
        getMediaElements();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Set list adapter
        mediaElementListAdapter = new MediaElementListAdapter(getActivity(), mediaElements);
        setListAdapter(mediaElementListAdapter);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        // Enable fast scrolling
        getListView().setFastScrollEnabled(true);

        // Show list if content is ready
        setListShown(isReady);
    }

    @Override
    public void onDestroy() {
        // Cancel any remaining requests
        restService.getClient().cancelRequests(getContext(), false);

        // Set fragment is not running
        isRunning = false;

        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mediaElementListener = (MediaElementListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement MediaElementListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mediaElementListener = null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Notify the active callbacks interface that an item has been selected.
        if (mediaElementListener != null) {
            mediaElementListener.MediaElementSelected(mediaElements.get(position));
        }
    }

    private void getMediaElements() {
        // Initialise array for media content
        mediaElements = new ArrayList<>();

        // Retrieve contents of a Media Folder
        if(folder) {
            restService.getMediaFolderContents(getContext(), id, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                    // Check fragment has not been destroyed
                    if(!isRunning) {
                        return;
                    }

                    Gson parser = new Gson();

                    for(int i=0; i<response.length(); i++) {
                        try {
                            mediaElements.add(parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class));
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing json", e);
                        }
                    }

                    mediaElementListAdapter.setItemList(mediaElements);
                    mediaElementListAdapter.notifyDataSetChanged();

                    if(isRunning) {
                        setListShown(true);
                    }

                    isReady = true;
                }
            });
        }
        // Retrieve Media Element directory contents
        else {
            restService.getMediaElementContents(getContext(), id, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                    // Check fragment has not been destroyed
                    if (!isRunning) {
                        return;
                    }

                    Gson parser = new Gson();

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            mediaElements.add(parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class));
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing json", e);
                        }
                    }

                    // For audio directories check for multiple artists and display artist if found
                    if (directoryType.equals(MediaElement.DirectoryMediaType.AUDIO)) {
                        String artist = null;

                        for (MediaElement element : mediaElements) {
                            if (element.getType().equals(MediaElement.MediaElementType.AUDIO)) {
                                if (element.getArtist() != null) {
                                    if (artist == null) {
                                        artist = element.getArtist();
                                    } else if (!element.getArtist().equals(artist)) {
                                        mediaElementListAdapter.showSubtitle(true);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    mediaElementListAdapter.setItemList(mediaElements);
                    mediaElementListAdapter.notifyDataSetChanged();

                    if (isRunning) {
                        setListShown(true);
                    }

                    isReady = true;
                }
            });
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface MediaElementListener {
        void MediaElementSelected(MediaElement element);
        void PlayAll(ArrayList<MediaElement> mediaElements);
        void AddAllAndPlayNext(ArrayList<MediaElement> mediaElements);
        void AddAllToQueue(ArrayList<MediaElement> mediaElements);
    }
}
