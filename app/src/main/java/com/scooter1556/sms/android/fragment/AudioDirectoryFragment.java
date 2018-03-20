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
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.adapter.MediaElementListAdapter;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.module.GlideApp;
import com.scooter1556.sms.android.service.RESTService;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.UUID;

public class AudioDirectoryFragment extends Fragment {

    private static final String TAG = "AudioDirectoryFragment";

    // Callback
    private MediaElementFragment.MediaElementListener mediaElementListener;

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
    String artist = null;

    ListView listView = null;
    private ActionMode actionMode;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AudioDirectoryFragment() {}


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
        artist = this.getArguments().getString("artist");

        // Action Bar
        setHasOptionsMenu(true);

        // Retrieve Media Elements from server
        getMediaElements();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_audio_directory, container, false);

        mediaElementListAdapter = new MediaElementListAdapter(getActivity(), mediaElements);

        ImageView coverArt = (ImageView) rootView.findViewById(R.id.cover_art);

        GlideApp.with(getActivity().getBaseContext())
                .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + id + "/cover?scale=80")
                .into(coverArt);

        TextView directoryTitle = (TextView) rootView.findViewById(R.id.directory_title);
        if(title != null) {
            directoryTitle.setText(title);
        }

        TextView directoryArtist = (TextView) rootView.findViewById(R.id.directory_artist);
        if(artist != null) {
            directoryArtist.setText(artist);
        }

        listView = (ListView) rootView.findViewById(R.id.songList);
        listView.setAdapter(mediaElementListAdapter);
        listView.setEmptyView(rootView.findViewById(R.id.emptyList));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (actionMode == null) {
                    if (mediaElementListener != null) {
                        mediaElementListener.MediaElementSelected(mediaElements.get(position));
                    }
                } else {
                    // Add or remove selection for current list item
                    onListItemSelect(position);
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                onListItemSelect(position);
                return true;
            }
        });

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_directory_audio, menu);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mediaElementListener = (MediaElementFragment.MediaElementListener) context;
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

    private void onListItemSelect(int position) {
        mediaElementListAdapter.toggleSelection(position);
        boolean hasCheckedItems = mediaElementListAdapter.getSelectedCount() > 0;

        if (hasCheckedItems && actionMode == null) {
            // There are some selected items, start action mode.
            actionMode = listView.startActionMode(new ActionModeCallback());
        } else if (!hasCheckedItems && actionMode != null) {
            // There no selected items, finish action mode.
            actionMode.finish();
        }

        if (actionMode != null) {
            actionMode.setTitle(String.valueOf(mediaElementListAdapter.getSelectedCount()) + " Selected");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.play_all:
                mediaElementListener.PlayAll(mediaElements);
                return true;
            case R.id.add_all_and_play_next:
                mediaElementListener.AddAllAndPlayNext(mediaElements);
                return true;
            case R.id.add_all_to_queue:
                mediaElementListener.AddAllToQueue(mediaElements);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void getMediaElements() {

        mediaElements = new ArrayList<>();

        restService.getMediaElementContents(getContext(), id, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {

                Gson parser = new Gson();

                for(int i=0; i<response.length(); i++) {
                    try {
                        mediaElements.add(parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class));
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing json", e);
                    }
                }

                // Check for multiple artists and display artist if found
                String artist = null;

                for(MediaElement element : mediaElements) {
                    if(element.getType().equals(MediaElement.MediaElementType.AUDIO)) {
                        if(element.getArtist() != null) {
                            if(artist == null) {
                                artist = element.getArtist();
                            } else if(!element.getArtist().equals(artist)) {
                                mediaElementListAdapter.showSubtitle(true);
                                break;
                            }
                        }
                    }
                }

                mediaElementListAdapter.setItemList(mediaElements);
                mediaElementListAdapter.notifyDataSetChanged();
            }
        });
    }

    private class ActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // inflate contextual menu
            mode.getMenuInflater().inflate(R.menu.menu_audio_context, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            SparseBooleanArray selected;
            ArrayList<MediaElement> selectedItems;

            switch (item.getItemId()) {

                case R.id.play:
                    // Retrieve selected item ids
                    selected = mediaElementListAdapter.getSelectedIds();

                    // Get selected media elements
                    selectedItems = new ArrayList<>();
                    for (int i = 0; i < selected.size(); i++) {
                        if (selected.valueAt(i)) {
                            selectedItems.add(mediaElementListAdapter.getItem(selected.keyAt(i)));
                        }
                    }

                    // Play selected items
                    mediaElementListener.PlayAll(selectedItems);

                    // End action mode
                    mode.finish();
                    return true;

                    case R.id.add_and_play_next:
                        // Retrieve selected item ids
                        selected = mediaElementListAdapter.getSelectedIds();

                        // Get selected media elements
                        selectedItems = new ArrayList<>();
                        for (int i = 0; i < selected.size(); i++) {
                            if (selected.valueAt(i)) {
                                selectedItems.add(mediaElementListAdapter.getItem(selected.keyAt(i)));
                            }
                        }

                        // Play selected items next
                        mediaElementListener.AddAllAndPlayNext(selectedItems);

                        // End action mode
                        mode.finish();
                        return true;

                case R.id.add_to_queue:
                    // Retrieve selected item ids
                    selected = mediaElementListAdapter.getSelectedIds();

                    // Get selected media elements
                    selectedItems = new ArrayList<>();
                    for (int i = 0; i < selected.size(); i++) {
                        if (selected.valueAt(i)) {
                            selectedItems.add(mediaElementListAdapter.getItem(selected.keyAt(i)));
                        }
                    }

                    // Play selected items next
                    mediaElementListener.AddAllToQueue(selectedItems);

                    // End action mode
                    mode.finish();
                    return true;

                default:
                    return false;
            }

        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Remove selection
            mediaElementListAdapter.removeSelection();
            actionMode = null;
        }
    }
}
