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

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;

import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.mediarouter.app.MediaRouteButton;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.IntroductoryOverlay;
import com.scooter1556.sms.android.R;

/**
 * Abstract activity with toolbar, navigation drawer and cast support. Needs to be extended by
 * any activity that wants to be shown as a top level activity.
 */
public abstract class ActionBarCastActivity extends AppCompatActivity {

    private static final String TAG = "ActionBarCastActivity";

    public static final int RESULT_CODE_SETTINGS = 101;
    public static final int RESULT_CODE_CONNECTIONS = 102;

    private static final int DELAY_MILLIS = 1000;

    private CastContext castContext;
    private MenuItem mediaRouteMenuItem;
    private Toolbar toolbar;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;

    private boolean toolbarInitialised;

    private int selectedDrawerItem = -1;
    private int lastSelectedDrawerItem = -1;

    private CastStateListener castStateListener = new CastStateListener() {
        @Override
        public void onCastStateChanged(int newState) {
            Log.d(TAG, "onCastStateChanged()");
            if (newState != CastState.NO_DEVICES_AVAILABLE) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mediaRouteMenuItem.isVisible()) {
                            Log.d(TAG, "Cast Icon is visible");
                            showFirstTimeUserExperience();
                        }
                    }
                }, DELAY_MILLIS);
            }
        }
    };

    private final DrawerLayout.DrawerListener drawerListener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerClosed(View drawerView) {
            if (drawerToggle != null) drawerToggle.onDrawerClosed(drawerView);

            if (selectedDrawerItem != lastSelectedDrawerItem) {
                Class activityClass;

                switch (selectedDrawerItem) {
                    case R.id.navigation_drawer_home:
                        activityClass = HomeActivity.class;
                        startActivity(new Intent(ActionBarCastActivity.this, activityClass));
                        finish();
                        break;

                    case R.id.navigation_drawer_music:
                        activityClass = MusicActivity.class;
                        startActivity(new Intent(ActionBarCastActivity.this, activityClass));
                        finish();
                        break;

                    case R.id.navigation_drawer_video:
                        activityClass = VideoActivity.class;
                        startActivity(new Intent(ActionBarCastActivity.this, activityClass));
                        finish();
                        break;

                    case R.id.navigation_drawer_settings:
                        activityClass = SettingsActivity.class;
                        startActivityForResult(new Intent(ActionBarCastActivity.this, activityClass), RESULT_CODE_SETTINGS);
                        break;
                }
            }
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            if (drawerToggle != null) drawerToggle.onDrawerStateChanged(newState);
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            if (drawerToggle != null) drawerToggle.onDrawerSlide(drawerView, slideOffset);
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            if (drawerToggle != null) drawerToggle.onDrawerOpened(drawerView);
            lastSelectedDrawerItem = selectedDrawerItem;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");

        castContext = CastContext.getSharedInstance(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart()");

        if (!toolbarInitialised) {
            throw new IllegalStateException("You must run super.initialiseToolbar at the end of your onCreate method");
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Log.d(TAG, "onPostCreate()");

        if (drawerToggle != null) {
            drawerToggle.syncState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");

        castContext.addCastStateListener(castStateListener);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (drawerToggle != null) {
            drawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "onPause()");

        castContext.removeCastStateListener(castStateListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main, menu);
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle != null && drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // If not handled by drawerToggle, home needs to be handled by returning to previous
        if (item != null && item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If the drawer is open, back will close it
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
            return;
        }

        super.onBackPressed();
    }

    /**
     * This method is called when an activity has quit which was called with
     * startActivityForResult method. Depending on the given request and result
     * code certain action can be done.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_CODE_SETTINGS) {
            if(lastSelectedDrawerItem >= 0) {
                setDrawerItem(lastSelectedDrawerItem);
            }
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

    protected void setDrawerItem(int id) {
        if (drawerLayout != null) {
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

            if (navigationView == null) {
                throw new IllegalStateException("Layout requires a NavigationView with id 'nav_view'");
            }

            selectedDrawerItem = id;
            navigationView.setCheckedItem(id);
        }
    }

    protected void initialiseToolbar() {
        Log.d(TAG, "initialiseToolbar()");

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (toolbar == null) {
            throw new IllegalStateException("Layout is required to include a Toolbar with id 'toolbar'");
        }

        toolbar.inflateMenu(R.menu.main);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (drawerLayout != null) {
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

            if (navigationView == null) {
                throw new IllegalStateException("Layout requires a NavigationView with id 'nav_view'");
            }

            // Create an ActionBarDrawerToggle that will handle opening/closing of the drawer:
            drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(drawerListener);
            populateDrawerItems(navigationView);
            setSupportActionBar(toolbar);
        } else {
            setSupportActionBar(toolbar);
        }

        toolbarInitialised = true;
    }

    private void populateDrawerItems(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        menuItem.setChecked(true);
                        selectedDrawerItem = menuItem.getItemId();
                        drawerLayout.closeDrawers();
                        return true;
                    }
                });
    }

    /**
     * Shows the Cast First Time User experience to the user
     */
    private void showFirstTimeUserExperience() {
        Menu menu = toolbar.getMenu();
        View view = menu.findItem(R.id.media_route_menu_item).getActionView();

        if (view instanceof MediaRouteButton) {
            IntroductoryOverlay overlay = new IntroductoryOverlay.Builder(this, mediaRouteMenuItem)
                    .setTitleText(R.string.cast_first_time_ux)
                    .setSingleTime()
                    .build();
            overlay.show();
        }
    }
}