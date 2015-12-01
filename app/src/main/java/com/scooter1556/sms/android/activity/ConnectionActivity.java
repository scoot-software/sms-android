package com.scooter1556.sms.android.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.fragment.ConnectionFragment;
import com.scooter1556.sms.android.fragment.EditConnectionFragment;
import com.scooter1556.sms.lib.android.database.ConnectionDatabase;
import com.scooter1556.sms.lib.android.domain.Connection;

public class ConnectionActivity extends AppCompatActivity implements ConnectionFragment.ConnectionListener {

    // Application Preferences
    SharedPreferences sharedPreferences;

    // Fragments
    ConnectionFragment connectionFragment;

    // Database
    private ConnectionDatabase db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_connection);

        // Initialisation
        db = new ConnectionDatabase(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (findViewById(R.id.fragment_container) != null) {

            if (savedInstanceState != null) {
                return;
            }

            connectionFragment = new ConnectionFragment();

            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, connectionFragment).commit();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void AddConnection() {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new EditConnectionFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .commit();

        getSupportActionBar().setTitle(R.string.connections_add_title);
    }

    @Override
    public void EditConnection(long id) {
        EditConnectionFragment fragment = new EditConnectionFragment();
        Bundle arguments = new Bundle();
        arguments.putSerializable("Connection", db.getConnection(id));
        fragment.setArguments(arguments);

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .commit();

        getSupportActionBar().setTitle(R.string.connections_edit_title);
    }

    @Override
    public void SaveConnection(Connection connection) {

        // Determine if we should add or update the connection
        if(connection.getID() == null) {
            long id = db.addConnection(connection);

            // Update preferences
            if(id > -1) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putLong("Connection", id).apply();
            }
        } else {
            db.updateConnection(connection);
        }


        // Load updated connections list
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ConnectionFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .commit();

        getSupportActionBar().setTitle(R.string.connections_title);
    }
}
