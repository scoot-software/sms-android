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
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.fragment.SimpleMediaFragment;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.domain.MediaElement;

public class BrowseActivity extends BaseActivity implements SimpleMediaFragment.MediaFragmentListener  {

    private static final String TAG = "HomeActivity";

    public static final String TAG_FRAGMENT = "fragment_browse";

    private MediaBrowserCompat.MediaItem parentMediaItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_base);

        initialiseToolbar();

        setToolbarTitle(null);

        if (savedInstanceState == null) {
            Intent intent = getIntent();

            if (intent == null || !intent.hasExtra(MediaUtils.EXTRA_MEDIA_ITEM)) {
                Log.e(TAG, "Browse activity started with no media id in the intent");
                finish();
                return;
            }

            // Set toolbar title
            setToolbarTitle(intent.getStringExtra(MediaUtils.EXTRA_MEDIA_TITLE));

            // Set parent media item
            parentMediaItem = intent.getParcelableExtra(MediaUtils.EXTRA_MEDIA_ITEM);

            // Initialise fragment
            SimpleMediaFragment fragment = SimpleMediaFragment.newInstance(parentMediaItem.getMediaId());
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
            if(MediaUtils.getMediaTypeFromID(item.getMediaId()) == MediaElement.MediaElementType.VIDEO) {
                // Start video playback
                Intent intent = new Intent(BrowseActivity.this, VideoPlayerActivity.class)
                        .putExtra(MediaUtils.EXTRA_MEDIA_ITEM, item);
                startActivityForResult(intent, RESULT_CODE_BROWSE);
            } else {
                getSupportMediaController().getTransportControls().playFromMediaId(item.getMediaId(), null);
            }
        } else if (item.isBrowsable()) {
            SimpleMediaFragment fragment = SimpleMediaFragment.newInstance(item.getMediaId());
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.container, fragment, TAG_FRAGMENT);
            transaction.addToBackStack(null);
            transaction.commit();
        } else {
            Log.w(TAG, "Ignoring MediaItem that is neither browsable nor playable: mediaID=" + item.getMediaId());
        }
    }

    @Override
    public void setToolbarTitle(CharSequence title) {
        if (title == null) {
            return;
        }

        setTitle(title);
    }
}