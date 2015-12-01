package com.scooter1556.sms.android.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.preference.PreferenceManager;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.adapter.ConnectionListAdapter;
import com.scooter1556.sms.lib.android.database.ConnectionDatabase;
import com.scooter1556.sms.lib.android.domain.Connection;

import java.util.List;

/**
 * Connection Fragment
 */
public class ConnectionFragment extends ListFragment {

    private static final String TAG = "ConnectionFragment";

    private ConnectionFragment.ConnectionListener connectionListener;

    // Stores preferences for the application
    SharedPreferences sharedPreferences;

    private List<Connection> connectionList;
    private ConnectionListAdapter connectionListAdapter;
    private ConnectionDatabase db;

    public ConnectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve preferences if they exist
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

        // Initialisation
        db = new ConnectionDatabase(getActivity().getApplicationContext());
        connectionList = db.getAllConnections();
        connectionListAdapter = new ConnectionListAdapter(getActivity().getBaseContext(), connectionList);

        // Set default connection
        connectionListAdapter.setCheckedItemID(sharedPreferences.getLong("Connection", -1));

        // Action Bar
        setHasOptionsMenu(true);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Set the adapter
        setListAdapter(connectionListAdapter);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setEmptyText(getString(R.string.connections_empty));

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView parent, View view, int position, long id) {
                getListView().startActionMode(actionModeCallBack);
                parent.requestFocusFromTouch();
                parent.setSelected(true);
                connectionListAdapter.setSelectedItemID(id);
                return true;
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            connectionListener = (ConnectionFragment.ConnectionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ConnectionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        connectionListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        updateConnectionList();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_connections, menu);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
       if(sharedPreferences.getLong("Connection", -1) != id) {
            // Update list
            connectionListAdapter.setCheckedItemID(id);
            connectionListAdapter.notifyDataSetChanged();

            // Update preferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong("Connection", id).apply();
       }
    }

    private ActionMode.Callback actionModeCallBack = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitle(R.string.connections_action_mode_title);
            mode.getMenuInflater().inflate(R.menu.menu_connections_context, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();

            switch (id) {
                case R.id.edit_connection:
                    connectionListener.EditConnection(connectionListAdapter.getSelectedItemID());
                    mode.finish();
                    break;

                case R.id.remove_connection:
                    db.deleteConnection(connectionListAdapter.getSelectedItemID());

                    if(sharedPreferences.getLong("Connection", -1) == connectionListAdapter.getSelectedItemID()) {
                        // Update preferences
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putLong("Connection", -1).apply();
                        connectionListAdapter.setCheckedItemID(-1);
                    }

                    updateConnectionList();

                    mode.finish();
                    break;

                default:
                    return false;
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            connectionListAdapter.setSelectedItemID(-1);
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_connection:
                connectionListener.AddConnection();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updateConnectionList() {
        connectionList = db.getAllConnections();
        connectionListAdapter.setItemList(connectionList);
        connectionListAdapter.notifyDataSetChanged();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface ConnectionListener {

        void AddConnection();
        void EditConnection(long id);
        void SaveConnection(Connection connection);
    }
}
