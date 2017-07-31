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

import android.support.v4.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.fragment.BaseFragment;
import com.scooter1556.sms.android.fragment.HomeFragment;
import com.scooter1556.sms.android.fragment.SimpleMediaFragment;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.utils.MediaUtils;

/**
 * Main activity for the application.
 * This class holds the MediaBrowser and the MediaController instances. It will create a MediaBrowser
 * when it is created and connect/disconnect on start/stop.
 */
public class HomeActivity extends BaseActivity implements BaseFragment.MediaFragmentListener  {

    private static final String TAG = "HomeActivity";

    public static final String EXTRA_START_FULLSCREEN = "com.scooter1556.sms.android.activity.EXTRA_START_FULLSCREEN";
    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION = "com.scooter1556.sms.android.activity.CURRENT_MEDIA_DESCRIPTION";

    public static final String TAG_FRAGMENT = "fragment_home";

    private Bundle voiceSearchParams;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_base);

        initialiseToolbar();
        initialiseFromParams(savedInstanceState, getIntent());

        setToolbarTitle(null);
        setDrawerItem(R.id.navigation_drawer_home);

        if (savedInstanceState == null) {
            // Only check if a full screen player is needed the first time
            startFullScreenActivityIfNeeded(getIntent());

            // Initialise main fragment
            HomeFragment fragment = HomeFragment.newInstance();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.container, fragment, TAG_FRAGMENT);
            transaction.commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMediaItemSelected(MediaBrowserCompat.MediaItem item) {
        Log.d(TAG, "onMediaItemSelected(): ID=" + item.getMediaId());

        if (item.isPlayable()) {
            MediaControllerCompat.getMediaController(this).getTransportControls().playFromMediaId(item.getMediaId(), null);
        } else if (item.isBrowsable()) {
            Intent intent = new Intent(HomeActivity.this, BrowseActivity.class)
                    .putExtra(MediaUtils.EXTRA_MEDIA_ITEM, item);
            startActivityForResult(intent, RESULT_CODE_BROWSE);
        } else {
            Log.w(TAG, "Ignoring MediaItem that is neither browsable nor playable: mediaID=" + item.getMediaId());
        }
    }

    @Override
    public void setToolbarTitle(CharSequence title) {
        if (title == null) {
            title = getString(R.string.heading_home);
        }

        setTitle(title);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        initialiseFromParams(null, intent);
        startFullScreenActivityIfNeeded(intent);
    }

    private void startFullScreenActivityIfNeeded(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_START_FULLSCREEN, false)) {
            Intent fullScreenIntent = new Intent(this, FullScreenPlayerActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION, intent.getParcelableExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION));

            startActivity(fullScreenIntent);
        }
    }

    protected void initialiseFromParams(Bundle savedInstanceState, Intent intent) {
        // check if we were started from a "Play XYZ" voice search. If so, we save the extras
        // (which contain the query details) in a parameter, so we can reuse it later, when the
        // MediaSession is connected.
        if (intent.getAction() != null && intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)) {
            voiceSearchParams = intent.getExtras();
            Log.d(TAG, "Starting from voice search query=" + voiceSearchParams.getString(SearchManager.QUERY));
        }
    }

    @Override
    protected void onMediaControllerConnected() {
        if (voiceSearchParams != null) {
            // If there is a bootstrap parameter to start from a search query, we
            // send it to the media session and set it to null, so it won't play again
            // when the activity is stopped/started or recreated:
            String query = voiceSearchParams.getString(SearchManager.QUERY);
            MediaControllerCompat.getMediaController(this).getTransportControls().playFromSearch(query, voiceSearchParams);
            voiceSearchParams = null;
        }
    }
}