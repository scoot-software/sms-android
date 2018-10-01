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
import androidx.annotation.NonNull;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.PlaybackFragment;
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.BaseOnItemViewClickedListener;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.ControlButtonPresenterSelector;
import androidx.leanback.widget.OnActionClickedListener;
import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.PlaybackControlsRow.PlayPauseAction;
import androidx.leanback.widget.PlaybackControlsRow.SkipNextAction;
import androidx.leanback.widget.PlaybackControlsRow.SkipPreviousAction;
import androidx.leanback.widget.PlaybackControlsRowPresenter;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowPresenter;
import androidx.core.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.presenter.HeaderPresenter;
import com.scooter1556.sms.android.presenter.QueueItemPresenter;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.service.RESTService;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TvAudioPlayerFragment extends androidx.leanback.app.PlaybackFragment {
    private static final String TAG = "TvAudioPlayerFragment";

    private static final int BACKGROUND_TYPE = PlaybackFragment.BG_DARK;

    public static final int ACTION_STOP_ID = 0x7f0f0012;
    public static final int ACTION_CLEAR_PLAYLIST_ID = 0x7f0f0013;
    public static final int ACTION_SHUFFLE_ID = 0x7f0f0014;
    public static final int ACTION_REPEAT_ID = 0x7f0f0015;

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
    private PlaybackControlsRow.ShuffleAction shuffleAction;
    private PlaybackControlsRow.RepeatAction repeatAction;

    private Handler handler;

    private Drawable defaultBackground;
    private DisplayMetrics displayMetrics;

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

    private String shuffleMode;

    private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            Log.d(TAG, "onPlaybackStateChanged() -> " + state);

            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            Log.d(TAG, "onMetadataChanged()");

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

        handler = new Handler();

        presenterSelector = new ClassPresenterSelector();
        rowsAdapter = new ArrayObjectAdapter(presenterSelector);

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
        updatePlayListRow();

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

        if(!BackgroundManager.getInstance(getActivity()).isAttached()) {
            prepareBackgroundManager();
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

        BackgroundManager.getInstance(getActivity()).release();
    }

    @Override
    public void onDestroy() {
        stopSeekbarUpdate();
        executorService.shutdown();

        super.onDestroy();
    }

    private void prepareBackgroundManager() {
        Log.d(TAG, "prepareBackgroundManager()");

        setBackgroundType(BACKGROUND_TYPE);

        // Setup background manager
        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());

        // Get default background
        defaultBackground = ContextCompat.getDrawable(getActivity(), R.drawable.default_background);
        backgroundManager.setDrawable(defaultBackground);

        // Get screen size
        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    }

    private void setupRows() {
        PlaybackControlsRowPresenter playbackControlsRowPresenter;
        playbackControlsRowPresenter = new PlaybackControlsRowPresenter(new DescriptionPresenter());

        playbackControlsRowPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            public void onActionClicked(Action action) {
                if (action.getId() == playPauseAction.getId()) {
                    if (lastPlaybackState != null) {
                        if (lastPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
                            mediaController.getTransportControls().pause();
                        } else {
                            mediaController.getTransportControls().play();
                        }
                    }
                } else if (action.getId() == skipNextAction.getId()) {
                    mediaController.getTransportControls().skipToNext();
                } else if (action.getId() == skipPreviousAction.getId()) {
                    mediaController.getTransportControls().skipToPrevious();
                } else if (action.getId() == stopAction.getId()) {
                    mediaController.getTransportControls().stop();
                } else if (action.getId() == clearPlaylistAction.getId()) {
                    Log.d(TAG, "onActionClicked -> Clear Playlist");
                    MediaControllerCompat.TransportControls controls = mediaController.getTransportControls();
                    controls.sendCustomAction(MediaService.ACTION_CLEAR_PLAYLIST, null);
                } else if(action.getId() == shuffleAction.getId()) {
                    Log.d(TAG, "onActionClicked -> Shuffle");

                    PlaybackStateCompat state = mediaController.getPlaybackState();
                    MediaControllerCompat.TransportControls controls = mediaController.getTransportControls();

                    if(state == null) {
                        return;
                    }

                    for(PlaybackStateCompat.CustomAction cAction : state.getCustomActions()) {
                        switch (cAction.getAction()) {
                            case MediaService.STATE_SHUFFLE_ON:
                                controls.sendCustomAction(MediaService.STATE_SHUFFLE_ON, null);
                                break;

                            case MediaService.STATE_SHUFFLE_OFF:
                                controls.sendCustomAction(MediaService.STATE_SHUFFLE_OFF, null);
                                break;
                        }
                    }

                    updatePlaybackState(state);
                } else if(action.getId() == repeatAction.getId()) {
                    Log.d(TAG, "onActionClicked -> Repeat");

                    PlaybackStateCompat state = mediaController.getPlaybackState();
                    MediaControllerCompat.TransportControls controls = mediaController.getTransportControls();

                    if(state == null) {
                        return;
                    }

                    for(PlaybackStateCompat.CustomAction cAction : state.getCustomActions()) {
                        switch (cAction.getAction()) {
                            case MediaService.STATE_REPEAT_NONE: case MediaService.STATE_REPEAT_ALL: case MediaService.STATE_REPEAT_ONE:
                                controls.sendCustomAction(cAction.getAction(), null);
                                break;
                        }
                    }

                    updatePlaybackState(state);
                }
            }
        });

        presenterSelector.addClassPresenter(PlaybackControlsRow.class, playbackControlsRowPresenter);
        presenterSelector.addClassPresenter(String.class, new HeaderPresenter());

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
        shuffleAction = new PlaybackControlsRow.ShuffleAction(getActivity());
        repeatAction = new PlaybackControlsRow.RepeatAction(getActivity());


        primaryActionsAdapter.add(skipPreviousAction);
        primaryActionsAdapter.add(stopAction);
        primaryActionsAdapter.add(playPauseAction);
        primaryActionsAdapter.add(skipNextAction);

        secondaryActionsAdapter.add(shuffleAction);
        secondaryActionsAdapter.add(repeatAction);
        secondaryActionsAdapter.add(clearPlaylistAction);

        rowsAdapter.add(playbackControlsRow);

        // Add heading
        rowsAdapter.add(getString(R.string.heading_queue));

        setAdapter(rowsAdapter);
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private void resetPlaybackRow() {
        ((MediaDescriptionHolder) playbackControlsRow.getItem()).item = null;
        playbackControlsRow.setTotalTime(0);
        playbackControlsRow.setCurrentTime(0);
        playbackControlsRow.setImageDrawable(null);
        rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(playbackControlsRow), 1);

        BackgroundManager.getInstance(getActivity()).setDrawable(defaultBackground);
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

        // Get cover
        Glide.with(getActivity())
                .asBitmap()
                .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + description.getMediaId() + "/cover?scale=" + THUMB_HEIGHT)
                .into(new SimpleTarget<Bitmap>(THUMB_WIDTH, THUMB_HEIGHT) {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        playbackControlsRow.setImageBitmap(getActivity(), resource);
                        rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(playbackControlsRow), 1);
                    }
                });

        // Get fanart
        Glide.with(getActivity())
                .asBitmap()
                .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + description.getMediaId() + "/fanart?scale=" + displayMetrics.widthPixels)
                .into(new SimpleTarget<Bitmap>(displayMetrics.widthPixels, displayMetrics.heightPixels) {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        BackgroundManager.getInstance(getActivity()).setBitmap(resource);
                    }

                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                        BackgroundManager.getInstance(getActivity()).setDrawable(defaultBackground);
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
                updatePlayListRow();
                break;

            case PlaybackStateCompat.STATE_STOPPED:
                stopSeekbarUpdate();
                playPauseAction.setIndex(PlayPauseAction.PLAY);
                playbackControlsRow.setCurrentTime(0);
                break;

            case PlaybackStateCompat.STATE_BUFFERING:
                stopSeekbarUpdate();
                break;

            default:
                Log.d(TAG, "Unhandled state: " + state.getState());
        }

        // Custom Actions
        for(PlaybackStateCompat.CustomAction action : state.getCustomActions()) {
            switch (action.getAction()) {
                case MediaService.STATE_SHUFFLE_ON:
                    shuffleAction.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_shuffle_enabled_white_48dp));

                    // Update interface if necessary
                    if(shuffleMode == null || !shuffleMode.equals(MediaService.STATE_SHUFFLE_ON)) {
                        shuffleMode = MediaService.STATE_SHUFFLE_ON;
                        updatePlayListRow();
                    }

                    break;

                case MediaService.STATE_SHUFFLE_OFF:
                    shuffleAction.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_shuffle_white_48dp));

                    // Update interface if necessary
                    if(shuffleMode == null || !shuffleMode.equals(MediaService.STATE_SHUFFLE_OFF)) {
                        shuffleMode = MediaService.STATE_SHUFFLE_OFF;
                        updatePlayListRow();
                    }

                    break;

                case MediaService.STATE_REPEAT_NONE:
                    repeatAction.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_repeat_white_48dp));
                    break;

                case MediaService.STATE_REPEAT_ALL:
                    repeatAction.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_repeat_enable_white_48dp));
                    break;

                case MediaService.STATE_REPEAT_ONE:
                    repeatAction.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_repeat_one_white_48dp));
                    break;
            }
        }

        rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(playbackControlsRow), 1);
    }

    public void updatePlayListRow() {
        // Get current play queue
        List<MediaSessionCompat.QueueItem> queue = mediaController.getQueue();

        if (queue == null || queue.isEmpty()) {
            resetPlaybackRow();
            rowsAdapter.removeItems(2, rowsAdapter.size());
            return;
        }

        presenterSelector.addClassPresenter(MediaSessionCompat.QueueItem.class, new QueueItemPresenter());

        rowsAdapter.removeItems(2, rowsAdapter.size());

        // Populate playlist
        for (MediaSessionCompat.QueueItem item : queue) {
            rowsAdapter.add(item);
        }
    }

    private final class ItemViewClickedListener implements BaseOnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Object row) {
            if (row instanceof MediaSessionCompat.QueueItem) {
                Log.d(TAG, "onItemClicked() -> " + ((MediaSessionCompat.QueueItem) row).getQueueId());

                mediaController.getTransportControls().skipToQueueItem(((MediaSessionCompat.QueueItem) row).getQueueId());
            }
        }
    }

    private static final class DescriptionPresenter extends AbstractDetailsDescriptionPresenter {
        @Override
        protected void onBindDescription(ViewHolder viewHolder, Object item) {
            MediaDescriptionHolder itemHolder = ((MediaDescriptionHolder) item);

            if(itemHolder.item == null) {
                viewHolder.getTitle().setText("");
                viewHolder.getSubtitle().setText("");
            } else {
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
            setIcon(ContextCompat.getDrawable(context, androidx.leanback.R.drawable.lb_ic_stop));
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

    private static class ShuffleAction extends Action {
        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        private ShuffleAction(Context context) {
            super(ACTION_SHUFFLE_ID);
            setIcon(ContextCompat.getDrawable(context, R.drawable.ic_shuffle_white_48dp));
            setLabel1(context.getString(R.string.description_shuffle_playlist));
        }
    }

    private static class RepeatAction extends Action {
        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        private RepeatAction(Context context) {
            super(ACTION_REPEAT_ID);
            setIcon(ContextCompat.getDrawable(context, R.drawable.ic_repeat_white_48dp));
            setLabel1(context.getString(R.string.description_repeat));
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
