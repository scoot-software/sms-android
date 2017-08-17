package com.scooter1556.sms.android.presenter;

import android.content.Context;
import android.support.v17.leanback.widget.AbstractMediaItemPresenter;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.text.format.DateUtils;
import android.view.View;

import com.scooter1556.sms.android.R;

public class AudioItemPresenter extends AbstractMediaItemPresenter {
    public AudioItemPresenter() {
        super();
    }

    public AudioItemPresenter(Context context, int themeResId) {
        super(themeResId);
        setHasMediaRowSeparator(true);
    }

    @Override
    protected void onBindMediaDetails(ViewHolder vh, Object item) {
        MediaDescriptionCompat description = (MediaDescriptionCompat) item;

        if(description.getExtras() != null) {
            vh.getMediaItemDurationView().setVisibility(View.VISIBLE);
            vh.getMediaItemNumberView().setVisibility(View.VISIBLE);

            vh.getMediaItemNumberView().setText(String.valueOf(description.getExtras().getShort("TrackNumber", (short) 0)));
            vh.getMediaItemDurationView().setText(DateUtils.formatElapsedTime(description.getExtras().getInt("Duration", 0)));
        }

        vh.getMediaItemNameView().setText(description.getTitle());
    }
}
