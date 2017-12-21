package com.scooter1556.sms.android.views.row;

import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.PageRow;

import java.util.UUID;

public class MediaBrowserRow extends PageRow {
    UUID mediaId;

    public MediaBrowserRow(HeaderItem headerItem) {
        super(headerItem);
    }

    public void setMediaId(UUID id) {
        this.mediaId = id;
    }

    public UUID getMediaId() {
        return mediaId;
    }
}
