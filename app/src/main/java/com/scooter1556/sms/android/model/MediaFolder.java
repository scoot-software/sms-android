package com.scooter1556.sms.android.model;

import android.support.v4.media.MediaBrowserCompat;

import java.util.ArrayList;
import java.util.List;

public class MediaFolder extends BaseModel {

    public static final int TYPE_MEDIA_FOLDER = 0;

    private MediaBrowserCompat.MediaItem item;
    private List<String> imageUrls;

    public MediaFolder(String id, MediaBrowserCompat.MediaItem item) {
        super(TYPE_MEDIA_FOLDER, id);

        this.item = item;
        imageUrls = new ArrayList<>();
    }

    public MediaBrowserCompat.MediaItem getMediaItem() {
        return item;
    }

    public void addImageUrl(String url) {
        imageUrls.add(url);
    }

    public String getImageUrl(int num) {
        if(imageUrls.size() > num) {
            return imageUrls.get(num);
        }

        return null;
    }
}
