package com.scooter1556.sms.android.glue;

import android.content.Context;

import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;

import java.util.concurrent.TimeUnit;

import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;

public class VideoPlayerGlue extends PlaybackTransportControlGlue<LeanbackPlayerAdapter> {

    private static final long TEN_SECONDS = TimeUnit.SECONDS.toMillis(10);

    private PlaybackControlsRow.SkipPreviousAction skipPreviousAction;
    private PlaybackControlsRow.SkipNextAction skipNextAction;
    private PlaybackControlsRow.FastForwardAction fastForwardAction;
    private PlaybackControlsRow.RewindAction rewindAction;

    public VideoPlayerGlue(
            Context context,
            LeanbackPlayerAdapter playerAdapter) {
        super(context, playerAdapter);

        skipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(context);
        skipNextAction = new PlaybackControlsRow.SkipNextAction(context);
        fastForwardAction = new PlaybackControlsRow.FastForwardAction(context);
        rewindAction = new PlaybackControlsRow.RewindAction(context);
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

        // Nothing for now...
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
        return action == rewindAction
                || action == fastForwardAction;
    }

    private void dispatchAction(Action action) {
        // Primary actions are handled manually.
        if (action == rewindAction) {
            rewind();
        } else if (action == fastForwardAction) {
            fastForward();
        } else if (action instanceof PlaybackControlsRow.MultiAction) {
            PlaybackControlsRow.MultiAction multiAction = (PlaybackControlsRow.MultiAction) action;
            multiAction.nextIndex();
            // Notify adapter of action changes to handle secondary actions, such as, thumbs up/down
            // and repeat.
            notifyActionChanged(
                    multiAction,
                    (ArrayObjectAdapter) getControlsRow().getSecondaryActionsAdapter());
        }
    }

    private void notifyActionChanged(PlaybackControlsRow.MultiAction action, ArrayObjectAdapter adapter) {
        if (adapter != null) {
            int index = adapter.indexOf(action);
            if (index >= 0) {
                adapter.notifyArrayItemRangeChanged(index, 1);
            }
        }
    }

    @Override
    public void next() {
        getPlayerAdapter().next();
    }

    @Override
    public void previous() {
        getPlayerAdapter().previous();
    }

    /** Skips backwards 10 seconds. */
    public void rewind() {
        long newPosition = getCurrentPosition() - TEN_SECONDS;
        newPosition = (newPosition < 0) ? 0 : newPosition;
        getPlayerAdapter().seekTo(newPosition);
    }

    /** Skips forward 10 seconds. */
    public void fastForward() {
        if (getDuration() > -1) {
            long newPosition = getCurrentPosition() + TEN_SECONDS;
            newPosition = (newPosition > getDuration()) ? getDuration() : newPosition;
            getPlayerAdapter().seekTo(newPosition);
        }
    }
}
