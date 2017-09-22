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
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.h6ah4i.android.widget.advrecyclerview.composedadapter.ComposedAdapter;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.listener.OnListItemClickListener;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.utils.NetworkUtils;
import com.scooter1556.sms.android.views.adapter.HeaderFooterAdapter;
import com.scooter1556.sms.android.views.adapter.MediaFolderAdapter;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends BaseFragment {

    private static final String TAG = "HomeFragment";

    private MediaBrowserCompat mediaBrowser;
    private MediaFragmentListener mediaFragmentListener;

    private CoordinatorLayout coordinatorLayout;
    private RecyclerView recyclerView;
    private ComposedAdapter adapter;
    private MediaFolderAdapter mediaAdapter;
    private List<MediaBrowserCompat.MediaItem> items;
    Snackbar snackbar;

    boolean online = false;

    private final BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isOnline = NetworkUtils.isOnline(context);

            if (isOnline != online) {
                if(!isOnline) {
                    snackbar.show();
                } else if(snackbar.isShown()) {
                    snackbar.dismiss();
                }

                online = isOnline;
            }
        }
    };

    private final MediaBrowserCompat.SubscriptionCallback subscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(@NonNull String parentId,
                                             @NonNull List<MediaBrowserCompat.MediaItem> children) {
                    Log.d(TAG, "onChildrenLoaded() parentId=" + parentId);

                    if(children.isEmpty()) {
                        Log.d(TAG, "Result for " + parentId + " is empty");
                        return;
                    }

                    items.clear();
                    items.addAll(children);

                    mediaAdapter.notifyDataSetChanged();
                    mediaAdapter.setHasStableIds(true);

                    HeaderFooterAdapter headerFooterAdapter = new HeaderFooterAdapter(mediaAdapter);
                    headerFooterAdapter.addHeaderItem(getString(R.string.heading_media_browser));
                    headerFooterAdapter.setupFullSpanForGridLayoutManager(recyclerView);

                    adapter.addAdapter(headerFooterAdapter);
                    adapter.notifyDataSetChanged();
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

                    // Subscribe to media browser event
                    if(mediaAdapter.getItemCount() == 0) {
                        mediaBrowser.subscribe(MediaUtils.MEDIA_ID_FOLDERS, subscriptionCallback);
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
    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");

        OnListItemClickListener clickListener = new OnListItemClickListener() {
            @Override
            public void onItemClicked(MediaBrowserCompat.MediaItem item) {
                Log.d(TAG, "Item selected: " + item.getMediaId());
                mediaFragmentListener.onMediaItemSelected(item);
            }
        };

        // Initialisation
        items = new ArrayList<>();
        adapter = new ComposedAdapter();
        mediaAdapter = new MediaFolderAdapter(getContext(), items, clickListener);

        // Subscribe to relevant media service callbacks
        mediaBrowser = new MediaBrowserCompat(getActivity(),
                new ComponentName(getActivity(), MediaService.class),
                connectionCallback, null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");

        final View view = inflater.inflate(R.layout.fragment_home, container, false);

        coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.coordinatorLayout);
        snackbar = Snackbar.make(coordinatorLayout, "No connection!", Snackbar.LENGTH_INDEFINITE);
        final GridLayoutManager lm = new GridLayoutManager(getContext(), 2, GridLayoutManager.VERTICAL, false);

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

        mediaBrowser.disconnect();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Log.d(TAG, "onAttach()");

        mediaFragmentListener = (MediaFragmentListener) context;

        // Register connectivity receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getActivity().registerReceiver(connectivityChangeReceiver, intentFilter);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        Log.d(TAG, "onDetach()");

        mediaFragmentListener = null;

        // Unregister receiver
        getActivity().unregisterReceiver(connectivityChangeReceiver);
    }
}
