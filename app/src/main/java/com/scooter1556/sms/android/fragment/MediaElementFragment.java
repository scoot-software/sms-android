package com.scooter1556.sms.android.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.adapter.MediaElementListAdapter;
import com.scooter1556.sms.lib.android.domain.MediaElement;
import com.scooter1556.sms.lib.android.service.RESTService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A fragment representing a list of media elements.
 *
 * Activities containing this fragment MUST implement the {@link MediaElementListener}
 * interface.
 */
public class MediaElementFragment extends ListFragment {

    private static final String TAG = "MediaElementFragment";

    private ListView listView;
    private MediaElementListener mediaElementListener;

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

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        restService = RESTService.getInstance();

        // Retrieve arguments from main activity
        id = this.getArguments().getLong("id");
        title = this.getArguments().getString("title");
        directoryType = this.getArguments().getByte("directoryType");
        folder = this.getArguments().getBoolean("folder");

        // Action Bar
        setHasOptionsMenu(true);

        // Retrieve Media Elements from server
        getMediaElements();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mediaElementListAdapter = new MediaElementListAdapter(getActivity(), mediaElements);

        // Set the adapter
        setListAdapter(mediaElementListAdapter);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        getListView().setFastScrollEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mediaElementListener = (MediaElementListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MediaElementListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mediaElementListener = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mediaElementListener != null) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mediaElementListener.MediaElementSelected(mediaElements.get(position));
        }
    }

    private void getMediaElements() {

        mediaElements = new ArrayList<>();

        // Retrieve contents of a Media Folder
        if(folder) {

            restService.getMediaFolderContents(id, new JsonHttpResponseHandler() {
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

                    mediaElementListAdapter.setItemList(mediaElements);
                    mediaElementListAdapter.notifyDataSetChanged();

                    restProgress.dismiss();
                }

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {

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
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONArray response) {

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
        // Retrieve Media Element directory contents
        else {

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

                    // For audio directories check for multiple artists and display artist if found
                    if(directoryType.equals(MediaElement.DirectoryMediaType.AUDIO)) {
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
