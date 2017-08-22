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
import java.util.UUID;

/**
 * Simple data provider for queues. Keeps track of a current queue and a current index in the
 * queue. Also provides methods to set the current queue based on common queries.
 */
public class QueueManager {
    private static final String TAG = "QueueManager";

    private Context ctx;
    private MetadataUpdateListener metadataListener;

    private List<MediaSessionCompat.QueueItem> queue;
    private List<MediaSessionCompat.QueueItem> currentQueue;
    private int currentIndex;

    private boolean shuffle = false;


    public QueueManager(@NonNull Context ctx, @NonNull MetadataUpdateListener listener) {
        this.ctx = ctx;
        this.metadataListener = listener;

        queue = new ArrayList<MediaSessionCompat.QueueItem>();
        currentQueue = new ArrayList<MediaSessionCompat.QueueItem>();
        currentIndex = 0;
    }

    public void setCurrentQueueIndex(int index) {
        if (index >= 0 && index < currentQueue.size()) {
            currentIndex = index;
            metadataListener.onCurrentQueueIndexUpdated(currentIndex);
        }
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean setCurrentQueueItem(long queueID) {
        // Set the current index on queue from queue ID
        int index = QueueUtils.getIndexByQueueID(currentQueue, queueID);
        setCurrentQueueIndex(index);
        return index >= 0;
    }

    public boolean setCurrentQueueItem(String mediaID) {
        // Set the current index on queue from media ID
        int index = QueueUtils.getIndexByMediaID(currentQueue, mediaID);
        setCurrentQueueIndex(index);
        return index >= 0;
    }

    public boolean skipQueuePosition(int amount) {
        int index = currentIndex + amount;

        // Make sure queue bounds are adhered to
        if (index < 0) {
            index = 0;
        }

        if (!QueueUtils.isIndexPlayable(index, currentQueue)) {
            Log.d(TAG, "Cannot increment queue index by " + amount + ". Out of range.");
            return false;
        }

        currentIndex = index;
        return true;
    }

    public void setShuffleMode(boolean enabled) {
        // Update flag
        shuffle = enabled;

        List<MediaSessionCompat.QueueItem> newQueue = new ArrayList<>();
        MediaSessionCompat.QueueItem currentItem = getCurrentMedia();

        if(currentQueue == null || currentQueue.isEmpty()) {
            return;
        }

        // Process queue
        if(enabled) {
            newQueue.addAll(currentQueue);
            Collections.shuffle(newQueue);
            newQueue.remove(currentItem);
            newQueue.add(0, currentItem);

            //Update current queue
            currentQueue.clear();
            currentQueue.addAll(newQueue);
            currentIndex = 0;
        } else {
            currentQueue.clear();
            currentQueue.addAll(queue);
            currentIndex = currentQueue.indexOf(currentItem);
        }

        metadataListener.onQueueUpdated(currentQueue);
    }

    public MediaSessionCompat.QueueItem getCurrentMedia() {
        if (!QueueUtils.isIndexPlayable(currentIndex, currentQueue)) {
            return null;
        }

        return currentQueue.get(currentIndex);
    }

    public int getCurrentQueueSize() {
        if (currentQueue == null) {
            return 0;
        }

        return currentQueue.size();
    }

    public void setQueueFromMediaId(final String id) {
        Log.d(TAG, "setQueueFromMediaId(" + id + ")");

        updateQueue(id, -1);
    }

    private void setQueue(List<MediaSessionCompat.QueueItem> newQueue) {
        queue.clear();
        queue.addAll(newQueue);
    }

    private void setCurrentQueue(List<MediaSessionCompat.QueueItem> newQueue) {
        setCurrentQueue(newQueue, null);
    }

    protected void setCurrentQueue(List<MediaSessionCompat.QueueItem> newQueue, int index) {
        currentQueue.clear();
        currentQueue.addAll(newQueue);
        currentIndex = Math.max(index, 0);

        metadataListener.onQueueUpdated(currentQueue);
        metadataListener.onCurrentQueueIndexUpdated(currentIndex);
    }

    private void setCurrentQueue(List<MediaSessionCompat.QueueItem> newQueue, String initialMediaID) {
        currentQueue.clear();
        currentQueue.addAll(newQueue);

        int index = 0;

        if (initialMediaID != null) {
            index = QueueUtils.getIndexByMediaID(currentQueue, initialMediaID);
        }

        currentIndex = Math.max(index, 0);

        metadataListener.onQueueUpdated(currentQueue);
        metadataListener.onCurrentQueueIndexUpdated(currentIndex);
    }

    public void addToQueue(MediaDescriptionCompat description, int index) {
        if(description.getMediaId() == null) {
            return;
        }

        updateQueue(description.getMediaId(), index);
    }

    public void resetQueue() {
        queue.clear();
        currentQueue.clear();
        currentIndex = 0;

        metadataListener.onQueueUpdated(currentQueue);
        metadataListener.onCurrentQueueIndexUpdated(currentIndex);
        metadataListener.onMetadataChanged(null);
    }

    private void updateQueue(final String id, final int index) {
        final List<MediaSessionCompat.QueueItem> newQueue = Collections.synchronizedList(new ArrayList<MediaSessionCompat.QueueItem>());
        List<String> parsedMediaId = MediaUtils.parseMediaId(id);

        if(parsedMediaId.isEmpty()) {
            metadataListener.onMetadataRetrieveError();
            return;
        }

        // Handle Playlist
        if(parsedMediaId.get(0).equals(MediaUtils.MEDIA_ID_PLAYLIST)) {
            if(parsedMediaId.size() < 2) {
                metadataListener.onMetadataRetrieveError();
                return;
            }

            final UUID playlistId = UUID.fromString(parsedMediaId.get(1));

            RESTService.getInstance().getPlaylistContents(ctx, playlistId, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                    MediaElement element;
                    Gson parser = new Gson();

                    element = parser.fromJson(response.toString(), MediaElement.class);

                    if (element == null) {
                        throw new IllegalArgumentException("Failed to fetch contents of playlist with ID: " + playlistId);
                    }

                    MediaDescriptionCompat description = MediaUtils.getMediaDescription(element);

                    if (description != null) {
                        List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();
                        newQueue.add(new MediaSessionCompat.QueueItem(description, element.getID()));
                    }

                    if (!newQueue.isEmpty()) {
                        if(index == -1 || currentQueue.size() == 0) {
                            setQueue(newQueue);
                            setCurrentQueue(newQueue);
                            updateMetadata();
                        } else {
                            queue.addAll(newQueue);

                            if(currentQueue.size() > index) {
                                currentQueue.addAll(index, newQueue);
                            } else {
                                currentQueue.addAll(newQueue);
                            }

                            metadataListener.onQueueUpdated(currentQueue);
                        }
                    } else {
                        Log.e(TAG, "No media items to add to queue after processing playlist with ID: " + playlistId);
                    }
                }

                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                    Gson parser = new Gson();

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);

                            if (element == null) {
                                throw new IllegalArgumentException("Failed to fetch contents of playlist with ID: " + playlistId);
                            }

                            MediaDescriptionCompat description = MediaUtils.getMediaDescription(element);

                            if (description != null) {
                                newQueue.add(new MediaSessionCompat.QueueItem(description, element.getID()));
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Failed to process JSON", e);
                        }
                    }

                    if (!newQueue.isEmpty()) {
                        if(index == -1 || currentQueue.size() == 0) {
                            // Update master queue
                            setQueue(newQueue);

                            // Handle Shuffle
                            if(shuffle) {
                                Collections.shuffle(newQueue);
                            }

                            setCurrentQueue(newQueue);
                            updateMetadata();
                        } else {
                            queue.addAll(newQueue);

                            // Handle Shuffle
                            if(shuffle) {
                                Collections.shuffle(newQueue);
                            }

                            if(currentQueue.size() > index) {
                                currentQueue.addAll(index, newQueue);
                            } else {
                                currentQueue.addAll(newQueue);
                            }

                            metadataListener.onQueueUpdated(currentQueue);
                        }
                    } else {
                        Log.e(TAG, "No media items to add to queue after processing playlist with ID: " + playlistId);
                    }
                }

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                    throw new IllegalArgumentException("Failed to fetch contents for playlist with ID: " + playlistId);
                }
            });
        } else if(parsedMediaId.get(0).equals(MediaUtils.MEDIA_ID_RANDOM_AUDIO)) {
            RESTService.getInstance().getRandomMediaElements(ctx, 200, MediaElement.MediaElementType.AUDIO, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                    MediaElement element;
                    Gson parser = new Gson();

                    element = parser.fromJson(response.toString(), MediaElement.class);

                    if (element == null) {
                        throw new IllegalArgumentException("Failed to fetch random media elements.");
                    }

                    MediaDescriptionCompat description = MediaUtils.getMediaDescription(element);

                    if (description != null) {
                        newQueue.add(new MediaSessionCompat.QueueItem(description, element.getID()));
                    }

                    if (!newQueue.isEmpty()) {
                        setQueue(newQueue);
                        setCurrentQueue(newQueue);
                        updateMetadata();
                    } else {
                        Log.e(TAG, "No media items to add to queue.");
                    }
                }

                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                    Gson parser = new Gson();

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);

                            if (element == null) {
                                throw new IllegalArgumentException("Failed to fetch random media elements.");
                            }

                            MediaDescriptionCompat description = MediaUtils.getMediaDescription(element);

                            if (description != null) {
                                newQueue.add(new MediaSessionCompat.QueueItem(description, element.getID()));
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Failed to process JSON", e);
                        }
                    }

                    if (!newQueue.isEmpty()) {
                        setQueue(newQueue);
                        setCurrentQueue(newQueue);
                        updateMetadata();
                    } else {
                        Log.e(TAG, "No media items to add to queue");
                    }
                }

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                    throw new IllegalArgumentException("Failed to fetch random media elements.");
                }
            });
        } else if(index == -1) {
            if(parsedMediaId.size() < 2) {
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

                    if (element == null) {
                        throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
                    }

                    MediaDescriptionCompat description = MediaUtils.getMediaDescription(element);

                    if (description != null) {
                        newQueue.add(new MediaSessionCompat.QueueItem(description, element.getID()));
                    }

                    if (!newQueue.isEmpty()) {
                        setQueue(newQueue);
                        setCurrentQueue(newQueue, id);
                        updateMetadata();
                    } else {
                        Log.e(TAG, "No media items to add to queue after processing media ID: " + id);
                    }
                }

                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                    Gson parser = new Gson();

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);

                            if (element == null) {
                                throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
                            }

                            MediaDescriptionCompat description = MediaUtils.getMediaDescription(element);

                            if (description != null) {
                                newQueue.add(new MediaSessionCompat.QueueItem(description, element.getID()));
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Failed to process JSON", e);
                        }
                    }

                    if (!newQueue.isEmpty()) {
                        // Update master queue
                        setQueue(newQueue);

                        // Handle Shuffle
                        if(shuffle) {
                            Collections.shuffle(newQueue);

                            if(QueueUtils.getIndexByMediaID(newQueue, id) != -1) {
                                MediaSessionCompat.QueueItem currentItem = newQueue.get(QueueUtils.getIndexByMediaID(newQueue, id));
                                newQueue.remove(currentItem);
                                newQueue.add(0, currentItem);
                            }
                        }

                        setCurrentQueue(newQueue, id);
                        updateMetadata();
                    } else {
                        Log.e(TAG, "No media items to add to queue after processing media ID: " + id);
                    }
                }

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                    throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
                }
            });
        } else {
            if(parsedMediaId.size() < 2) {
                metadataListener.onMetadataRetrieveError();
                return;
            }

            final long elementId = Long.parseLong(parsedMediaId.get(1));

            switch(parsedMediaId.get(0)) {
                case MediaUtils.MEDIA_ID_DIRECTORY_AUDIO:case MediaUtils.MEDIA_ID_DIRECTORY_VIDEO:
                    RESTService.getInstance().getMediaElementContents(ctx, elementId, new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                            MediaElement element;
                            Gson parser = new Gson();

                            element = parser.fromJson(response.toString(), MediaElement.class);

                            if (element == null) {
                                throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
                            }

                            MediaDescriptionCompat description = MediaUtils.getMediaDescription(element);

                            if (description != null) {
                                newQueue.add(new MediaSessionCompat.QueueItem(description, element.getID()));
                            }

                            if (!newQueue.isEmpty()) {
                                if(currentQueue.size() == 0) {
                                    setQueue(newQueue);
                                    setCurrentQueue(newQueue, id);
                                    updateMetadata();
                                } else {
                                    queue.addAll(newQueue);

                                    if (currentQueue.size() > index) {
                                        currentQueue.addAll(index, newQueue);
                                    } else {
                                        currentQueue.addAll(newQueue);
                                    }

                                    metadataListener.onQueueUpdated(currentQueue);
                                }
                            } else {
                                Log.e(TAG, "No media items to add to queue after processing media ID: " + id);
                            }
                        }

                        @Override
                        public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                            Gson parser = new Gson();

                            for (int i = 0; i < response.length(); i++) {
                                try {
                                    MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);

                                    if (element == null) {
                                        throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
                                    }

                                    MediaDescriptionCompat description = MediaUtils.getMediaDescription(element);

                                    if (description != null) {
                                        newQueue.add(new MediaSessionCompat.QueueItem(description, element.getID()));
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "Failed to process JSON", e);
                                }
                            }

                            if (!newQueue.isEmpty()) {
                                if(currentQueue.size() == 0) {
                                    setQueue(newQueue);

                                    // Handle Shuffle
                                    if (shuffle) {
                                        Collections.shuffle(newQueue);

                                        if (QueueUtils.getIndexByMediaID(newQueue, id) != -1) {
                                            MediaSessionCompat.QueueItem currentItem = newQueue.get(QueueUtils.getIndexByMediaID(newQueue, id));
                                            newQueue.remove(currentItem);
                                            newQueue.add(0, currentItem);
                                        }
                                    }

                                    setCurrentQueue(newQueue, id);
                                    updateMetadata();
                                } else {
                                    // Update master queue
                                    queue.addAll(newQueue);

                                    // Handle Shuffle
                                    if (shuffle) {
                                        Collections.shuffle(newQueue);

                                        if (QueueUtils.getIndexByMediaID(newQueue, id) != -1) {
                                            MediaSessionCompat.QueueItem currentItem = newQueue.get(QueueUtils.getIndexByMediaID(newQueue, id));
                                            newQueue.remove(currentItem);
                                            newQueue.add(0, currentItem);
                                        }
                                    }

                                    if (currentQueue.size() > index) {
                                        currentQueue.addAll(index, newQueue);
                                    } else {
                                        currentQueue.addAll(newQueue);
                                    }

                                    metadataListener.onQueueUpdated(currentQueue);
                                }
                            } else {
                                Log.e(TAG, "No media items to add to queue after processing media ID: " + id);
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                            throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
                        }
                    });

                    break;

                case MediaUtils.MEDIA_ID_AUDIO:case MediaUtils.MEDIA_ID_VIDEO:
                    RESTService.getInstance().getMediaElement(ctx, elementId, new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                            MediaElement element;
                            Gson parser = new Gson();

                            element = parser.fromJson(response.toString(), MediaElement.class);

                            if (element == null) {
                                throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
                            }

                            MediaDescriptionCompat description = MediaUtils.getMediaDescription(element);

                            if (description != null) {
                                newQueue.add(new MediaSessionCompat.QueueItem(description, element.getID()));
                            }

                            if (!newQueue.isEmpty()) {
                                if(currentQueue.size() == 0) {
                                    setQueue(newQueue);
                                    setCurrentQueue(newQueue, id);
                                    updateMetadata();
                                } else {
                                    queue.addAll(newQueue);

                                    if (currentQueue.size() > index) {
                                        currentQueue.addAll(index, newQueue);
                                    } else {
                                        currentQueue.addAll(newQueue);
                                    }

                                    metadataListener.onQueueUpdated(currentQueue);
                                }
                            } else {
                                Log.e(TAG, "No media items to add to queue after processing media ID: " + id);
                            }
                        }

                        @Override
                        public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONArray response) {
                            Gson parser = new Gson();

                            for (int i = 0; i < response.length(); i++) {
                                try {
                                    MediaElement element = parser.fromJson(response.getJSONObject(i).toString(), MediaElement.class);

                                    if (element == null) {
                                        throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
                                    }

                                    MediaDescriptionCompat description = MediaUtils.getMediaDescription(element);

                                    if (description != null) {
                                        newQueue.add(new MediaSessionCompat.QueueItem(description, element.getID()));
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "Failed to process JSON", e);
                                }
                            }

                            if (!newQueue.isEmpty()) {
                                // Update master queue
                                queue.addAll(newQueue);

                                if(currentQueue.size() > index) {
                                    currentQueue.addAll(index, newQueue);
                                } else {
                                    currentQueue.addAll(newQueue);
                                }

                                metadataListener.onQueueUpdated(currentQueue);
                            } else {
                                Log.e(TAG, "No media items to add to queue after processing media ID: " + id);
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                            throw new IllegalArgumentException("Failed to fetch item with ID: " + id);
                        }
                    });

                    break;
            }
        }
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
