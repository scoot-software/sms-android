package com.scooter1556.sms.android.provider;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.session.PlaybackStateCompat;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.scooter1556.sms.android.R;

/**
 * Provides a custom action for toggling shuffle mode.
 */
public final class ShuffleActionProvider implements MediaSessionConnector.CustomActionProvider {

    private static final String ACTION_SHUFFLE_MODE = "ACTION_EXO_SHUFFLE_MODE";

    private final Player player;
    private final CharSequence shuffleOnDescription;
    private final CharSequence shuffleOffDescription;

    /**
     * Creates a new instance.
     *
     * @param context The context.
     * @param player The player on which to toggle the repeat mode.
     */
    public ShuffleActionProvider(Context context, Player player) {
        this.player = player;
        shuffleOnDescription = context.getString(R.string.description_shuffle_on);
        shuffleOffDescription = context.getString(R.string.description_shuffle_off);
    }

    @Override
    public void onCustomAction(String action, Bundle extras) {
        boolean isShuffleModeEnabled = player.getShuffleModeEnabled();
        player.setShuffleModeEnabled(!isShuffleModeEnabled);
    }

    @Override
    public PlaybackStateCompat.CustomAction getCustomAction() {
        CharSequence actionLabel;
        int iconResourceId;

        if(player.getShuffleModeEnabled()) {
            actionLabel = shuffleOnDescription;
            iconResourceId = R.drawable.ic_shuffle;
        } else {
            actionLabel = shuffleOffDescription;
            iconResourceId = R.drawable.ic_shuffle_off;
        }

        PlaybackStateCompat.CustomAction.Builder repeatBuilder = new PlaybackStateCompat.CustomAction
                .Builder(ACTION_SHUFFLE_MODE, actionLabel, iconResourceId);

        return repeatBuilder.build();
    }

}