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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v17.leanback.app.PlaybackFragment;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRow.FastForwardAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.PlayPauseAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.RewindAction;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.activity.tv.TvVideoPlaybackActivity;
import com.scooter1556.sms.android.domain.TranscodeProfile;
import com.scooter1556.sms.android.playback.Playback;
import com.scooter1556.sms.android.playback.PlaybackManager;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.utils.AudioUtils;
import com.scooter1556.sms.android.utils.MediaUtils;
import com.scooter1556.sms.android.utils.TrackSelectionUtils;

import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

import cz.msebera.android.httpclient.Header;

import static com.scooter1556.sms.android.utils.MediaUtils.EXTRA_QUEUE_ITEM;

public class TvVideoPlayerFragment extends android.support.v17.leanback.app.PlaybackFragment implements SurfaceHolder.Callback, Playback, ExoPlayer.EventListener, TextRenderer.Output {
    private static final String TAG = "TvVideoPlaybackFragment";

    private static final String CLIENT_ID = "android";
    public static final String USER_AGENT = "SMSAndroidPlayer";
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    static final String FORMAT = "hls";
    static final String SUPPORTED_FILES = "mkv,webm,mp4";
    static final String SUPPORTED_CODECS = "h264,vp8,aac,mp3,vorbis";
    static final String MCH_CODECS = "ac3";

    static final int MAX_SAMPLE_RATE = 48000;

    private static final int CARD_SIZE = 240;

    private static final int BACKGROUND_TYPE = PlaybackFragment.BG_LIGHT;

    private static final int DEFAULT_UPDATE_PERIOD = 1000;

    private static final int DEFAULT_SEEK_DELAY = 1000;
    private static final int DEFAULT_SEEK_SPEED = 10000;
    private static final int MAX_SEEK_SPEED = 300000;

    public static final int ACTION_VIDEO_QUALITY_ID = 0x7f0f0014;
    public static final int ACTION_AUDIO_TRACK_ID = 0x7f0f0015;
    public static final int ACTION_SUBTITLE_TRACK_ID = 0x7f0f0016;

    private MediaSessionCompat.QueueItem currentMedia;
    private volatile long currentPosition = 0;
    private volatile String currentMediaID;
    private volatile UUID currentJobId;
    private UUID sessionId;
    private int playbackState;

    private Callback callback;
    private PlaybackManager playbackManager;

    private SimpleExoPlayer mediaPlayer;
    private DefaultTrackSelector trackSelector;
    private TrackSelectionUtils trackSelectionUtils;
    private TrackGroupArray lastSeenTrackGroupArray;

    // Override automatic controller visibility
    private boolean initialised = false;
    private boolean ready = false;
    private boolean paused = false;

    private ArrayObjectAdapter rowsAdapter;
    private ArrayObjectAdapter primaryActionsAdapter;
    private ArrayObjectAdapter secondaryActionsAdapter;
    private FastForwardAction fastForwardAction;
    private PlayPauseAction playPauseAction;
    private RewindAction rewindAction;
    private VideoQualityAction videoQualityAction;
    private AudioTrackAction audioTrackAction;
    private SubtitleTrackAction subtitleTrackAction;
    private PlaybackControlsRow playbackControlsRow;
    private Handler handler;
    private Runnable runnable;

    private SubtitleView subtitleView;

    // Seek
    private Handler seekHandler;
    private Runnable seekRunnable;
    private int seekSpeed = DEFAULT_SEEK_SPEED;
    private boolean seekInProgress = false;

    private SurfaceHolder surfaceHolder;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        surfaceHolder = ((TvVideoPlaybackActivity) getActivity()).getSurfaceHolder();
        surfaceHolder.addCallback(this);

        subtitleView = (SubtitleView) getActivity().findViewById(R.id.subtitle_view);
        if (subtitleView != null) {
            subtitleView.setUserDefaultStyle();
            subtitleView.setUserDefaultTextSize();
        }

        handler = new Handler();
        seekHandler = new Handler();

        Bundle bundle = getActivity().getIntent().getExtras();
        if (bundle != null) {
            currentMedia = getActivity().getIntent().getParcelableExtra(EXTRA_QUEUE_ITEM);
        }

        this.playbackState = PlaybackStateCompat.STATE_NONE;

