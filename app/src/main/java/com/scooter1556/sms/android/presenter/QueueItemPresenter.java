package com.scooter1556.sms.android.presenter;

import android.content.Context;
import androidx.leanback.widget.AbstractMediaItemPresenter;

import android.support.v4.media.session.MediaSessionCompat;
import android.text.format.DateUtils;
import android.view.View;

public class QueueItemPresenter extends AbstractMediaItemPresenter {
    public QueueItemPresenter() {
        super();
    }

    public QueueItemPresenter(Context context, int themeResId) {
        super(themeResId);
        setHasMediaRowSeparator(true);
    }

    @Override
    protected void onBindMediaDetails(ViewHolder vh, Object item) {
        MediaSessionCompat.QueueItem queueItem = (MediaSessionCompat.QueueItem) item;

        if(queueItem.getDescription().getExtras() != null) {
            vh.getMediaItemDurationView().setVisibility(View.VISIBLE);
            vh.getMediaItemNumberView().setVisibility(View.VISIBLE);

            vh.getMediaItemNumberView().setText(String.valueOf(queueItem.getDescription().getExtras().getShort("TrackNumber", (short) 0)));
            vh.getMediaItemDurationView().setText(DateUtils.formatElapsedTime(queueItem.getDescription().getExtras().getInt("Duration", 0)));
        }

        vh.getMediaItemNameView().setText(queueItem.getDescription().getTitle());
    }
}
