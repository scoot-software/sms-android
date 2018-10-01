package com.scooter1556.sms.android.views.row;

import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.PageRow;

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
