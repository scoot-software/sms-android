package com.scooter1556.sms.android.domain;

import java.io.Serializable;
import java.util.UUID;

public class Playlist implements Serializable {

    private UUID id;
    private String name;
    private String description;
    private String username;

    public Playlist() {};

    public Playlist(UUID id,
                    String name,
                    String description,
                    String username) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.username = username;
    }

    @Override
    public String toString() {
        return String.format(
                "Playlist[ID=%s, Name=%s, Description=%s, User=%s, Path=%s, Parent Path=%s, Last Scanned=%s]",
                id == null ? "N/A" : id,
                name == null ? "N/A" : name,
                description == null ? "N/A" : description,
                username == null ? "N/A" : username);
    }

    public UUID getID()  {
        return id;
    }

    public void setID(UUID id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
