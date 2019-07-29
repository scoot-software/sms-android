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
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.common.images.WebImage;
import com.scooter1556.sms.android.SMS;
import com.scooter1556.sms.android.service.RESTService;
import com.scooter1556.sms.android.domain.MediaElement;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

public class MediaUtils {

    private static final String TAG = "MediaUtils";

    public static final String EXTRA_MEDIA_TITLE = "com.scooter1556.sms.android.activity.EXTRA_MEDIA_TITLE";
    public static final String EXTRA_MEDIA_ID = "com.scooter1556.sms.android.activity.EXTRA_MEDIA_ID";
    public static final String EXTRA_MEDIA_ITEM = "com.scooter1556.sms.android.activity.EXTRA_MEDIA_ITEM";
    public static final String EXTRA_QUEUE_ITEM = "com.scooter1556.sms.android.activity.EXTRA_QUEUE_ITEM";
    public static final String EXTRA_MEDIA_OPTION = "com.scooter1556.sms.android.activity.EXTRA_MEDIA_OPTION";

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
    public static final String MEDIA_ID_PLAYLISTS = "__PLAYLISTS__";
    public static final String MEDIA_ID_PLAYLIST = "__PLAYLIST__";
    public static final String MEDIA_ID_FOLDERS = "__FOLDERS__";
    public static final String MEDIA_ID_FOLDERS_AUDIO = "__FOLDERS_AUDIO__";
    public static final String MEDIA_ID_FOLDERS_VIDEO = "__FOLDERS_VIDEO__";
    public static final String MEDIA_ID_FOLDER = "__FOLDER__";
    public static final String MEDIA_ID_FOLDER_AUDIO = "__FOLDER_AUDIO__";
    public static final String MEDIA_ID_FOLDER_VIDEO = "__FOLDER_VIDEO__";
    public static final String MEDIA_ID_DIRECTORY = "__DIRECTORY__";
    public static final String MEDIA_ID_DIRECTORY_AUDIO = "__DIRECTORY_AUDIO__";
    public static final String MEDIA_ID_DIRECTORY_VIDEO = "__DIRECTORY_VIDEO__";
    public static final String MEDIA_ID_VIDEO = "__VIDEO__";
    public static final String MEDIA_ID_AUDIO = "__AUDIO__";
    public static final String MEDIA_ID_RANDOM_AUDIO = "__RANDOM_AUDIO__";

    public static final int MEDIA_MENU_NONE = -1;
    public static final int MEDIA_MENU_SHUFFLE = 0;

    public static final short MEDIA_ID_KEY_TYPE = 0;
    public static final short MEDIA_ID_KEY_MEID = 1;


    /*
     * Make sure public utility methods remain static
     */
    private MediaUtils() {}

    public static MediaQueueItem getMediaQueueItem(MediaElement mediaElement) {
        MediaMetadata metadata = getMediaMetadataFromMediaElement(mediaElement);
        MediaInfo mediaInfo = new MediaInfo.Builder(mediaElement.getID().toString())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(metadata).build();
        return new MediaQueueItem.Builder(mediaInfo).build();
    }

