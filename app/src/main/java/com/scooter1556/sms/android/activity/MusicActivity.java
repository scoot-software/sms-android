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

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.fragment.SimpleMediaFragment;
import com.scooter1556.sms.android.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity for the application.
 * This class holds the MediaBrowser and the MediaController instances. It will create a MediaBrowser
 * when it is created and connect/disconnect on start/stop.
 */
public class MusicActivity extends BaseActivity implements SimpleMediaFragment.MediaFragmentListener  {

    private static final String TAG = "MusicActivity";

    private Bundle voiceSearchParams;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_base_tab);

        initialiseToolbar();
        setDrawerItem(R.id.navigation_drawer_music);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setTabTextColors(ContextCompat.getColorStateList(this, R.color.selector_tab));
        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.indicator));
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(SimpleMediaFragment.newInstance(MediaUtils.MEDIA_ID_RECENTLY_ADDED_AUDIO), getString(R.string.heading_recently_added));
        adapter.addFragment(SimpleMediaFragment.newInstance(MediaUtils.MEDIA_ID_RECENTLY_PLAYED_AUDIO), getString(R.string.heading_recently_played));
        adapter.addFragment(SimpleMediaFragment.newInstance(MediaUtils.MEDIA_ID_PLAYLISTS), getString(R.string.heading_playlists));
        adapter.addFragment(SimpleMediaFragment.newInstance(MediaUtils.MEDIA_ID_ARTISTS), getString(R.string.heading_artists));
        adapter.addFragment(SimpleMediaFragment.newInstance(MediaUtils.MEDIA_ID_ALBUM_ARTISTS), getString(R.string.heading_album_artists));
        adapter.addFragment(SimpleMediaFragment.newInstance(MediaUtils.MEDIA_ID_ALBUMS), getString(R.string.heading_albums));
        viewPager.setAdapter(adapter);
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
            Intent intent = new Intent(MusicActivity.this, BrowseActivity.class)
                    .putExtra(MediaUtils.EXTRA_MEDIA_ID, item.getMediaId())
                    .putExtra(MediaUtils.EXTRA_MEDIA_TITLE, item.getDescription().getTitle());
            startActivityForResult(intent, RESULT_CODE_BROWSE);
        } else {
            Log.w(TAG, "Ignoring MediaItem that is neither browsable nor playable: mediaID=" + item.getMediaId());
        }
    }

    @Override
    public void setToolbarTitle(CharSequence title) {
        if (title == null) {
            title = getString(R.string.heading_music);
        }

        setTitle(title);
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

    public class ViewPagerAdapter extends FragmentStatePagerAdapter {
        private final List<Fragment> fragmentList = new ArrayList<>();
        private final List<String> fragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if(fragmentList.size() > position) {
                return fragmentList.get(position);
            }

            return null;
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if(fragmentTitleList.size() > position) {
                return fragmentTitleList.get(position);
            }

            return null;
        }

        public void addFragment(Fragment fragment, String title) {
            fragmentList.add(fragment);
            fragmentTitleList.add(title);
        }
    }
}