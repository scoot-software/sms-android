package com.scooter1556.sms.android.action;

import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Action;

import com.scooter1556.sms.android.R;

public class AudioTrackAction extends Action {
    private static final int ACTION_AUDIO_TRACK_ID = 0x7f0f0015;

    private int index;

    public AudioTrackAction(Context context, int index) {
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
