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

import android.content.Context;
import android.content.res.Resources;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.utils.QueueUtils;
import com.scooter1556.sms.android.domain.MediaElement;
import com.scooter1556.sms.android.service.RESTService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple data provider for queues. Keeps track of a current queue and a current index in the
 * queue. Also provides methods to set the current queue based on common queries.
 */
public class QueueManager {
    private static final String TAG = "QueueManager";

    private Context ctx;
    private MetadataUpdateListener metadataListener;

    private List<MediaSessionCompat.QueueItem> queue;
    private int currentIndex;

    public QueueManager(@NonNull Context ctx, @NonNull MetadataUpdateListener listener) {
        this.ctx = ctx;
        this.metadataListener = listener;

        queue = Collections.synchronizedList(new ArrayList<MediaSessionCompat.QueueItem>());
        currentIndex = 0;
    }

    private void setCurrentQueueIndex(int index) {
        if (index >= 0 && index < queue.size()) {
            currentIndex = index;
            metadataListener.onCurrentQueueIndexUpdated(currentIndex);
        }
    }

    public boolean setCurrentQueueItem(long queueID) {
        // Set the current index on queue from queue ID:
        int index = QueueUtils.getIndexByQueueID(queue, queueID);
        setCurrentQueueIndex(index);
        return index >= 0;
    }

    public boolean setCurrentQueueItem(String mediaID) {
        // Set the current index on queue from media ID:
        int index = QueueUtils.getIndexByMediaID(queue, mediaID);
        setCurrentQueueIndex(index);
        return index >= 0;
    }

    public boolean skipQueuePosition(int amount) {
        int index = currentIndex + amount;

        // Make sure queue bounds are adhered to
        if (index < 0) {
            index = 0;
        }

        if (!QueueUtils.isIndexPlayable(index, queue)) {
            Log.d(TAG, "Cannot increment queue index by " + amount + ". Out of range.");
            return false;
        }

        currentIndex = index;
        return true;
    }

    public MediaSessionCompat.QueueItem getCurrentMedia() {
        if (!QueueUtils.isIndexPlayable(currentIndex, queue)) {
            return null;
        }

        return queue.get(currentIndex);
    }

    public int getCurrentQueueSize() {
        if (queue == null) {
            return 0;
        }

        return queue.size();
    }

    public void setQueueFromMediaId(final String id) {
        Log.d(TAG, "setQueueFromMediaId(" + id + ")");

        List<MediaSessionCompat.QueueItem> newQueue = new ArrayList<>();

        List<String> parsedMediaId = MediaUtils.parseMediaId(id);

        if(parsedMediaId.size() <= 1) {
            metadataListener.onMetadataRetrieveError();
            return;
        }

        final long elementId = Long.parseLong(parsedMediaId.get(1));

        RESTService.getInstance().getMediaElementContents(ctx, elementId, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                MediaElement element;
                Gson parser = new Gson();

                element = parser.fromJson(response.toString(), MediaElement.class);

                if(element == null) {
                    throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
                }

                MediaDescriptionCompat description = MediaUtils.getMediaDescription(element);

                if(description != null) {
                    List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();
                    queue.add(new MediaSessionCompat.QueueItem(description, elementId));
                }

                if(!queue.isEmpty()) {
                    setCurrentQueue(queue, id);
                    updateMetadata();
                } else {
                    Log.e(TAG, "No media items to add to queue after processing media id: " + id);
                }
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                List<MediaElement> elements = new ArrayList<>();
                List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();
                Gson parser = new Gson();

                for (int i = 0; i < response.length(); i++) {
                    try {
                        MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);

                        if(element == null) {
                            throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
                        }

                        MediaDescriptionCompat description = MediaUtils.getMediaDescription(element);

                        if(description != null) {
                            queue.add(new MediaSessionCompat.QueueItem(description, elementId));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to process JSON", e);
                    }
                }

                if(!queue.isEmpty()) {
                    setCurrentQueue(queue, id);
                    updateMetadata();
                } else {
                    Log.e(TAG, "No media items to add to queue after processing media id: " + id);
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
            }
        });
    }

    public void addToQueueFromMediaElement(@NonNull final MediaElement element) {
        Log.d(TAG, "addToQueueFromMediaElement(" + element.getID() + ")");

        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(MediaUtils.MEDIA_ID_AUDIO + MediaUtils.SEPARATOR + String.valueOf(element.getID()))
                .setTitle(element.getTitle())
                .setSubtitle(element.getArtist())
                .setDescription(element.getTrackNumber().toString())
                .build();

        if(this.queue == null) {
            initialiseQueue(ctx.getString(R.string.app_name));
        }

        queue.add(new MediaSessionCompat.QueueItem(description, element.getID()));

        // Get index in queue
        int index = QueueUtils.getIndexByQueueID(queue, element.getID());
        currentIndex = Math.max(index, 0);

        metadataListener.onQueueUpdated(this.queue);

        updateMetadata();
    }

    protected void setCurrentQueue(List<MediaSessionCompat.QueueItem> newQueue) {
        setCurrentQueue(newQueue, null);
    }

    protected void setCurrentQueue(List<MediaSessionCompat.QueueItem> newQueue, String initialMediaID) {
        queue = newQueue;
        int index = 0;

        if (initialMediaID != null) {
            index = QueueUtils.getIndexByMediaID(queue, initialMediaID);
        }

        currentIndex = Math.max(index, 0);
        metadataListener.onQueueUpdated(newQueue);
    }

    protected void initialiseQueue(String title) {
        currentIndex = 0;
        queue = new ArrayList<>();

        metadataListener.onQueueUpdated(queue);
    }

    public void updateMetadata() {
        Log.d(TAG, "updateMetadata()");

        MediaSessionCompat.QueueItem currentMedia = getCurrentMedia();

        if (currentMedia == null || currentMedia.getDescription().getMediaId() == null) {
            metadataListener.onMetadataRetrieveError();
            return;
        }

        List<String> parsedMediaId = MediaUtils.parseMediaId(currentMedia.getDescription().getMediaId());

        if(parsedMediaId.size() <= 1) {
            metadataListener.onMetadataRetrieveError();
            return;
        }

        final long id = Long.parseLong(parsedMediaId.get(1));

        RESTService.getInstance().getMediaElement(ctx, id, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                MediaElement element;
                Gson parser = new Gson();

                element = parser.fromJson(response.toString(), MediaElement.class);

                if(element == null) {
                    throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
                }

                MediaMetadataCompat metadata = MediaUtils.getMediaMetadataCompatFromMediaElement(element);
                metadataListener.onMetadataChanged(metadata);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                //throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
            }
        });
    }

    public interface MetadataUpdateListener {
        void onMetadataChanged(MediaMetadataCompat metadata);
        void onMetadataRetrieveError();
        void onCurrentQueueIndexUpdated(int queueIndex);
        void onQueueUpdated(List<MediaSessionCompat.QueueItem> newQueue);
    }
}
