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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.database.ConnectionDatabase;
import com.scooter1556.sms.android.service.RESTService;

import cz.msebera.android.httpclient.Header;

public class TvMainActivity extends Activity {
    // REST Client
    RESTService restService = null;

    // Preferences
    private static SharedPreferences sharedPreferences;

    private static ConnectionDatabase db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_tv_main);

        // Retrieve preferences if they exist
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Initialisation
        restService = RESTService.getInstance();
        db = new ConnectionDatabase(getApplicationContext());

        // Set connection
        long id = sharedPreferences.getLong("Connection", -1);

        if(id >= 0) {
            restService.setConnection(db.getConnection(id));
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        // Set connection
        long id = sharedPreferences.getLong("Connection", -1);

        if(id >= 0) {
            restService.setConnection(db.getConnection(id));
        }

        // Check Server
        checkServerVersion();

        super.onResume();
    }

    // Check server version meets minimum requirement and display connections if not
    public void checkServerVersion() {
        // Get server version
        RESTService.getInstance().getVersion(this, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String result = new String(responseBody);
                int version = Integer.valueOf(result);

                if (version < RESTService.MIN_SUPPORTED_SERVER_VERSION) {
                    // Display warning
                    Toast version_warning = Toast.makeText(getApplicationContext(), getString(R.string.error_unsupported_server_version), Toast.LENGTH_LONG);
                    version_warning.show();

                    // Open connections activity
                    Intent intent = new Intent(getApplicationContext(), TvConnectionActivity.class);
                    startActivity(intent);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                // Display warning
                Toast connection_warning = Toast.makeText(getApplicationContext(), getString(R.string.error_server_not_found), Toast.LENGTH_LONG);
                connection_warning.show();

                // Open connections activity
                Intent intent = new Intent(getApplicationContext(), TvConnectionActivity.class);
                startActivity(intent);
            }
        });
    }
}