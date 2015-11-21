package com.scooter1556.sms.android.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.adapter.MediaFolderListAdapter;
import com.scooter1556.sms.lib.android.domain.MediaFolder;
import com.scooter1556.sms.lib.android.service.RESTService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link MediaFolderListener}
 * interface.
 */
public class MediaFolderFragment extends ListFragment {

    private static final String TAG = "MediaFolderFragment";

    private ListView listView;
    private MediaFolderListener mediaFolderListener;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private MediaFolderListAdapter mediaFolderListAdapter;

    private List<MediaFolder> mediaFolders;

    // REST Client
    RESTService restService = null;

    ProgressDialog restProgress;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MediaFolderFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        restService = RESTService.getInstance();

        // Retrieve Media Folders from server
        getMediaFolders();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mediaFolderListAdapter = new MediaFolderListAdapter(getActivity(), mediaFolders);

        // Set the adapter
        setListAdapter(mediaFolderListAdapter);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mediaFolderListener = (MediaFolderListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
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
        if (mediaFolderListener != null) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mediaFolderListener.MediaFolderSelected(mediaFolders.get(position));
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface MediaFolderListener {

        public void MediaFolderSelected(MediaFolder folder);

    }

    private void getMediaFolders() {

        mediaFolders = new ArrayList<>();

        restService.getMediaFolders(new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                restProgress = ProgressDialog.show(getActivity(), getString(R.string.media_retrieving_folders), getString(R.string.notification_please_wait), true);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                Gson parser = new Gson();
                mediaFolders.add(parser.fromJson(response.toString(), MediaFolder.class));

                mediaFolderListAdapter.setItemList(mediaFolders);
                mediaFolderListAdapter.notifyDataSetChanged();

                restProgress.dismiss();
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {

                Gson parser = new Gson();

                for(int i=0; i<response.length(); i++)
                {
                    try {
                        mediaFolders.add(parser.fromJson(response.getJSONObject(i).toString(), MediaFolder.class));
                    } catch (JSONException e) {
                        Toast error = Toast.makeText(getActivity(), getString(R.string.media_error_parsing_json), Toast.LENGTH_SHORT);
                        error.show();
                    }
                }

                mediaFolderListAdapter.setItemList(mediaFolders);
                mediaFolderListAdapter.notifyDataSetChanged();

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
}
