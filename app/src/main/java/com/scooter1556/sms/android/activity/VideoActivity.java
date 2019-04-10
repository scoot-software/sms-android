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
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.core.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import androidx.viewpager.widget.ViewPager;
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
public class VideoActivity extends BaseActivity implements SimpleMediaFragment.MediaFragmentListener  {

    private static final String TAG = "HomeActivity";

    private static final int TAB_RECENTLY_ADDED = 0;
    private static final int TAB_RECENTLY_PLAYED = 1;
    private static final int TAB_COLLECTIONS = 2;

    private static final int TAB_COUNT = 3;

    private static final String SAVED_MEDIA_ID="com.scooter1556.sms.android.activity.MEDIA_ID";

    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION = "com.scooter1556.sms.android.activity.CURRENT_MEDIA_DESCRIPTION";

    private Bundle voiceSearchParams;

    private ViewPagerAdapter viewPagerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_base_tab);

        initialiseToolbar();
        setDrawerItem(R.id.navigation_drawer_video);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setTabTextColors(ContextCompat.getColorStateList(this, R.color.selector_tab));
        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.indicator));
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMediaItemSelected(MediaBrowserCompat.MediaItem item, int extra) {
        Log.d(TAG, "onMediaItemSelected(): ID=" + item.getMediaId());

        if (item.isPlayable()) {
            MediaControllerCompat.getMediaController(this).getTransportControls().playFromMediaId(item.getMediaId(), null);
        } else if (item.isBrowsable()) {
            Intent intent = new Intent(VideoActivity.this, BrowseActivity.class)
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
            title = getString(R.string.heading_video);
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

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(SimpleMediaFragment.newInstance(MediaUtils.MEDIA_ID_RECENTLY_ADDED_VIDEO), getString(R.string.heading_recently_added));
        adapter.addFragment(SimpleMediaFragment.newInstance(MediaUtils.MEDIA_ID_RECENTLY_PLAYED_VIDEO), getString(R.string.heading_recently_played));
        adapter.addFragment(SimpleMediaFragment.newInstance(MediaUtils.MEDIA_ID_COLLECTIONS), getString(R.string.heading_collections));
        viewPager.setAdapter(adapter);
    }

    public class ViewPagerAdapter extends FragmentStatePagerAdapter {
        private final List<Fragment> fragmentList = new ArrayList<>();
        private final List<String> fragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return TAB_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitleList.get(position);
        }

        public void addFragment(Fragment fragment, String title) {
            fragmentList.add(fragment);
            fragmentTitleList.add(title);
        }
    }
}