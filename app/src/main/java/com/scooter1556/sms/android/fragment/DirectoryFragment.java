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

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * A Fragment that lists the contents of a directory
 */
public class DirectoryFragment extends BaseFragment {

    /*private static final String TAG = "DirectoryFragment";

    private static final String ARG_MEDIA_ID = "media_id";

    private MediaBrowserCompat mediaBrowser;
    private RecyclerViewAdapter sectionAdapter;
    private MediaFragmentListener mediaFragmentListener;

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

                    List<MediaBrowserCompat.MediaItem> dirs = new ArrayList<>();
                    List<MediaBrowserCompat.MediaItem> audio_dirs = new ArrayList<>();
                    List<MediaBrowserCompat.MediaItem> video_dirs = new ArrayList<>();

                    // Sort child elements
                    for(MediaBrowserCompat.MediaItem item : children) {
                        switch(MediaUtils.getMediaID(item.getMediaId())) {
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
                                break;

                            case MediaService.MEDIA_ID_AUDIO:
                                break;
                        }
                    }

                    // Add sections as required
                    if(!audio_dirs.isEmpty()) {
                        sectionAdapter.addSection(new MediaSection(R.layout.item_media_audio, audio_dirs));
                    }

                    if(!video_dirs.isEmpty()) {
                        sectionAdapter.addSection(new MediaSection(R.layout.item_media_video, video_dirs));
                    }

                    if(!dirs.isEmpty()) {
                        sectionAdapter.addSection(new MediaSection(R.layout.item_media_audio, dirs));
                    }

                    sectionAdapter.notifyDataSetChanged();
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

                    // Subscribe to media browser event
                    mediaBrowser.subscribe(mediaId, subscriptionCallback);
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
    public static DirectoryFragment newInstance(String mediaId) {
        DirectoryFragment fragment = new DirectoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MEDIA_ID, mediaId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Log.d(TAG, "onAttach()");

        mediaFragmentListener = (MediaFragmentListener) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");

        View view = inflater.inflate(R.layout.fragment_simple_media, container, false);

        // Add sections adapter for home screen
        sectionAdapter = new SectionedRecyclerViewAdapter();

        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);

        final GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int viewWidth = recyclerView.getMeasuredWidth();
                        float cardViewWidth = getActivity().getResources().getDimension(R.dimen.card_media_width);
                        int newSpanCount = (int) Math.floor(viewWidth / cardViewWidth);
                        layoutManager.setSpanCount(newSpanCount);
                        layoutManager.requestLayout();
                    }
                });

        recyclerView.setAdapter(sectionAdapter);

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

        mediaBrowser.disconnect();
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
            return args.getString(ARG_MEDIA_ID);
        }

        return null;
    }

    class MediaSection extends StatelessSection {
        List<MediaBrowserCompat.MediaItem> list;

        public MediaSection(int itemResourceId, List<MediaBrowserCompat.MediaItem> list) {
            super(itemResourceId);

            this.list = list;
        }

        @Override
        public int getContentItemsTotal() {
            return list.size();
        }

        @Override
        public RecyclerView.ViewHolder getItemViewHolder(View view) {
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
            final ItemViewHolder itemHolder = (ItemViewHolder) holder;
            final MediaBrowserCompat.MediaItem item = list.get(position);

            if(list.get(position) != null) {
                CharSequence title = item.getDescription().getTitle();
                CharSequence subtitle = item.getDescription().getSubtitle();

                itemHolder.title.setText(title);

                if(itemHolder.subtitle != null) {
                    itemHolder.subtitle.setText(subtitle);
                }

                Glide.with(getActivity())
                        .load(item.getDescription().getIconUri())
                        .asBitmap()
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap bitmap, GlideAnimation glideAnimation) {
                                itemHolder.image.setImageBitmap( bitmap );
                            }

                            @Override
                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                switch(MediaUtils.getMediaID(item.getMediaId())) {

                                    case MediaService.MEDIA_ID_DIRECTORY_AUDIO:
                                        itemHolder.image.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_album));
                                        break;

                                    case MediaService.MEDIA_ID_DIRECTORY_VIDEO:
                                        itemHolder.image.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_video));
                                        break;

                                    default:
                                        itemHolder.image.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_folder));
                                        break;
                                }
                            }
                        });
            }

            itemHolder.rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getActivity(), String.format("Clicked on position %s",
                            list.get(sectionAdapter.getSectionPosition(itemHolder.getAdapterPosition())).getMediaId()),
                            Toast.LENGTH_SHORT).show();

                    mediaFragmentListener.onMediaItemSelected(list.get(sectionAdapter.getSectionPosition(itemHolder.getAdapterPosition())));
                }
            });
        }
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private final View rootView;
        private final ImageView image;
        private final TextView title;
        private final TextView subtitle;

        public ItemViewHolder(View view) {
            super(view);

            rootView = view;
            image = (ImageView) view.findViewById(R.id.image);
            title = (TextView) view.findViewById(R.id.title);
            subtitle = (TextView) view.findViewById(R.id.subtitle);
        }
    }*/
}
