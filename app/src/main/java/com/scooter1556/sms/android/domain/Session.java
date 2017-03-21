package com.scooter1556.sms.android.domain;

import java.io.Serializable;
import java.util.UUID;

public class Session implements Serializable {
    private UUID id;
    private String username;

    public Session() {}

    public Session(UUID id, String username) {
        this.id = id;
        this.username = username;
    }

    @Override
    public String toString() {
        return String.format("{ID=%s, Username=%s}",
                id == null ? "null" : id,
                username == null ? "null" : username);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}