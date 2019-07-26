package com.scooter1556.sms.android.action;

import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Action;

import com.scooter1556.sms.android.R;

public class TextTrackAction extends Action {
    private static final int ACTION_TEXT_TRACK_ID = 0x7f0f0016;

    private int index;

    public TextTrackAction(Context context, int index) {
        super(ACTION_TEXT_TRACK_ID);
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
