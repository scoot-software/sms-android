package com.scooter1556.sms.android.glue;

import android.content.Context;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;
import com.scooter1556.sms.android.action.AudioTrackAction;
import com.scooter1556.sms.android.action.TextTrackAction;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;

public class LeanbackPlayerGlue extends PlaybackTransportControlGlue<LeanbackPlayerAdapter> {

    private static final String TAG = "LeanbackPlayerGlue";

    private static final long SKIP_DURATION = TimeUnit.SECONDS.toMillis(30);

    public static final int ACTION_PREVIOUS = 0;
    public static final int ACTION_NEXT = 1;
    public static final int ACTION_AUDIO_TRACK = 2;
    public static final int ACTION_TEXT_TRACK = 3;

    private PlaybackControlsRow.SkipPreviousAction skipPreviousAction;
    private PlaybackControlsRow.SkipNextAction skipNextAction;
    private PlaybackControlsRow.FastForwardAction fastForwardAction;
    private PlaybackControlsRow.RewindAction rewindAction;
    public AudioTrackAction audioTrackAction;
    public TextTrackAction textTrackAction;

    private ActionListener listener;

    private int mode;

    /**
     * Listener for actions.
     */
    public interface ActionListener {

        /**
         * Called when an unhandled action is clicked
         */
        void onActionClicked(int action);

    }

    public LeanbackPlayerGlue(Context context, int mode, LeanbackPlayerAdapter playerAdapter, ActionListener listener) {
        super(context, playerAdapter);

        this.mode = mode;

        skipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(context);
        skipNextAction = new PlaybackControlsRow.SkipNextAction(context);
        fastForwardAction = new PlaybackControlsRow.FastForwardAction(context);
        rewindAction = new PlaybackControlsRow.RewindAction(context);

        if(mode == Mode.VIDEO) {
            audioTrackAction = new AudioTrackAction(context, C.INDEX_UNSET);
            textTrackAction = new TextTrackAction(context, C.INDEX_UNSET);
        }

        this.listener = listener;
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter adapter) {
        super.onCreatePrimaryActions(adapter);

        adapter.add(skipPreviousAction);
        adapter.add(rewindAction);
        adapter.add(fastForwardAction);
        adapter.add(skipNextAction);
    }

    @Override
    protected void onCreateSecondaryActions(ArrayObjectAdapter adapter) {
        super.onCreateSecondaryActions(adapter);

        if(mode == Mode.VIDEO) {
            adapter.add(audioTrackAction);
            adapter.add(textTrackAction);
        }
    }

    @Override
    public void onActionClicked(Action action) {
        if (shouldDispatchAction(action)) {
            dispatchAction(action);
            return;
        }

        // Super class handles play/pause and delegates to abstract methods next()/previous().
        super.onActionClicked(action);
    }

    // Should dispatch actions that the super class does not supply callbacks for.
    private boolean shouldDispatchAction(Action action) {
        return action == skipPreviousAction
                || action == skipNextAction
                || action == rewindAction
                || action == fastForwardAction
                || action == audioTrackAction
                || action == textTrackAction;
    }

    private void dispatchAction(Action action) {
        if (action == rewindAction) {
            rewind();
        } else if (action == fastForwardAction) {
            fastForward();
        } else if (action == skipNextAction) {
            listener.onActionClicked(ACTION_NEXT);
        } else if (action == skipPreviousAction) {
            listener.onActionClicked(ACTION_PREVIOUS);
        } else if (action == audioTrackAction) {
            listener.onActionClicked(ACTION_AUDIO_TRACK);
        } else if (action == textTrackAction) {
            listener.onActionClicked(ACTION_TEXT_TRACK);
        }
    }

    /** Skips backwards */
    public void rewind() {
        long newPosition = getCurrentPosition() - SKIP_DURATION;
        newPosition = (newPosition < 0) ? 0 : newPosition;
        getPlayerAdapter().seekTo(newPosition);
    }

    /** Skips forward */
    public void fastForward() {
        if (getDuration() > -1) {
            long newPosition = getCurrentPosition() + SKIP_DURATION;
            newPosition = (newPosition > getDuration()) ? getDuration() : newPosition;
            getPlayerAdapter().seekTo(newPosition);
        }
    }

    //
    // Listener
    //

    public void setListener(@NonNull ActionListener listener) {
        this.listener = listener;
    }

    public void removeListener(){
        this.listener=null;
    }

    public static class Mode {
        public static final int AUDIO = 0;
        public static final int VIDEO = 1;
    }
}
