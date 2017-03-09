/*
 * Author: Scott Ware <scoot.software@gmail.com>
 * Copyright (c) 2015 Scott Ware
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.scooter1556.sms.android.utils;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;

import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.scooter1556.sms.android.service.MediaService;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.lib.android.domain.MediaElement;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class MediaUtils {

    private static final String TAG = "MediaUtils";

    public static final String EXTRA_MEDIA_TITLE = "com.scooter1556.sms.android.activity.EXTRA_MEDIA_TITLE";
    public static final String EXTRA_MEDIA_ITEM = "com.scooter1556.sms.android.activity.EXTRA_MEDIA_ID";

    public static final String SEPARATOR = "|";

    // Media IDs used on MediaBrowser items
    public static final String MEDIA_ID_MENU_AUDIO = "__MENU_AUDIO__";
    public static final String MEDIA_ID_MENU_VIDEO = "__MENU_VIDEO__";
    public static final String MEDIA_ID_RECENTLY_PLAYED = "__RECENTLY_PLAYED__";
    public static final String MEDIA_ID_RECENTLY_ADDED = "__RECENTLY_ADDED__";
    public static final String MEDIA_ID_RECENTLY_PLAYED_AUDIO = "__RECENTLY_PLAYED_AUDIO__";
    public static final String MEDIA_ID_RECENTLY_PLAYED_VIDEO = "__RECENTLY_PLAYED_VIDEO__";
    public static final String MEDIA_ID_RECENTLY_ADDED_AUDIO = "__RECENTLY_ADDED_AUDIO__";
    public static final String MEDIA_ID_RECENTLY_ADDED_VIDEO = "__RECENTLY_ADDED_VIDEO__";
    public static final String MEDIA_ID_COLLECTIONS = "__COLLECTIONS__";
    public static final String MEDIA_ID_COLLECTION = "__COLLECTION__";
    public static final String MEDIA_ID_ARTISTS = "__ARTISTS__";
    public static final String MEDIA_ID_ARTIST = "__ARTIST__";
    public static final String MEDIA_ID_ALBUM_ARTISTS = "__ALBUM_ARTISTS__";
    public static final String MEDIA_ID_ALBUM_ARTIST = "__ALBUM_ARTIST__";
    public static final String MEDIA_ID_ALBUMS = "__ALBUMS__";
    public static final String MEDIA_ID_ALBUM = "__ALBUM__";
    public static final String MEDIA_ID_ARTIST_ALBUM = "__ARTIST_ALBUM__";
    public static final String MEDIA_ID_ALBUM_ARTIST_ALBUM = "__ALBUM_ARTIST_ALBUM__";
    public static final String MEDIA_ID_FOLDERS = "__FOLDERS__";
    public static final String MEDIA_ID_FOLDERS_AUDIO = "__FOLDERS_AUDIO__";
    public static final String MEDIA_ID_FOLDERS_VIDEO = "__FOLDERS_VIDEO__";
    public static final String MEDIA_ID_FOLDER = "__FOLDER__";
    public static final String MEDIA_ID_DIRECTORY = "__DIRECTORY__";
    public static final String MEDIA_ID_DIRECTORY_AUDIO = "__DIRECTORY_AUDIO__";
    public static final String MEDIA_ID_DIRECTORY_VIDEO = "__DIRECTORY_VIDEO__";
    public static final String MEDIA_ID_VIDEO = "__VIDEO__";
    public static final String MEDIA_ID_AUDIO = "__AUDIO__";


    /*
     * Make sure public utility methods remain static
     */
    private MediaUtils() {}

    /*
     * Retrieve MediaMetadata for a MediaElement
     */
    public static MediaMetadataCompat getMediaMetadataCompatFromMediaElement(MediaElement mediaElement) {
        if(mediaElement == null) {
            return null;
        }

        // Update session metadata
        MediaMetadataCompat.Builder metadata = new MediaMetadataCompat.Builder();
        metadata.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaElement.getID().toString());

        if(mediaElement.getArtist() != null) {
            metadata.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mediaElement.getArtist());
            metadata.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, mediaElement.getArtist());
        }

        if(mediaElement.getAlbum() != null) {
            metadata.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, mediaElement.getAlbum());
            metadata.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, mediaElement.getAlbum());
        }

        if(mediaElement.getTitle() != null) {
            metadata.putString(MediaMetadataCompat.METADATA_KEY_TITLE, mediaElement.getTitle());
            metadata.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, mediaElement.getTitle());
        }

        if(mediaElement.getDuration() != null) {
            metadata.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaElement.getDuration() * 1000);
        }

        if(mediaElement.getYear() != null) {
            metadata.putLong(MediaMetadataCompat.METADATA_KEY_YEAR, mediaElement.getYear());
        }

        if(mediaElement.getGenre() != null) {
            metadata.putString(MediaMetadataCompat.METADATA_KEY_GENRE, mediaElement.getGenre());
        }

        if(mediaElement.getTrackNumber() != null) {
            metadata.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, mediaElement.getTrackNumber());
        }

        if(mediaElement.getDiscNumber() != null) {
            metadata.putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, mediaElement.getDiscNumber());
        }

        if(mediaElement.getAlbumArtist() != null) {
            metadata.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, mediaElement.getAlbumArtist());
        }

        if(RESTService.getInstance().getAddress() != null) {
            metadata.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, RESTService.getInstance().getAddress() + "/image/" + mediaElement.getID() + "/cover/200");
            metadata.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, RESTService.getInstance().getAddress() + "/image/" + mediaElement.getID() + "/cover/1000");
            metadata.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, RESTService.getInstance().getAddress() + "/image/" + mediaElement.getID() + "/fanart/1000");
        }

        return metadata.build();
    }

    public static MediaMetadata getMediaMetadataFromMediaElement(MediaElement mediaElement) {
        MediaMetadata metadata = new MediaMetadata(mediaElement.getType());

        if(mediaElement.getArtist() != null) {
            metadata.putString(MediaMetadata.KEY_ARTIST, mediaElement.getArtist());
            metadata.putString(MediaMetadata.KEY_SUBTITLE, mediaElement.getArtist());
            metadata.putString(MediaMetadata.KEY_SUBTITLE, mediaElement.getArtist());
        }

        if(mediaElement.getAlbum() != null) {
            metadata.putString(MediaMetadata.KEY_ALBUM_TITLE, mediaElement.getAlbum());
        }

        if(mediaElement.getTitle() != null) {
            metadata.putString(MediaMetadata.KEY_TITLE, mediaElement.getTitle());
        }

        if(mediaElement.getTrackNumber() != null) {
            metadata.putInt(MediaMetadata.KEY_TRACK_NUMBER, mediaElement.getTrackNumber());
        }

        if(mediaElement.getDiscNumber() != null) {
            metadata.putInt(MediaMetadata.KEY_DISC_NUMBER, mediaElement.getDiscNumber());
        }

        if(mediaElement.getAlbumArtist() != null) {
            metadata.putString(MediaMetadata.KEY_ALBUM_ARTIST, mediaElement.getAlbumArtist());
        }

        if(RESTService.getInstance().getAddress() != null) {
            metadata.addImage(new WebImage(Uri.parse(RESTService.getInstance().getAddress() + "/image/" + mediaElement.getID() + "/cover/500")));
            metadata.addImage(new WebImage(Uri.parse(RESTService.getInstance().getAddress() + "/image/" + mediaElement.getID() + "/fanart/1280")));
        }

        return metadata;
    }

    public static List<String> parseMediaId(@NonNull String mediaId) {
        List<String> result = new ArrayList<>();

        StringTokenizer tokens = new StringTokenizer(mediaId, SEPARATOR);

        while(tokens.hasMoreTokens()) {
            result.add(tokens.nextToken());
        }

        return result;
    }

    public static String getMediaIDFromMediaElement(@NonNull MediaElement element) {
        switch(element.getType()) {
            case MediaElement.MediaElementType.AUDIO:
                return MEDIA_ID_AUDIO + SEPARATOR + String.valueOf(element.getID());

            case MediaElement.MediaElementType.VIDEO:
                return MEDIA_ID_VIDEO + SEPARATOR + String.valueOf(element.getID());

            default:
                return null;
        }
    }

    public static Byte getMediaTypeFromID(@NonNull String mediaId) {
        if(mediaId.startsWith(MEDIA_ID_AUDIO)) {
            return MediaElement.MediaElementType.AUDIO;
        } else if(mediaId.startsWith(MEDIA_ID_VIDEO)) {
            return MediaElement.MediaElementType.VIDEO;
        } else {
            return -1;
        }
    }
}
