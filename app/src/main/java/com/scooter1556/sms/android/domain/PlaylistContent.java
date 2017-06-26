package com.scooter1556.sms.android.domain;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class PlaylistContent implements Serializable {

    private UUID id;
    private List<Long> media;

    public PlaylistContent() {};

    public PlaylistContent(UUID id, List<Long> media) {
        this.id = id;
        this.media = media;
    }

    @Override
    public String toString() {
        return String.format(
                "Playlist Content[ID=%s, Media Elements=%s]",
                id == null ? "N/A" : id,
                media == null ? "N/A" : media);
    }

    public UUID getID()  {
        return id;
    }

    public void setID(UUID id) {
        this.id = id;
    }

    public List<Long> getMedia() {
        return this.media;
    }

    public void setMedia(List<Long> media) {
        this.media =  media;
    }
}
