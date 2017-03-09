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
package com.scooter1556.sms.android.utils;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

public class AutoUtils {

    private static final String TAG = "AutoUtils";

    private static final String AUTO_APP_PACKAGE_NAME = "com.google.android.projection.gearhead";

    // Use these extras to reserve space for the corresponding actions, even when they are disabled in the Playback State.
    private static final String SLOT_RESERVATION_SKIP_TO_NEXT =
            "com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_NEXT";
    private static final String SLOT_RESERVATION_SKIP_TO_PREV =
            "com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_PREVIOUS";
    private static final String SLOT_RESERVATION_QUEUE =
            "com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_QUEUE";

    /**
     * Action for an intent broadcast by Android Auto when a media app is connected or
     * disconnected. A "connected" media app is the one currently attached to the "media" facet
     * on Android Auto.
     */
    public static final String ACTION_MEDIA_STATUS = "com.google.android.gms.car.media.STATUS";

    /**
     * Key in Intent extras that contains the media connection event type (connected or disconnected)
     */
    public static final String MEDIA_CONNECTION_STATUS = "media_connection_status";

    /**
     * Value of the key MEDIA_CONNECTION_STATUS in Intent extras used when the current media app
     * is connected.
     */
    public static final String MEDIA_CONNECTED = "media_connected";

    /**
     * Value of the key MEDIA_CONNECTION_STATUS in Intent extras used when the current media app
     * is disconnected.
     */
    public static final String MEDIA_DISCONNECTED = "media_disconnected";


    public static boolean isValidAutoPackage(String packageName) {
        return AUTO_APP_PACKAGE_NAME.equals(packageName);
    }

    public static void setSlotReservationFlags(Bundle extras, boolean reservePlayingQueueSlot,
                                               boolean reserveSkipToNextSlot, boolean reserveSkipToPrevSlot) {
        if (reservePlayingQueueSlot) {
            extras.putBoolean(SLOT_RESERVATION_QUEUE, true);
        } else {
            extras.remove(SLOT_RESERVATION_QUEUE);
        }
        if (reserveSkipToPrevSlot) {
            extras.putBoolean(SLOT_RESERVATION_SKIP_TO_PREV, true);
        } else {
            extras.remove(SLOT_RESERVATION_SKIP_TO_PREV);
        }
        if (reserveSkipToNextSlot) {
            extras.putBoolean(SLOT_RESERVATION_SKIP_TO_NEXT, true);
        } else {
            extras.remove(SLOT_RESERVATION_SKIP_TO_NEXT);
        }
    }

    /**
     * Returns true when running Android Auto or a car dock.
     */
    public static boolean isAutoUiMode(Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);

        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR) {
            return true;
        } else {
            return false;
        }
    }
}