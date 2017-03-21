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
package com.scooter1556.sms.android.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.fragment.ConnectionFragment;
import com.scooter1556.sms.android.fragment.EditConnectionFragment;
import com.scooter1556.sms.android.database.ConnectionDatabase;
import com.scooter1556.sms.android.domain.Connection;

public class ConnectionActivity extends AppCompatActivity implements ConnectionFragment.ConnectionListener {

    // Application Preferences
    SharedPreferences sharedPreferences;

    // Fragments
    ConnectionFragment connectionFragment;

    // Toolbar
    private Toolbar toolbar;

    // Database
    private ConnectionDatabase db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_connection);

        // Initialisation
        db = new ConnectionDatabase(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        initialiseToolbar();

        if (findViewById(R.id.container) != null) {

            if (savedInstanceState != null) {
                return;
            }

            connectionFragment = new ConnectionFragment();

            getSupportFragmentManager().beginTransaction().add(R.id.container, connectionFragment).commit();
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        toolbar.setTitle(title);
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        toolbar.setTitle(titleId);
    }

    protected void initialiseToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.menu_connections);
            setSupportActionBar(toolbar);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
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
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new EditConnectionFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .commit();
    }

    @Override
    public void EditConnection(long id) {
        EditConnectionFragment fragment = new EditConnectionFragment();
        Bundle arguments = new Bundle();
        arguments.putSerializable("Connection", db.getConnection(id));
        fragment.setArguments(arguments);

        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .commit();
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
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new ConnectionFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .commit();
    }
}
