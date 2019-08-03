package com.scooter1556.sms.android.presenter;

import android.content.Context;
import androidx.leanback.widget.AbstractMediaItemPresenter;

import android.text.format.DateUtils;
import android.view.View;

import com.scooter1556.sms.android.domain.MediaElement;

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
        MediaElement queueItem = (MediaElement) item;

        if(item != null) {
            vh.getMediaItemDurationView().setVisibility(View.VISIBLE);
            vh.getMediaItemNumberView().setVisibility(View.VISIBLE);

            vh.getMediaItemNameView().setText(queueItem.getTitle());

            vh.getMediaItemNumberView().setText(String.valueOf(queueItem.getTrackNumber()));
            vh.getMediaItemDurationView().setText(DateUtils.formatElapsedTime(queueItem.getDuration().longValue()));
        }
    }
}
