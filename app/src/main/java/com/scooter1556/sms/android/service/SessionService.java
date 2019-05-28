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
package com.scooter1556.sms.android.service;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.TextHttpResponseHandler;
import com.scooter1556.sms.android.domain.ClientProfile;

import java.util.UUID;

import cz.msebera.android.httpclient.Header;

public class SessionService {
    private static final String TAG = "SessionService";

    private UUID sessionId = null;

    private SessionService() {}

    private static final SessionService instance = new SessionService();

    public static SessionService getInstance() {
        return instance;
    }

    public void newSession(Context context, UUID id, ClientProfile profile) {
        Log.d(TAG, "newSession() > " + id);

        if(id == null && profile == null) {
            return;
        }

        // End current session
        endSession(sessionId);

        RESTService.getInstance().addSession(context, id, profile, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.d(TAG, "Failed to add new session");
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                // Parse result
                sessionId = UUID.fromString(responseString);
                Log.d(TAG, "New session ID: " + sessionId);
            }
        });
    }

    public void endCurrentSession() {
        endSession(sessionId);
    }

    public void endSession(final UUID id) {
        Log.d(TAG, "endSession() > " + id);

        if(id == null) {
            return;
        }

        RESTService.getInstance().endSession(id, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                Log.d(TAG, "Failed to end session with id: " + id);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                Log.d(TAG, "Session ended with id: " + id);

                if(sessionId.equals(id)) {
                    sessionId = null;
                }
            }
        });
    }

    public UUID getSessionId() {
        return sessionId;
    }
}
