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
package com.scooter1556.sms.android.fragment.tv;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.PlaybackFragment;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRow.PlayPauseAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.SkipNextAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.SkipPreviousAction;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.presenter.MediaDescriptionPresenter;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.MediaUtils;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TvAudioPlayerFragment extends android.support.v17.leanback.app.PlaybackFragment implements PopupMenu.OnMenuItemClickListener {
    private static final String TAG = "TvAudioPlayerFragment";

    private static final int BACKGROUND_TYPE = PlaybackFragment.BG_DARK;

    public static final int ACTION_STOP_ID = 0x7f0f0012;
    public static final int ACTION_CLEAR_PLAYLIST_ID = 0x7f0f0013;

    private static final int THUMB_WIDTH = 274;
    private static final int THUMB_HEIGHT = 274;

    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

    private ArrayObjectAdapter rowsAdapter;
    private ArrayObjectAdapter primaryActionsAdapter;
    private ArrayObjectAdapter secondaryActionsAdapter;
    private PlaybackControlsRow playbackControlsRow;

    protected PlayPauseAction playPauseAction;
    private StopAction stopAction;
    private SkipNextAction skipNextAction;
    private SkipPreviousAction skipPreviousAction;
    private ClearPlaylistAction clearPlaylistAction;

    private Handler handler;
    private Runnable runnable;

    private BackgroundManager backgroundManager;
    private Drawable defaultBackground;
    private DisplayMetrics displayMetrics;

    private ArrayObjectAdapter listRowAdapter;
    private ListRow listRow;

    private MediaDescriptionCompat selectedItem;

    private ClassPresenterSelector presenterSelector;

    private PlaybackStateCompat lastPlaybackState;
    private final Runnable updateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> scheduleFuture;

    private MediaBrowserCompat mediaBrowser;
    private static MediaControllerCompat mediaController;

    private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
                updateMediaDescription(metadata.getDescription());
                updateDuration(metadata);
            }
        }
    };

    private final MediaBrowserCompat.ConnectionCallback connectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    try {
                        connectToSession(mediaBrowser.getSessionToken());
                    } catch (RemoteException e) {
                        Log.e(TAG, "Could not connect media controller", e);
                    }
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup background manager
        backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());

        // Get default background
        defaultBackground = ContextCompat.getDrawable(getActivity(), R.drawable.default_background);
        backgroundManager.setDrawable(defaultBackground);

        // Get screen size
        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        handler = new Handler();

        listRowAdapter = new ArrayObjectAdapter(new MediaDescriptionPresenter());
        presenterSelector = new ClassPresenterSelector();
        rowsAdapter = new ArrayObjectAdapter(presenterSelector);

        setBackgroundType(BACKGROUND_TYPE);
        setFadingEnabled(false);

        setupRows();

        // Subscribe to relevant media service callbacks
        mediaBrowser = new MediaBrowserCompat(getActivity(),
                new ComponentName(getActivity(), MediaService.class),
                connectionCallback, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                        if (lastPlaybackState != null) {
                            if (lastPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
                                mediaController.getTransportControls().pause();
                            } else {
                                mediaController.getTransportControls().play();
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        Log.d(TAG, "connectToSession()");

        mediaController = new MediaControllerCompat(getActivity(), token);

        MediaControllerCompat.setMediaController(getActivity(), mediaController);
        mediaController.registerCallback(callback);
        PlaybackStateCompat state = mediaController.getPlaybackState();
        updatePlaybackState(state);
        MediaMetadataCompat metadata = mediaController.getMetadata();

        if (metadata != null) {
            updateMediaDescription(metadata.getDescription());
            updateDuration(metadata);
        }

        updateProgress();

        if (state != null && (state.getState() == PlaybackStateCompat.STATE_PLAYING || state.getState() == PlaybackStateCompat.STATE_BUFFERING)) {
            scheduleSeekbarUpdate();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mediaBrowser != null) {
            mediaBrowser.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mediaBrowser != null) {
            mediaBrowser.disconnect();
        }

        if (mediaController != null) {
            mediaController.unregisterCallback(callback);
        }
    }

    @Override
    public void onDestroy() {
        stopSeekbarUpdate();
        executorService.shutdown();

        super.onDestroy();
    }

    private void setupRows() {
        PlaybackControlsRowPresenter playbackControlsRowPresenter;
        playbackControlsRowPresenter = new PlaybackControlsRowPresenter(new DescriptionPresenter());

        playbackControlsRowPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            public void onActionClicked(Action action) {
                if (action.getId() == playPauseAction.getId()) {
                    if (lastPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
                        mediaController.getTransportControls().pause();
                    } else {
                        mediaController.getTransportControls().play();
                    }
                } else if (action.getId() == skipNextAction.getId()) {
                    mediaController.getTransportControls().skipToNext();
                } else if (action.getId() == skipPreviousAction.getId()) {
                    mediaController.getTransportControls().skipToPrevious();
                } else if (action.getId() == stopAction.getId()) {
                    mediaController.getTransportControls().stop();
                } else if (action.getId() == clearPlaylistAction.getId()) {
                    //TODO
                }
            }
        });

        presenterSelector.addClassPresenter(PlaybackControlsRow.class, playbackControlsRowPresenter);

        playbackControlsRow = new PlaybackControlsRow(new MediaDescriptionHolder());
        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        primaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        secondaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        playbackControlsRow.setPrimaryActionsAdapter(primaryActionsAdapter);
        playbackControlsRow.setSecondaryActionsAdapter(secondaryActionsAdapter);

        playPauseAction = new PlayPauseAction(getActivity());
        stopAction = new StopAction((getActivity()));
        skipNextAction = new PlaybackControlsRow.SkipNextAction(getActivity());
        skipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(getActivity());
        clearPlaylistAction = new ClearPlaylistAction(getActivity());

        primaryActionsAdapter.add(skipPreviousAction);
        primaryActionsAdapter.add(stopAction);
        primaryActionsAdapter.add(playPauseAction);
        primaryActionsAdapter.add(skipNextAction);

        secondaryActionsAdapter.add(clearPlaylistAction);

        rowsAdapter.add(playbackControlsRow);
        setAdapter(rowsAdapter);
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private void resetPlaybackRow() {
        playbackControlsRow.setTotalTime(0);
        playbackControlsRow.setCurrentTime(0);
        playbackControlsRow.setImageDrawable(null);
        rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(playbackControlsRow), 1);

        backgroundManager.setDrawable(defaultBackground);
    }

    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        if (!executorService.isShutdown()) {
            scheduleFuture = executorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            handler.post(updateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL, PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekbarUpdate() {
        if (scheduleFuture != null) {
            scheduleFuture.cancel(false);
        }
    }

    public void updateMediaDescription(MediaDescriptionCompat description) {
        if (description == null) {
            return;
        }

        ((MediaDescriptionHolder) playbackControlsRow.getItem()).item = description;
        rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(playbackControlsRow), 1);

        List<String> id = MediaUtils.parseMediaId(description.getMediaId());

        if(id.size() < 2) {
            return;
        }

        // Get cover
        Glide.with(getActivity())
                .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + id.get(1) + "/cover/" + THUMB_HEIGHT)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>(THUMB_WIDTH, THUMB_HEIGHT) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        playbackControlsRow.setImageBitmap(getActivity(), resource);
                        rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(playbackControlsRow), 1);
                    }
                });

        // Get fanart
        Glide.with(getActivity())
                .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + id.get(1) + "/fanart/" + displayMetrics.widthPixels)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>(displayMetrics.widthPixels, displayMetrics.heightPixels) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap>
                            glideAnimation) {
                        backgroundManager.setBitmap(resource);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        backgroundManager.setDrawable(defaultBackground);
                    }
                });
    }

    private void updateDuration(MediaMetadataCompat metadata) {
        if (metadata == null) {
            return;
        }

        int duration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        playbackControlsRow.setTotalTime(duration);
    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        if (state == null || playbackControlsRow == null) {
            return;
        }

        lastPlaybackState = state;

        switch(state.getState()) {

            case PlaybackStateCompat.STATE_PLAYING:
                scheduleSeekbarUpdate();
                playPauseAction.setIndex(PlayPauseAction.PAUSE);
                break;

            case PlaybackStateCompat.STATE_PAUSED:
                stopSeekbarUpdate();
                playPauseAction.setIndex(PlayPauseAction.PLAY);
                break;

            case PlaybackStateCompat.STATE_NONE:
                stopSeekbarUpdate();
                playPauseAction.setIndex(PlayPauseAction.PLAY);
                resetPlaybackRow();
                break;

            case PlaybackStateCompat.STATE_STOPPED:
                stopSeekbarUpdate();
                playPauseAction.setIndex(PlayPauseAction.PLAY);
                resetPlaybackRow();
                break;

            case PlaybackStateCompat.STATE_BUFFERING:
                stopSeekbarUpdate();
                break;

            default:
                Log.d(TAG, "Unhandled state: " + state.getState());
        }

        /*skipNext.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) == 0
                ? INVISIBLE : VISIBLE );
        skipPrev.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) == 0
                ? INVISIBLE : VISIBLE );
        shuffle.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE_ENABLED) == 0
                ? INVISIBLE : VISIBLE );
        repeat.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SET_REPEAT_MODE) == 0
                ? INVISIBLE : VISIBLE );


        // Custom Actions
        for(PlaybackStateCompat.CustomAction action : state.getCustomActions()) {
            switch (action.getAction()) {
                case MediaService.STATE_SHUFFLE_ON:
                    shuffle.setColorFilter(ContextCompat.getColor(getBaseContext(), R.color.accent));
                    break;

                case MediaService.STATE_SHUFFLE_OFF:
                    shuffle.clearColorFilter();
                    break;

                case MediaService.STATE_REPEAT_NONE:
                    repeat.setImageResource(R.drawable.ic_repeat_white_24dp);
                    repeat.clearColorFilter();
                    break;

                case MediaService.STATE_REPEAT_ALL:
                    repeat.setImageResource(R.drawable.ic_repeat_white_24dp);
                    repeat.setColorFilter(ContextCompat.getColor(getBaseContext(), R.color.accent));
                    break;

                case MediaService.STATE_REPEAT_ONE:
                    repeat.setImageResource(R.drawable.ic_repeat_one_white_24dp);
                    repeat.setColorFilter(ContextCompat.getColor(getBaseContext(), R.color.accent));
                    break;
            }
        }
        */
        rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(playbackControlsRow), 1);
    }

    public void updatePlayListRow() {
        // Get current play queue
        List<MediaSessionCompat.QueueItem> queue = mediaController.getQueue();

        if (queue == null || queue.isEmpty()) {
            // Remove the playlist row if no items are in the playlist
            rowsAdapter.remove(listRow);
            listRow = null;
            resetPlaybackRow();
            return;
        }

        // Reset playlist row
        listRowAdapter.clear();

        // Populate playlist
        for (MediaSessionCompat.QueueItem item : queue) {
            listRowAdapter.add(item.getDescription());
        }

        // Add to interface
        if (listRow == null) {
            HeaderItem header = new HeaderItem(0, getString(R.string.action_bar_view_playlist));
            presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
            listRow = new ListRow(header, listRowAdapter);
            rowsAdapter.add(listRow);
        } else {
            rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(listRow), 1);
        }
    }

    public void showOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(getActivity(), v);

        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.menu_playlist_item);
        popup.show();
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof MediaDescriptionCompat) {
                selectedItem = (MediaDescriptionCompat) item;
                showOptionsMenu(itemViewHolder.view);
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.play:
                if(selectedItem != null) {
                    List<MediaSessionCompat.QueueItem> queue = mediaController.getQueue();

                    if(queue == null || queue.isEmpty()) {
                        Log.d(TAG, "Failed to play media. Unable to retrieve queue.");
                    }

                    // Get queue id
                    for(MediaSessionCompat.QueueItem qItem : queue) {
                        if(qItem.getDescription().equals(selectedItem)) {
                            mediaController.getTransportControls().skipToQueueItem(qItem.getQueueId());
                        }
                    }
                }

                return true;

            case R.id.remove:
                //TODO
                return true;

            default:
                return false;
        }
    }

    private static final class DescriptionPresenter extends AbstractDetailsDescriptionPresenter {
        @Override
        protected void onBindDescription(ViewHolder viewHolder, Object item) {
            MediaDescriptionHolder itemHolder = ((MediaDescriptionHolder) item);

            if(itemHolder.item != null) {
                viewHolder.getTitle().setText(itemHolder.item.getTitle());
                viewHolder.getSubtitle().setText(itemHolder.item.getSubtitle());
            }
        }
    }

    private static final class MediaDescriptionHolder {
        MediaDescriptionCompat item;
        MediaDescriptionHolder() {}
        public MediaDescriptionHolder (MediaDescriptionCompat item) {
            this.item = item;
        }
    }

    private static class StopAction extends Action {
        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        private StopAction(Context context) {
            super(ACTION_STOP_ID);
            setIcon(ContextCompat.getDrawable(context, android.support.v17.leanback.R.drawable.lb_ic_stop));
            setLabel1(context.getString(R.string.action_bar_stop));
            addKeyCode(KeyEvent.KEYCODE_MEDIA_STOP);
        }
    }

    private static class ClearPlaylistAction extends Action {
        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        private ClearPlaylistAction(Context context) {
            super(ACTION_CLEAR_PLAYLIST_ID);
            setIcon(ContextCompat.getDrawable(context, R.drawable.ic_clear_all_white_48dp));
            setLabel1(context.getString(R.string.action_bar_clear));
        }
    }

    private void updateProgress() {
        if (lastPlaybackState == null || playbackControlsRow == null) {
            return;
        }

        long currentPosition = lastPlaybackState.getPosition();

        if (lastPlaybackState.getState() != PlaybackStateCompat.STATE_PAUSED) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensures that we do not repeatedly call the getPlaybackState()
            // on MediaControllerCompat.
            long timeDelta = SystemClock.elapsedRealtime() - lastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * lastPlaybackState.getPlaybackSpeed();
        }

        playbackControlsRow.setCurrentTime((int) (currentPosition));
    }
}
