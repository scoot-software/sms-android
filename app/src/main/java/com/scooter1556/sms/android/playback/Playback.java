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
package com.scooter1556.sms.android.playback;

import android.support.v4.media.session.MediaSessionCompat;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.scooter1556.sms.android.domain.MediaElement;

import java.util.UUID;

/**
 * Interface representing either Local or Remote Playback.
 */
public interface Playback {
    /**
     * Start playback.
     */
    void start();

    /**
     * Stop playback
     */
    void stop(boolean notifyListeners);

    /**
     * Stop playback
     */
    void destroy();

    /**
     * Set the latest playback state as determined by the caller.
     */
    void setState(int state);

    /**
     * Get the current playback state.
     */
    int getState();

    /**
     * @return boolean that indicates that this is ready to be used.
     */
    boolean isConnected();

    /**
     * @return boolean indicating whether the player is playing.
     */
    boolean isPlaying();

    /**
     * @return position if currently playing an item.
     */
    long getCurrentStreamPosition();

    /**
     * Set the current position. Typically used when switching players that are in paused state.
     */
    void setCurrentStreamPosition(long pos);

    /**
     * Query the underlying stream and update the internal last known stream position.
     */
    void updateLastKnownStreamPosition();


    void play(String mediaId);

    /**
     * Pause the current playing item
     */
    void pause();

    /**
     * Seek to the given position
     */
    void seekTo(long position);

    /**
     * Set the current media ID. This is only used when switching from one playback to another.
     */
    void setCurrentMediaId(String mediaId);

    /**
     * @return the current media ID being processed in any state or null.
     */
    String getCurrentMediaId();

    /**
     * Set the SMS session ID.
     */
    void setSessionId(UUID sessionId);

    /**
     * @return the SMS session ID.
     */
    UUID getSessionId();

    /**
     * Set the current job ID. This is only used when switching from one playback to another.
     */
    void setCurrentJobId(UUID jobId);

    /**
     * @return the current job ID being processed in any state or null.
     */
    UUID getCurrentJobId();

    /**
     * @return the current media element.
     */
    MediaElement getCurrentMediaElement();

    /**
     * @return the current media player.
     */
    SimpleExoPlayer getMediaPlayer();

    interface Callback {
        /**
         * On playback completed.
         */
        void onCompletion();

        /**
         * on Playback status changed.
         */
        void onPlaybackStatusChanged(int state);

        /**
         * @param error to be added to the PlaybackState
         */
        void onError(String error);

        /**
         * @param mediaID being currently played
         */
        void setCurrentMediaID(String mediaID);
    }

    /**
     * @param callback to be called
     */
    void setCallback(Callback callback);
}
