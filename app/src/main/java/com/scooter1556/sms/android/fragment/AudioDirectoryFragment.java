package com.scooter1556.sms.android.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.Toast;

import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.adapter.MediaElementListAdapter;
import com.scooter1556.sms.lib.android.domain.MediaElement;
import com.scooter1556.sms.lib.android.service.RESTService;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A fragment representing an audio directory.
 *
 */
public class AudioDirectoryFragment extends Fragment {

    private static final String TAG = "AudioDirectoryFragment";

    private MediaElementFragment.MediaElementListener mediaElementListener;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private MediaElementListAdapter mediaElementListAdapter;

    private ArrayList<MediaElement> mediaElements;

    // REST Client
    RESTService restService = null;

    ProgressDialog restProgress;

    // Information we need to retrieve contents
    Long id = null;
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

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        restService = RESTService.getInstance();

        // Retrieve arguments from main activity
        id = this.getArguments().getLong("id");
        title = this.getArguments().getString("title");
        artist = this.getArguments().getString("artist");

        // Action Bar
        setHasOptionsMenu(true);

        // Retrieve Media Elements from server
        getMediaElements();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_audio_directory, container, false);

        mediaElementListAdapter = new MediaElementListAdapter(getActivity(), mediaElements);

        ImageView coverArt = (ImageView) rootView.findViewById(R.id.cover_art);

        Picasso.with(getActivity().getBaseContext())
                .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + id + "/cover/80")
                .error(R.drawable.ic_content_album)
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
                    // add or remove selection for current list item
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
    public void onResume() {
        super.onResume();
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

        restService.getMediaElementContents(id, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                restProgress = ProgressDialog.show(getActivity(), getString(R.string.media_retrieving_elements), getString(R.string.notification_please_wait), true);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                mediaElements.add(parser.fromJson(response.toString(), MediaElement.class));

                mediaElementListAdapter.setItemList(mediaElements);
                mediaElementListAdapter.notifyDataSetChanged();

                restProgress.dismiss();
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {

                Gson parser = new Gson();

                for(int i=0; i<response.length(); i++)
                {
                    try {
                        mediaElements.add(parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class));
                    } catch (JSONException e) {
                        Toast error = Toast.makeText(getActivity(), getString(R.string.media_error_parsing_json), Toast.LENGTH_SHORT);
                        error.show();
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
                                mediaElementListAdapter.showArtist(true);
                                break;
                            }
                        }
                    }
                }

                mediaElementListAdapter.setItemList(mediaElements);
                mediaElementListAdapter.notifyDataSetChanged();

                restProgress.dismiss();
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {

                Toast error;

                restProgress.dismiss();

                switch (statusCode) {
                    case 401:
                        error = Toast.makeText(getActivity(), getString(R.string.error_unauthenticated), Toast.LENGTH_SHORT);
                        error.show();
                        break;

                    case 404:
                    case 0:
                        error = Toast.makeText(getActivity(), getString(R.string.error_server_not_found), Toast.LENGTH_SHORT);
                        error.show();
                        break;

                    default:
                        error = Toast.makeText(getActivity(), getString(R.string.error_server) + statusCode, Toast.LENGTH_SHORT);
                        error.show();
                        break;
                }

            }

            @Override
            public void onRetry(int retryNo)
            {
                String message = getString(R.string.notification_please_wait);

                // Add visual indicator of retry attempt to progress dialog
                for(int i=0; i<retryNo; i++)
                {
                    message += ".";
                }

                restProgress.setMessage(message);
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
