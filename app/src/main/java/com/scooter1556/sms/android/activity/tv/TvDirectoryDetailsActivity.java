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
package com.scooter1556.sms.android.activity.tv;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.fragment.tv.TvAudioDirectoryDetailsFragment;
import com.scooter1556.sms.android.fragment.tv.TvVideoDirectoryDetailsFragment;

public class TvDirectoryDetailsActivity extends Activity {

    private MediaElement mediaElement;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check media element
        mediaElement = (MediaElement) getIntent().getSerializableExtra("Directory");

        if(mediaElement == null) {
            Toast.makeText(this, getString(R.string.error_loading_media), Toast.LENGTH_LONG).show();
            ActivityCompat.finishAfterTransition(this);
        }

        // Start suitable fragment
        switch(mediaElement.getDirectoryType()) {

            case MediaElement.DirectoryMediaType.AUDIO:
                getFragmentManager().beginTransaction().add(android.R.id.content, new TvAudioDirectoryDetailsFragment()).commit();
                break;

            case MediaElement.DirectoryMediaType.VIDEO:
                getFragmentManager().beginTransaction().add(android.R.id.content, new TvVideoDirectoryDetailsFragment()).commit();
                break;
        }
    }

    public MediaElement getMediaElement() {
        return mediaElement;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}