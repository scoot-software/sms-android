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
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.adapter.MediaFolderListAdapter;
import com.scooter1556.sms.lib.android.domain.MediaFolder;
import com.scooter1556.sms.lib.android.service.RESTService;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class MediaFolderFragment extends ListFragment {

    private static final String TAG = "MediaFolderFragment";

    // Callback
    private MediaFolderListener mediaFolderListener;

    // Flags
    boolean isReady = false;
    boolean isRunning = false;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private MediaFolderListAdapter mediaFolderListAdapter;

    private List<MediaFolder> mediaFolders;

    // REST Client
    RESTService restService = null;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MediaFolderFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set fragment running
        isRunning = true;

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        // Get REST service instance
        restService = RESTService.getInstance();

        // Retrieve Media Folders from server
        getMediaFolders();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Set list adapter
        mediaFolderListAdapter = new MediaFolderListAdapter(getActivity(), mediaFolders);
        setListAdapter(mediaFolderListAdapter);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
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
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mediaFolderListener = (MediaFolderListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement MediaFolderListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mediaFolderListener = null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Notify the active callbacks interface that an item has been selected.
        if (mediaFolderListener != null) {
            mediaFolderListener.MediaFolderSelected(mediaFolders.get(position));
        }
    }

    private void getMediaFolders() {
        // Initialise array for media content
        mediaFolders = new ArrayList<>();

        // Retrieve list of media folders
        restService.getMediaFolders(getContext(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                // Check fragment has not been destroyed
                if(!isRunning) {
                    return;
                }

                Gson parser = new Gson();

                for(int i=0; i<response.length(); i++) {
                    try {
                        mediaFolders.add(parser.fromJson(response.getJSONObject(i).toString(), MediaFolder.class));
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing json", e);
                    }
                }

                mediaFolderListAdapter.setItemList(mediaFolders);
                mediaFolderListAdapter.notifyDataSetChanged();

                if (isRunning) {
                    setListShown(true);
                }

                isReady = true;
            }
        });
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface MediaFolderListener {
        void MediaFolderSelected(MediaFolder folder);
    }
}
