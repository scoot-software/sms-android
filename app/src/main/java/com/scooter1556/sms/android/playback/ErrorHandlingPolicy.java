package com.scooter1556.sms.android.playback;


import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy;
import com.google.android.exoplayer2.upstream.HttpDataSource;

import java.io.IOException;

public final class ErrorHandlingPolicy
        extends DefaultLoadErrorHandlingPolicy {

    /* Disable blacklisting */
    @Override
    public long getBlacklistDurationMsFor(
            int dataType, long loadDurationMs, IOException exception, int errorCount) {
        return C.TIME_UNSET;
    }

    @Override
    public long getRetryDelayMsFor(
            int dataType, long loadDurationMs, IOException exception, int errorCount) {
        if (exception instanceof HttpDataSource.HttpDataSourceException) {
            return 5000; // Retry every 5 seconds.
        } else {
            return C.TIME_UNSET;
        }
    }

    @Override
    public int getMinimumLoadableRetryCount(int dataType) {
        return Integer.MAX_VALUE;
    }
}