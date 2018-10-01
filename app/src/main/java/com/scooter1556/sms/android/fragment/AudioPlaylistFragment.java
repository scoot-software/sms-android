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

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.adapter.PlaylistAdapter;
import com.scooter1556.sms.android.domain.MediaElement;

import java.util.ArrayList;

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
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            audioPlaylistListener = (AudioPlaylistListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
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
                            selectedItemIds.add(Integer.valueOf(playlistAdapter.getItem(selected.keyAt(i)).getID().hashCode()).longValue());
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
