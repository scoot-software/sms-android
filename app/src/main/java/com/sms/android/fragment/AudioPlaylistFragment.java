package com.sms.android.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.sms.android.R;
import com.sms.android.adapter.PlaylistAdapter;
import com.sms.lib.android.domain.MediaElement;

import java.util.ArrayList;

/**
 * Audio Playlist Fragment
 */
public class AudioPlaylistFragment extends ListFragment {

    private ArrayList<MediaElement> mediaElementList;
    private PlaylistAdapter playlistAdapter;

    private AudioPlaylistListener audioPlaylistListener;

    public static AudioPlaylistFragment newInstance() {
        AudioPlaylistFragment fragment = new AudioPlaylistFragment();
        return fragment;
    }

    public AudioPlaylistFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get current playlist
        mediaElementList = audioPlaylistListener.getCurrentPlaylist();

        // Action Bar
        setHasOptionsMenu(true);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Setup view adapter
        playlistAdapter = new PlaylistAdapter(getActivity(), mediaElementList);

        // Set the adapter
        setListAdapter(playlistAdapter);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setEmptyText(getString(R.string.playlist_empty));
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // Check playlist position
        getListView().setItemChecked(audioPlaylistListener.getPlaylistPosition(), true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            audioPlaylistListener = (AudioPlaylistListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement AudioPlaylistListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        audioPlaylistListener = null;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Check playlist position
        getListView().setItemChecked(audioPlaylistListener.getPlaylistPosition(), true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_audio_playlist, menu);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (audioPlaylistListener != null) {
            audioPlaylistListener.PlaylistItemSelected(position);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_all:
                audioPlaylistListener.clearAll();
                mediaElementList = audioPlaylistListener.getCurrentPlaylist();
                playlistAdapter.notifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setCurrentPosition(int position) {
        // Update playlist position
        getListView().setItemChecked(position, true);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface AudioPlaylistListener {

        public void PlaylistItemSelected(int position);
        public ArrayList<MediaElement> getCurrentPlaylist();
        public int getPlaylistPosition();
        public void clearAll();

    }
}
