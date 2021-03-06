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
package com.scooter1556.sms.android.fragment;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.listener.OnListItemClickListener;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.utils.NetworkUtils;
import com.scooter1556.sms.android.views.adapter.MediaItemAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * A Fragment that lists Media Browser content
 */
public class SimpleMediaFragment extends BaseFragment implements MediaItemAdapter.OnClicked {

    private static final String TAG = "SimpleMediaFragment";

    private MediaBrowserCompat mediaBrowser;
    private MediaFragmentListener mediaFragmentListener;

    private View view;
    private RecyclerView recyclerView;
    private MediaItemAdapter adapter;
    private GridLayoutManager lm;
    private List<MediaBrowserCompat.MediaItem> items;
    private int lastFirstVisiblePosition = -1;

    private String mediaId;

    private final BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver() {
        private boolean oldOnline = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            // We don't care about network changes while this fragment is not associated with a media ID
            if (mediaId != null) {
                boolean isOnline = NetworkUtils.isOnline(context);

                if (isOnline != oldOnline) {
                    oldOnline = isOnline;
                }
            }
        }
    };

    private final MediaBrowserCompat.SubscriptionCallback subscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(@NonNull String parentId,
                                             @NonNull List<MediaBrowserCompat.MediaItem> children) {
                    Log.d(TAG, "onChildrenLoaded() parentId=" + parentId);

                    String id = MediaUtils.parseMediaId(parentId).get(0);

                    items.clear();
                    items.addAll(children);


                    /*
                    ArrayList<MediaBrowserCompat.MediaItem> dirs = new ArrayList<>();
                    ArrayList<MediaBrowserCompat.MediaItem> audio_dirs = new ArrayList<>();
                    ArrayList<MediaBrowserCompat.MediaItem> video_dirs = new ArrayList<>();
                    ArrayList<MediaBrowserCompat.MediaItem> albums = new ArrayList<>();
                    ArrayList<MediaBrowserCompat.MediaItem> artists = new ArrayList<>();
                    ArrayList<MediaBrowserCompat.MediaItem> collections = new ArrayList<>();
                    ArrayList<MediaBrowserCompat.MediaItem> audio = new ArrayList<>();
                    ArrayList<MediaBrowserCompat.MediaItem> video = new ArrayList<>();

                    // Sort child elements
                    for(MediaBrowserCompat.MediaItem item : children) {
                        switch(MediaUtils.getMediaID(item.getMediaId())) {
                            case MediaService.MEDIA_ID_ALBUM:
                                albums.add(item);
                                break;

                            case MediaService.MEDIA_ID_ARTIST:case MediaService.MEDIA_ID_ALBUM_ARTIST:
                                artists.add(item);
                                break;

                            case MediaService.MEDIA_ID_COLLECTION:
                                collections.add(item);
                                break;

                            case MediaService.MEDIA_ID_DIRECTORY:
                                dirs.add(item);
                                break;

                            case MediaService.MEDIA_ID_DIRECTORY_AUDIO:
                                audio_dirs.add(item);
                                break;

                            case MediaService.MEDIA_ID_DIRECTORY_VIDEO:
                                video_dirs.add(item);
                                break;

                            case MediaService.MEDIA_ID_VIDEO:
                                video.add(item);
                                break;

                            case MediaService.MEDIA_ID_AUDIO:
                                audio.add(item);
                                break;
                        }
                    }

                    if(!audio_dirs.isEmpty()) {
                        items.clear();
                        items.addAll(audio_dirs);
                        mediaAdapter = new MediaItemAdapter(getContext(), R.layout.item_media_audio, items, clickListener);
                    }

                    if(!video_dirs.isEmpty()) {
                        items.clear();
                        items.addAll(video_dirs);
                        mediaAdapter = new MediaItemAdapter(getContext(), R.layout.item_media_video, items, clickListener);
                    }

                    if(!dirs.isEmpty()) {
                        items.clear();
                        items.addAll(dirs);
                        mediaAdapter = new MediaItemAdapter(getContext(), R.layout.item_media_directory, items, clickListener);
                    }

                    if(!artists.isEmpty()) {
                        items.clear();
                        items.addAll(artists);
                        mediaAdapter = new MediaItemAdapter(getContext(), R.layout.item_media_artist, items, clickListener);
                    }

                    if(!albums.isEmpty()) {
                        items.clear();
                        items.addAll(albums);
                        mediaAdapter = new MediaItemAdapter(getContext(), R.layout.item_media_album, items, clickListener);
                    }

                    if(!collections.isEmpty()) {
                        items.clear();
                        items.addAll(collections);
                        mediaAdapter = new MediaItemAdapter(getContext(), R.layout.item_media_collection, items, clickListener);
                    }
                    */

                    adapter.notifyDataSetChanged();



                    // Restore scroll position
                    recyclerView.getLayoutManager().scrollToPosition(lastFirstVisiblePosition);
                }

                @Override
                public void onError(@NonNull String id) {
                    Log.e(TAG, "browse fragment subscription onError, id=" + id);
                }
            };

    private MediaBrowserCompat.ConnectionCallback connectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "onConnected()");

                    if (isDetached()) {
                        return;
                    }

                    mediaId = getMediaId();

                    if (mediaId == null) {
                        return;
                    }

                    if(adapter.getItemCount() == 0) {
                        mediaBrowser.subscribe(mediaId, subscriptionCallback);
                    }
                }

                @Override
                public void onConnectionFailed() {
                    Log.d(TAG, "onConnectionFailed");
                }

                @Override
                public void onConnectionSuspended() {
                    Log.d(TAG, "onConnectionSuspended");
                }
            };

    // Constructor for creating fragment with arguments
    public static SimpleMediaFragment newInstance(String mediaId) {
        SimpleMediaFragment fragment = new SimpleMediaFragment();
        Bundle args = new Bundle();
        args.putString(MediaUtils.EXTRA_MEDIA_ID, mediaId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");

        view = inflater.inflate(R.layout.fragment_simple_media, container, false);

        // Initialisation
        items = new ArrayList<>();
        adapter = new MediaItemAdapter(getContext(), items);
        adapter.setOnClick(this);

        lm = new GridLayoutManager(getContext(), 2, RecyclerView.VERTICAL, false);

        // Initialise UI
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);

        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int viewWidth = recyclerView.getMeasuredWidth();
                        float cardViewWidth = getActivity().getResources().getDimension(R.dimen.card_media_width);
                        int newSpanCount = (int) Math.floor(viewWidth / cardViewWidth);
                        lm.setSpanCount(newSpanCount);
                        lm.requestLayout();
                    }
                });

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(lm);

        // Subscribe to relevant media service callbacks
        mediaBrowser = new MediaBrowserCompat(getActivity(),
                new ComponentName(getActivity(), MediaService.class),
                connectionCallback, null);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(TAG, "onStart()");

        mediaBrowser.connect();
    }

    @Override
    public void onStop() {
        super.onStop();

        Log.d(TAG, "onStop()");

        if(mediaBrowser.isConnected()) {
            mediaBrowser.disconnect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "onPause()");

        // Store scroll position
        lastFirstVisiblePosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Log.d(TAG, "onAttach()");

        mediaFragmentListener = (MediaFragmentListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        Log.d(TAG, "onDetach()");

        mediaFragmentListener = null;
    }

    public String getMediaId() {
        Bundle args = getArguments();

        if (args != null) {
            return args.getString(MediaUtils.EXTRA_MEDIA_ID);
        }

        return null;
    }

    @Override
    public void onItemClicked(int position) {
        Log.d(TAG, "Item selected: " + items.get(position).getMediaId());
        mediaFragmentListener.onMediaItemSelected(items.get(position), MediaUtils.MEDIA_MENU_NONE);
    }

    @Override
    public void onMenuItemClicked(MenuItem item, int position) {
        Log.d(TAG, "Menu Item selected: " + items.get(position).getMediaId());

        if(item.getItemId() == R.id.shuffle) {
            mediaFragmentListener.onMediaItemSelected(items.get(position), MediaUtils.MEDIA_MENU_SHUFFLE);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.d(TAG, "onConfigurationChanged()");

        float viewWidth = newConfig.screenWidthDp * (newConfig.densityDpi / 160f);
        float cardViewWidth = getActivity().getResources().getDimension(R.dimen.card_media_width);
        int newSpanCount = (int) Math.floor(viewWidth / cardViewWidth);
        lm.setSpanCount(newSpanCount);
        lm.requestLayout();
    }
}