    public static MediaElement getMediaElement(@NonNull MediaQueueItem item) {
        if(item.getMedia() == null) {
            return null;
        }

        MediaMetadata metadata = item.getMedia().getMetadata();

        // Populate media element
        MediaElement element = new MediaElement();
        element.setID(UUID.fromString(item.getMedia().getContentId()));

        if(metadata != null) {
            // Type
            element.setType(Integer.valueOf(metadata.getMediaType()).byteValue());

            // Artist
            if(metadata.containsKey(MediaMetadata.KEY_ARTIST)) {
                element.setArtist(metadata.getString(MediaMetadata.KEY_ARTIST));
            }

            // Album
            if(metadata.containsKey(MediaMetadata.KEY_ALBUM_TITLE)) {
                element.setAlbum(metadata.getString(MediaMetadata.KEY_ALBUM_TITLE));
            }

            // Title
            if(metadata.containsKey(MediaMetadata.KEY_TITLE)) {
                element.setTitle(metadata.getString(MediaMetadata.KEY_TITLE));
            }

            // Track Number
            if(metadata.containsKey(MediaMetadata.KEY_TRACK_NUMBER)) {
                element.setTrackNumber(Integer.valueOf(metadata.getInt(MediaMetadata.KEY_TRACK_NUMBER)).shortValue());
            }

            // Disc Number
            if(metadata.containsKey(MediaMetadata.KEY_DISC_NUMBER)) {
                element.setDiscNumber(Integer.valueOf(metadata.getInt(MediaMetadata.KEY_DISC_NUMBER)).shortValue());
            }

            // Album Artist
            if(metadata.containsKey(MediaMetadata.KEY_ALBUM_ARTIST)) {
                element.setAlbumArtist(metadata.getString(MediaMetadata.KEY_ALBUM_ARTIST));
            }
        }

        return element;
    }

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
            metadata.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Double.valueOf(mediaElement.getDuration() * 1000.0).longValue());
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
            metadata.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, RESTService.getInstance().getAddress() + "/image/" + mediaElement.getID() + "/cover");
            metadata.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, RESTService.getInstance().getAddress() + "/image/" + mediaElement.getID() + "/cover");
            metadata.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, RESTService.getInstance().getAddress() + "/image/" + mediaElement.getID() + "/fanart");
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
            metadata.addImage(new WebImage(Uri.parse(RESTService.getInstance().getAddress() + "/image/" + mediaElement.getID() + "/cover")));
            metadata.addImage(new WebImage(Uri.parse(RESTService.getInstance().getAddress() + "/image/" + mediaElement.getID() + "/fanart")));
        }

        return metadata;
    }

    public static MediaMetadata getMediaMetadataFromMediaDescription(MediaDescriptionCompat description) {
        MediaMetadata metadata = new MediaMetadata(getMediaTypeFromID(description.getMediaId()));

        // Get Media Element ID from Media ID
        List<String> mediaID = parseMediaId(description.getMediaId());

        if(mediaID.size() <= 1) {
            return null;
        }

        UUID id = UUID.fromString(mediaID.get(1));

        if(description.getTitle() != null) {
            metadata.putString(MediaMetadata.KEY_TITLE, description.getTitle().toString());
        }

        if(description.getSubtitle() != null) {
            metadata.putString(MediaMetadata.KEY_SUBTITLE, description.getSubtitle().toString());
        }

        metadata.putInt(MediaMetadata.KEY_TRACK_NUMBER, description.getExtras().getShort("TrackNumber"));
        metadata.putInt(MediaMetadata.KEY_DISC_NUMBER, description.getExtras().getShort("DiscNumber"));

        if(RESTService.getInstance().getAddress() != null) {
            metadata.addImage(new WebImage(Uri.parse(RESTService.getInstance().getAddress() + "/image/" + id + "/cover")));
            metadata.addImage(new WebImage(Uri.parse(RESTService.getInstance().getAddress() + "/image/" + id + "/fanart")));
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
                return MEDIA_ID_AUDIO + SEPARATOR + element.getID();

            case MediaElement.MediaElementType.VIDEO:
                return MEDIA_ID_VIDEO + SEPARATOR + element.getID();

            default:
                return null;
        }
    }

    public static Byte getMediaTypeFromID(@NonNull String mediaId) {
        if(mediaId.startsWith(MEDIA_ID_AUDIO)) {
            return MediaElement.MediaElementType.AUDIO;
        } else if(mediaId.startsWith(MEDIA_ID_VIDEO)) {
            return MediaElement.MediaElementType.VIDEO;
        } else if(mediaId.startsWith(MEDIA_ID_DIRECTORY)) {
            return MediaElement.MediaElementType.DIRECTORY;
        }else {
            return -1;
        }
    }

    public static Byte getDirectoryTypeFromID(@NonNull String mediaId) {
        if(!mediaId.startsWith(MEDIA_ID_DIRECTORY)) {
            return -1;
        }

        if(mediaId.startsWith(MEDIA_ID_DIRECTORY_AUDIO)) {
            return MediaElement.DirectoryMediaType.AUDIO;
        } else if(mediaId.startsWith(MEDIA_ID_DIRECTORY_VIDEO)) {
            return MediaElement.DirectoryMediaType.VIDEO;
        } else {
            return MediaElement.DirectoryMediaType.NONE;
        }
    }

    public static String getSubtitle(@NonNull MediaElement element) {
        switch(element.getType()) {

            case MediaElement.MediaElementType.AUDIO:
                if (element.getArtist() == null) {
                    return "";
                } else {
                    return element.getArtist();
                }

            case MediaElement.MediaElementType.VIDEO:
                if (element.getCollection() == null) {
                    return "";
                } else {
                    return element.getCollection();
                }

            case MediaElement.MediaElementType.DIRECTORY:
                if (element.getType() == MediaElement.DirectoryMediaType.AUDIO) {
                    if (element.getArtist() == null) {
                        return "";
                    } else {
                        return element.getArtist();
                    }
                } else if (element.getDirectoryType() == MediaElement.DirectoryMediaType.VIDEO) {
                    if (element.getCollection() == null) {
                        return "";
                    } else {
                        return element.getCollection();
                    }
                } else {
                    return "";
                }

            default:
                return "";

        }
    }

    public static MediaDescriptionCompat getMediaDescription(@NonNull MediaElement element) {
        String mediaId = getMediaIDFromMediaElement(element);

        if(mediaId == null) { return null; }

        Bundle extras = new Bundle();
        if(element.getYear() != null) { extras.putShort("Year", element.getYear()); }
        if(element.getDuration() != null) { extras.putDouble("Duration", element.getDuration()); }
        if(element.getTrackNumber() != null) { extras.putShort("TrackNumber", element.getTrackNumber()); }
        if(element.getDiscNumber() != null) { extras.putShort("DiscNumber", element.getDiscNumber()); }
        if(element.getDiscSubtitle() != null) { extras.putString("DiscSubtitle", element.getDiscSubtitle()); }
        if(element.getGenre() != null) { extras.putString("Genre", element.getGenre()); }
        if(element.getRating() != null) { extras.putFloat("Rating", element.getRating()); }
        if(element.getCertificate() != null) { extras.putString("Certificate", element.getCertificate()); }
        if(element.getTagline() != null) { extras.putString("Tagline", element.getTagline()); }

        return new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .setTitle(element.getTitle() == null ? "" : element.getTitle())
                .setSubtitle(getSubtitle(element))
                .setDescription(element.getDescription() == null ? "" : element.getDescription())
                .setExtras(extras)
                .setIconUri(Uri.parse(RESTService.getInstance().getAddress() + "/image/" + element.getID() + "/cover"))
                .build();
    }

    public static MediaElement getMediaElementById(String id, @NonNull List<MediaElement> mediaElements) {
        if(mediaElements.isEmpty() || id.isEmpty()) {
            return null;
        }

        // Create UUID from id
        UUID uid = UUID.fromString(id);

        // Parse media elements for one with matching id
        for(MediaElement element : mediaElements) {
            if(element.getID().equals(uid)) {
                return element;
            }
        }

        return null;
    }

    public static MediaQueueItem[] getMediaQueue(List<MediaElement> queue) {
        MediaQueueItem[] items = new MediaQueueItem[queue.size()];

        for (int i = 0; i < items.length; i++) {
            items[i] = getMediaQueueItem(queue.get(i));
        }

        return items;
    }
}
