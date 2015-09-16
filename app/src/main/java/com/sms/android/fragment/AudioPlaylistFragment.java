package com.sms.android.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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

    private ActionMode actionMode;

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

        // Initialisation
        mediaElementList = audioPlaylistListener.GetCurrentPlaylist();
        playlistAdapter = new PlaylistAdapter(getActivity(), mediaElementList);

        // Action Bar
        setHasOptionsMenu(true);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Set the adapter
        setListAdapter(playlistAdapter);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setEmptyText(getString(R.string.playlist_empty));

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                onListItemSelect(position);
                return true;
            }
        });
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
    public void onResume() {
        super.onResume();

        updatePlaylist();
        updateCurrentPosition();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_audio_playlist, menu);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (actionMode == null) {
            if (audioPlaylistListener != null) {
                audioPlaylistListener.PlaylistItemSelected(position);
            }
        } else {
            // add or remove selection for current list item
            onListItemSelect(position);
        }
    }

    private void onListItemSelect(int position) {
        playlistAdapter.toggleSelection(position);
        boolean hasCheckedItems = playlistAdapter.getSelectedCount() > 0;

        if (hasCheckedItems && actionMode == null) {
            // There are some selected items, start action mode.
            actionMode = getListView().startActionMode(new ActionModeCallback());
        } else if (!hasCheckedItems && actionMode != null) {
            // There no selected items, finish action mode.
            actionMode.finish();
        }

        if (actionMode != null) {
            actionMode.setTitle(String.valueOf(playlistAdapter.getSelectedCount()) + " Selected");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.now_playing:
                audioPlaylistListener.ShowNowPlaying();
                return true;
            case R.id.clear_all:
                audioPlaylistListener.ClearAll();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updateCurrentPosition() {
        // Update playlist position
        playlistAdapter.setCheckedItem(audioPlaylistListener.GetPlaylistPosition());
        playlistAdapter.notifyDataSetChanged();
    }

    public void updatePlaylist() {
        mediaElementList = audioPlaylistListener.GetCurrentPlaylist();
        playlistAdapter.setItemList(mediaElementList);
        playlistAdapter.notifyDataSetChanged();
    }

    private class ActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // inflate contextual menu
            mode.getMenuInflater().inflate(R.menu.menu_playlist_context, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            SparseBooleanArray selected;
            ArrayList<Long> selectedItemIds = new ArrayList<>();

            switch (item.getItemId()) {

                case R.id.remove:
                    // Retrieve selected item ids
                    selected = playlistAdapter.getSelectedIds();

                    // Get selected item IDs
                    for (int i = 0; i < selected.size(); i++) {
                        if (selected.valueAt(i)) {
                            selectedItemIds.add(playlistAdapter.getItem(selected.keyAt(i)).getID());
                        }
                    }

                    // Remove these from the playlist
                    for(Long id : selectedItemIds) {
                        audioPlaylistListener.RemoveItemFromPlaylist(id);
                    }

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
            playlistAdapter.removeSelection();
            actionMode = null;
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface AudioPlaylistListener {

        void PlaylistItemSelected(int position);
        ArrayList<MediaElement> GetCurrentPlaylist();
        int GetPlaylistPosition();
        void RemoveItemFromPlaylist(long id);
        void ClearAll();
        void ShowNowPlaying();

    }
}
