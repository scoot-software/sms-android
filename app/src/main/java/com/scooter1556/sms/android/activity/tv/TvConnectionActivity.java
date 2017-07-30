package com.scooter1556.sms.android.activity.tv;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.GuidedActionsStylist;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.database.ConnectionDatabase;
import com.scooter1556.sms.android.domain.Connection;

import java.util.ArrayList;
import java.util.List;

public class TvConnectionActivity extends Activity {
    private static final String TAG = "TvConnectionActivity";

    // Preferences
    private static SharedPreferences sharedPreferences;

    private static ConnectionDatabase db;

    private static final int ADD_CONNECTION = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");

        super.onResume();

        // Retrieve preferences if they exist
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Initialisation
        db = new ConnectionDatabase(getApplicationContext());

        GuidedStepFragment.addAsRoot(this, new TvConnectionFragment(), android.R.id.content);
    }

    public static class TvConnectionFragment extends GuidedStepFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        private static final String TAG = "TvConnectionFragment";

        // Variables
        private boolean update = false;
        private List<Connection> connectionList;

        @Override
        public void onResume() {
            Log.d(TAG, "onResume()");

            super.onResume();

            // Update connections list if necessary
            if(update) {
                populateActions(getActions());
            }
        }

        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
            Log.d(TAG, "onCreateGuidance()");

            String title = getString(R.string.preferences_title_manage_connections);
            String breadcrumb = getString(R.string.preferences_title);
            Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_connection_settings);
            return new GuidanceStylist.Guidance(title, null, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            Log.d(TAG, "onCreateActions()");

            // Register preferences listener
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);

            // Set actions
            populateActions(actions);
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            Log.d(TAG, "onGuidedActionClicked(" + action.toString() + ")");

            if (action.getId() == ADD_CONNECTION) {
                Intent intent = new Intent(getActivity(), TvEditConnectionActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(getActivity(), TvConnectionOptionsActivity.class);
                intent.putExtra("Connection", action.getId());
                startActivity(intent);
            }
        }

        @Override
        public GuidedActionsStylist onCreateActionsStylist() {
            return new GuidedActionsStylist() {
                @Override
                public int onProvideItemLayoutId() {
                    return R.layout.guided_step_separator;
                }
            };
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d(TAG, "onSharedPreferenceChanged(" + key + ")");

            update = true;
        }

        public void populateActions(List<GuidedAction> actions) {
            Log.d(TAG, "populateActions()");

            // Get connections
            connectionList = db.getAllConnections();
            long currentConnection = sharedPreferences.getLong("Connection", -1);

            actions.clear();

            // Connections Header
            actions.add(new GuidedAction.Builder(getActivity())
                    .description(getResources().getString(R.string.connections_title))
                    .infoOnly(true)
                    .enabled(false)
                    .build());

            if(connectionList.isEmpty()) {
                actions.add(new GuidedAction.Builder(getActivity())
                        .title(getResources().getString(R.string.connections_empty))
                        .infoOnly(true)
                        .enabled(false)
                        .build());
            } else {
                // List connections
                for (Connection connection : connectionList) {
                    // Add Connection
                    GuidedAction action = new GuidedAction.Builder(getActivity())
                            .id(connection.getID())
                            .title(connection.getTitle())
                            .build();

                    action.setChecked(currentConnection == connection.getID());

                    // Add to list
                    actions.add(action);
                }
            }

            // Connections Header
            actions.add(new GuidedAction.Builder(getActivity())
                    .description(getResources().getString(R.string.connections_action_mode_title))
                    .infoOnly(true)
                    .enabled(false)
                    .build());


            // Add New Connection
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(ADD_CONNECTION)
                    .title(getString(R.string.connections_add_title))
                    .build());

            setActions(actions);
        }
    }
}