        // Set playback instance in our playback manager
        playbackManager = PlaybackManager.getInstance();
        playbackManager.setPlayback(this);

        // Set playback manager callback
        this.setCallback(playbackManager);

        setBackgroundType(BACKGROUND_TYPE);
        setFadingEnabled(false);
        setupRows();

        // Get session ID
        RESTService.getInstance().createSession(new TextHttpResponseHandler()  {

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String response, Throwable throwable) {
                Log.e(TAG, "Failed to initialise session");
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String response) {
                // Parse result
                sessionId = UUID.fromString(response);
                Log.d(TAG, "New session ID: " + sessionId);

                // Start Playback
                play(currentMedia);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                        if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
                            pause();
                        } else {
                            play(currentMedia);
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        stopProgressAutomation();

        // Stop playback
        stop(true);

        // Remove reference to playback in our playback manager
        playbackManager.setPlayback(null);

        // End session if needed
        if(sessionId != null) {
            RESTService.getInstance().endSession(sessionId);
        }

        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();

        stopProgressAutomation();
        setFadingEnabled(false);
        pause();
    }

    private void setupRows() {
        ClassPresenterSelector presenter = new ClassPresenterSelector();

        PlaybackControlsRowPresenter playbackControlsRowPresenter;
        playbackControlsRowPresenter = new PlaybackControlsRowPresenter(new DescriptionPresenter());

        playbackControlsRowPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            public void onActionClicked(Action action) {
                if (action.getId() == playPauseAction.getId()) {
                    if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
                        pause();
                    } else {
                        play(currentMedia);
                    }
                } else if (action.getId() == fastForwardAction.getId()) {
                    if(seekInProgress) {
                        if(seekSpeed > 0) {
                            seekSpeed = seekSpeed * 2;
                            if(seekSpeed > MAX_SEEK_SPEED) {
                                seekSpeed = MAX_SEEK_SPEED;
                            }
                        } else {
                            seekSpeed = DEFAULT_SEEK_SPEED;
                        }
                    } else {
                        seekSpeed = DEFAULT_SEEK_SPEED;
                        startSeeking();
                    }
                } else if (action.getId() == rewindAction.getId()) {
                    if(seekInProgress) {
                        if(seekSpeed < 0) {
                            seekSpeed = seekSpeed * 2;
                            if(seekSpeed < (MAX_SEEK_SPEED * -1)) {
                                seekSpeed = (MAX_SEEK_SPEED * -1);
                            }
                        } else {
                            seekSpeed = (DEFAULT_SEEK_SPEED * -1);
                        }
                    } else {
                        seekSpeed = (DEFAULT_SEEK_SPEED * -1);
                        startSeeking();
                    }
                } else if(action.getId() == videoQualityAction.getId()) {
                    MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

                    if (mappedTrackInfo != null) {
                        trackSelectionUtils.showSelectionDialog(getActivity(), trackSelector.getCurrentMappedTrackInfo(), videoQualityAction.getIndex());
                    }
                } else if(action.getId() == audioTrackAction.getId()) {
                    MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

                    if (mappedTrackInfo != null) {
                        trackSelectionUtils.showSelectionDialog(getActivity(), trackSelector.getCurrentMappedTrackInfo(), audioTrackAction.getIndex());
                    }
                } else if(action.getId() == subtitleTrackAction.getId()) {
                    MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

                    if (mappedTrackInfo != null) {
                        trackSelectionUtils.showSelectionDialog(getActivity(), trackSelector.getCurrentMappedTrackInfo(), subtitleTrackAction.getIndex());
                    }
                }
            }
        });

        presenter.addClassPresenter(PlaybackControlsRow.class, playbackControlsRowPresenter);
        presenter.addClassPresenter(ListRow.class, new ListRowPresenter());
        rowsAdapter = new ArrayObjectAdapter(presenter);

        addPlaybackControlsRow();

        setAdapter(rowsAdapter);
    }

    private void addPlaybackControlsRow() {
        playbackControlsRow = new PlaybackControlsRow(currentMedia);
        playbackControlsRow.setCurrentTime(0);
        playbackControlsRow.setTotalTime(Double.valueOf(currentMedia.getDescription().getExtras().getDouble("Duration", 0)).intValue() * 1000);

        rowsAdapter.add(playbackControlsRow);
        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        primaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        secondaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        playbackControlsRow.setPrimaryActionsAdapter(primaryActionsAdapter);
        playbackControlsRow.setSecondaryActionsAdapter(secondaryActionsAdapter);

        playPauseAction = new PlayPauseAction(getActivity());
        fastForwardAction = new FastForwardAction(getActivity());
        rewindAction = new RewindAction(getActivity());

        // Add main controls to primary adapter.
        primaryActionsAdapter.add(rewindAction);
        primaryActionsAdapter.add(playPauseAction);
        primaryActionsAdapter.add(fastForwardAction);

        updateButtonVisibilities();
        updateVideoImage();
    }

    private void startProgressAutomation() {
        if (runnable == null) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    int currentTime = (int) mediaPlayer.getCurrentPosition();
                    playbackControlsRow.setCurrentTime(currentTime);

                    handler.postDelayed(this, DEFAULT_UPDATE_PERIOD);
                }
            };

            handler.postDelayed(runnable, DEFAULT_UPDATE_PERIOD);
        }
    }

    private void stopProgressAutomation() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
            runnable = null;
        }
    }

    private void startSeeking() {
        Log.d(TAG, "startSeeking()");

        if(playbackState != PlaybackStateCompat.STATE_PLAYING && playbackState != PlaybackStateCompat.STATE_PAUSED) {
            return;
        }

        if(playbackState == PlaybackStateCompat.STATE_PLAYING) {
            pause();
        }

        seekInProgress = true;
        setFadingEnabled(false);
        stopProgressAutomation();

        if (seekRunnable == null) {
            seekRunnable = new Runnable() {
                @Override
                public void run() {
                    int currentTime = playbackControlsRow.getCurrentTime() + seekSpeed;

                    if(currentTime < 0) {
                        currentPosition= 0;
                        playbackControlsRow.setCurrentTime(0);
                    } else if(currentTime > playbackControlsRow.getTotalTime()) {
                        getActivity().finish();
                    } else {
                        currentPosition = currentTime;
                        playbackControlsRow.setCurrentTime(currentTime);
                        seekHandler.postDelayed(this, DEFAULT_SEEK_DELAY);
                    }
                }
            };

            seekHandler.postDelayed(seekRunnable, DEFAULT_SEEK_DELAY);
        }
    }

    private void stopSeeking() {
        Log.d(TAG, "stopSeeking()");

        if (seekHandler != null && seekRunnable != null) {
            seekHandler.removeCallbacks(seekRunnable);
            seekRunnable = null;
            seekInProgress = false;
        }
    }

    private void updateVideoImage() {
        Glide.with(getActivity())
                .asBitmap()
                .load(RESTService.getInstance().getConnection().getUrl() + "/image/" + currentMediaID + "/cover?scale=" + CARD_SIZE)
                .into(new SimpleTarget<Bitmap>(CARD_SIZE, CARD_SIZE) {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        playbackControlsRow.setImageBitmap(getActivity(), resource);
                        rowsAdapter.notifyArrayItemRangeChanged(0, rowsAdapter.size());
                    }
                });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Surface is ready
        ready = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Do nothing.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }

    @Override
    public void start() {
        Log.d(TAG, "start()");
    }

    @Override
    public void stop(boolean notifyListeners) {
        Log.d(TAG, "stop()");

        playbackState = PlaybackStateCompat.STATE_STOPPED;

        if (notifyListeners && callback != null) {
            callback.onPlaybackStatusChanged(playbackState);
        }

        currentPosition = getCurrentStreamPosition();

        // Relax all resources
        relaxResources(true);
    }

    @Override
    public void destroy() {
        getActivity().finish();
    }

    @Override
    public void setState(int state) {
        playbackState = state;
    }

    @Override
    public int getState() {
        return playbackState;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.getPlayWhenReady();
    }

    @Override
    public long getCurrentStreamPosition() {
        return mediaPlayer != null ?
                mediaPlayer.getCurrentPosition() : currentPosition;
    }

    @Override
    public void setCurrentStreamPosition(long pos) {
        this.currentPosition = pos;
    }

    @Override
    public void updateLastKnownStreamPosition() {
        if (mediaPlayer != null) {
            currentPosition = mediaPlayer.getCurrentPosition();
        }
    }

    @Override
    public void play(MediaSessionCompat.QueueItem item) {
        Log.d(TAG, "Play media item with id " + item.getDescription().getMediaId());

        boolean mediaHasChanged = !TextUtils.equals(item.getDescription().getMediaId(), currentMediaID);

        if (mediaHasChanged) {
            currentPosition = 0;
            currentMediaID = item.getDescription().getMediaId();
        }

        if (playbackState == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged && mediaPlayer != null) {
            configMediaPlayerState();
        } else {
            playbackState = PlaybackStateCompat.STATE_STOPPED;
            relaxResources(false);
            initialiseStream();
        }
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause()");

        if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            if (mediaPlayer != null && mediaPlayer.getPlayWhenReady()) {
                mediaPlayer.setPlayWhenReady(false);
                currentPosition = mediaPlayer.getCurrentPosition();
                playbackState = PlaybackStateCompat.STATE_PAUSED;
            }

            // While paused, retain the Media Player but give up audio focus
            relaxResources(false);
        }

        if (callback != null) {
            callback.onPlaybackStatusChanged(playbackState);
        }
    }

    @Override
    public void seekTo(long position) {
        Log.d(TAG, "seek(" + position + ")");

        if (mediaPlayer == null) {
            // If we do not have a current media player, simply update the current position
            currentPosition = position;
        } else {
            if (mediaPlayer.getPlayWhenReady()) {
                playbackState = PlaybackStateCompat.STATE_BUFFERING;
            }

            mediaPlayer.seekTo(position);

            if (callback != null) {
                callback.onPlaybackStatusChanged(playbackState);
            }
        }

        stopSeeking();
    }

    @Override
    public void setCurrentMediaId(String mediaId) {
        this.currentMediaID = mediaId;
    }

    @Override
    public String getCurrentMediaId() {
        return currentMediaID;
    }

    @Override
    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public UUID getSessionId() {
        return sessionId;
    }

    @Override
    public void setCurrentJobId(UUID jobId) {
        this.currentJobId = jobId;
    }

    @Override
    public UUID getCurrentJobId() {
        return currentJobId;
    }

    @Override
    public SimpleExoPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private void updateButtonVisibilities() {
        Log.d(TAG, "updateButtonVisibilities()");

        if (mediaPlayer == null) {
            return;
        }

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

        if (mappedTrackInfo == null) {
            return;
        }

        // Clear buttons from secondary actions
        secondaryActionsAdapter.clear();

        for (int i = 0; i < mappedTrackInfo.length; i++) {
            // Flags
            boolean video = false;
            boolean audio = false;
            boolean subtitles = false;

            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);

            if (trackGroups.length != 0) {
                switch (mediaPlayer.getRendererType(i)) {

                    case C.TRACK_TYPE_AUDIO:
                        if(!audio) {
                            audioTrackAction = new AudioTrackAction(getActivity(), i);
                            secondaryActionsAdapter.add(audioTrackAction);
                            audio = true;
                        }

                        break;

                    case C.TRACK_TYPE_VIDEO:
                        if(!video) {
                            videoQualityAction = new VideoQualityAction(getActivity(), i);
                            secondaryActionsAdapter.add(videoQualityAction);
                            video = true;
                        }

                        break;

                    case C.TRACK_TYPE_TEXT:
                        if(!subtitles) {
                            subtitleTrackAction = new SubtitleTrackAction(getActivity(), i);
                            secondaryActionsAdapter.add(subtitleTrackAction);
                            subtitles = true;
                        }

                        break;

                    default:
                        continue;
                }
            }
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        updateButtonVisibilities();

        if (trackGroups != lastSeenTrackGroupArray) {
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

            if (mappedTrackInfo != null) {
                if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO) == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                    Toast.makeText(getActivity(), R.string.error_unsupported_video, Toast.LENGTH_LONG).show();
                }
                if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO) == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                    Toast.makeText(getActivity(), R.string.error_unsupported_audio, Toast.LENGTH_LONG).show();
                }
            }

            lastSeenTrackGroupArray = trackGroups;
        }
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        Log.d(TAG, "onLoadingChanged(" + isLoading + ")");

        if(!isLoading && playbackState != PlaybackStateCompat.STATE_PAUSED) {
            initialised = true;
            configMediaPlayerState();
        }
    }

    private void configMediaPlayerState() {
        String state = "?";
        int oldState = playbackState;

        if (mediaPlayer != null && !mediaPlayer.getPlayWhenReady()) {
            mediaPlayer.setPlayWhenReady(true);

            if (currentPosition == mediaPlayer.getCurrentPosition()) {
                playbackState = PlaybackStateCompat.STATE_PLAYING;

                state = "Playing";
            } else {
                seekTo(currentPosition);

                state = "Seeking to " + currentPosition;
            }
        }

        if (callback != null) {
            callback.onPlaybackStatusChanged(playbackState);
        }

        Log.d(TAG, "configMediaPlayerState() > " + state);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int state) {
        switch(state) {
            case ExoPlayer.STATE_BUFFERING:
                Log.d(TAG, "onPlayerStateChanged(BUFFERING)");

                // Update state
                playbackState = PlaybackStateCompat.STATE_BUFFERING;

                break;

            case ExoPlayer.STATE_ENDED:
                Log.d(TAG, "onPlayerStateChanged(ENDED)");

                // End job if required
                if (currentJobId != null) {
                    Log.d(TAG, "Ending job with id " + currentJobId);
                    RESTService.getInstance().endJob(currentJobId);
                    currentJobId = null;
                }

                // The media player finished playing the current item, so we go ahead and start the next.
                if (callback != null) {
                    callback.onCompletion();
                }

                break;

            case ExoPlayer.STATE_IDLE:
                Log.d(TAG, "onPlayerStateChanged(IDLE)");

                break;

            case ExoPlayer.STATE_READY:
                Log.d(TAG, "onPlayerStateChanged(READY)");

                if(playWhenReady) {
                    // Set timeline
                    playbackControlsRow.setCurrentTime((int) mediaPlayer.getCurrentPosition());

                    playbackState = PlaybackStateCompat.STATE_PLAYING;
                    playPauseAction.setIndex(PlayPauseAction.PAUSE);
                    setFadingEnabled(true);
                    getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    startProgressAutomation();
                } else if(initialised) {
                    playbackState = PlaybackStateCompat.STATE_PAUSED;
                    playPauseAction.setIndex(PlayPauseAction.PLAY);
                }

                rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(playbackControlsRow), 1);

                break;

            default:
                break;
        }

        updateButtonVisibilities();

        if (callback != null) {
            callback.onPlaybackStatusChanged(playbackState);
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    private void initialiseStream() {
        // Check session ID
        if(sessionId == null) {
            Log.d(TAG, "Session ID not set, unable to initialise stream!");
            return;
        }

        // Get Media Element ID from Media ID
        List<String> mediaID = MediaUtils.parseMediaId(currentMediaID);

        if(mediaID.size() <= 1) {
            error("Error initialising stream", null);
            return;
        }

        // Get media element ID
        final UUID id = UUID.fromString(mediaID.get(1));

        Log.d(TAG, "Initialising stream for media item with id " + id);

        // Get settings
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // Get quality
        int quality = Integer.parseInt(settings.getString("pref_video_quality", "0"));

        // Audio Capabilities
        AudioCapabilities audioCapabilities = AudioCapabilities.getCapabilities(getActivity().getApplicationContext());
        Log.d(TAG, audioCapabilities.toString());

        // Initialise Stream
        RESTService.getInstance().initialiseStream(getActivity(), sessionId, id, CLIENT_ID, SUPPORTED_FILES, SUPPORTED_CODECS, AudioUtils.getSupportedMchAudioCodecs(audioCapabilities), FORMAT, quality, MAX_SAMPLE_RATE, null, null, settings.getBoolean("pref_direct_play", false), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                try {
                    // Parse profile
                    Gson parser = new Gson();
                    TranscodeProfile profile = parser.fromJson(response.toString(), TranscodeProfile.class);

                    currentJobId = profile.getID();

                    // Build stream URL
                    String url = RESTService.getInstance().getConnection().getUrl() + "/stream/" + profile.getID();

                    createMediaPlayerIfRequired();

                    // Get stream
                    String userAgent = Util.getUserAgent(getActivity(), USER_AGENT);
                    DataSource.Factory dataSource = new DefaultDataSourceFactory(getActivity(), userAgent, BANDWIDTH_METER);
                    ExtractorsFactory extractor = new DefaultExtractorsFactory();

                    MediaSource sampleSource;
                    if(profile.getType() > TranscodeProfile.StreamType.DIRECT) {
                        sampleSource =
                                new HlsMediaSource.Factory(dataSource)
                                        .createMediaSource(Uri.parse(url), new Handler(), null);
                    } else {
                        sampleSource =
                                new ExtractorMediaSource.Factory(dataSource)
                                        .setExtractorsFactory(extractor)
                                        .createMediaSource(Uri.parse(url));
                    }

                    playbackState = PlaybackStateCompat.STATE_BUFFERING;

                    mediaPlayer.prepare(sampleSource);

                    updateButtonVisibilities();

                    if (callback != null) {
                        callback.onPlaybackStatusChanged(playbackState);
                    }
                } catch (Exception e) {
                    error("Error initialising stream", e);
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject response) {
                error("Error initialising stream", null);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String error, Throwable throwable) {
                error("Error initialising stream", null);
            }
        });
    }

    private void error(String message, Exception e) {
        Log.e(TAG, message, e);

        if (callback != null) {
            callback.onError(message);
        }
    }

    public void createMediaPlayerIfRequired() {
        Log.d(TAG, "createMediaPlayerIfRequired()");

        TrackSelection.Factory adaptiveTrackSelectionFactory = new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
        trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
        trackSelectionUtils = new TrackSelectionUtils(trackSelector, adaptiveTrackSelectionFactory);
        lastSeenTrackGroupArray = null;

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Create player
        mediaPlayer = ExoPlayerFactory.newSimpleInstance(getActivity(), trackSelector);
        mediaPlayer.setVideoSurfaceHolder(surfaceHolder);
        mediaPlayer.addListener(this);
        mediaPlayer.setTextOutput(this);
    }


    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the Media Player.

     */
    private void relaxResources(boolean releaseMediaPlayer) {
        Log.d(TAG, "relaxResources()");

        // Stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            trackSelector = null;
            trackSelectionUtils = null;
        }

        // End job if required
        if(releaseMediaPlayer && currentJobId != null) {
            Log.d(TAG, "Ending job with id " + currentJobId);
            RESTService.getInstance().endJob(currentJobId);
            currentJobId = null;
        }

        stopProgressAutomation();
    }

    @Override
    public void onCues(List<Cue> cues) {
        if (subtitleView != null)
            subtitleView.onCues(cues);
    }

    private static class VideoQualityAction extends Action {
        private int index;

        /**
         * Constructor
         *
         * @param context Context used for loading resources.
         */
        private VideoQualityAction(Context context, int index) {
            super(ACTION_VIDEO_QUALITY_ID);
            setIcon(ContextCompat.getDrawable(context, R.drawable.ic_settings_white_48dp));
            setLabel1(context.getString(R.string.action_bar_video_quality));
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    private static class AudioTrackAction extends Action {
        private int index;

        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        private AudioTrackAction(Context context, int index) {
            super(ACTION_AUDIO_TRACK_ID);
            setIcon(ContextCompat.getDrawable(context, R.drawable.ic_surround_sound_white_48dp));
            setLabel1(context.getString(R.string.action_bar_audio_track));
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    private static class SubtitleTrackAction extends Action {
        private int index;

        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        private SubtitleTrackAction(Context context, int index) {
            super(ACTION_SUBTITLE_TRACK_ID);
            setIcon(ContextCompat.getDrawable(context, R.drawable.ic_subtitles_white_48dp));
            setLabel1(context.getString(R.string.action_bar_subtitle_track));
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    private static final class DescriptionPresenter extends AbstractDetailsDescriptionPresenter {
        @Override
        protected void onBindDescription(ViewHolder viewHolder, Object item) {
           MediaSessionCompat.QueueItem qItem = ((MediaSessionCompat.QueueItem) item);

            if(qItem != null) {
                viewHolder.getTitle().setText(qItem.getDescription().getTitle());
            }
        }
    }
}
