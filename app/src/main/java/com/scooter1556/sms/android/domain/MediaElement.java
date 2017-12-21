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
package com.scooter1556.sms.android.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MediaElement implements Serializable {

    private UUID id;
    private Byte type;
    private Byte directoryType;
    private String format;
    private Long size;
    private Double duration;
    private String title;
    private String artist;
    private String albumArtist;
    private String album;
    private Short year;
    private Short discNumber;
    private String discSubtitle;
    private Short trackNumber;
    private String genre;
    private Float rating;
    private String tagline;
    private String description;
    private String certificate;
    private String collection;

    public MediaElement() {};

    public MediaElement(UUID id,
                        Byte type,
                        Byte directoryType,
                        String format,
                        Long size,
                        Double duration,
                        String title,
                        String artist,
                        String albumArtist,
                        String album,
                        Short year,
                        Short discNumber,
                        String discSubtitle,
                        Short trackNumber,
                        String genre,
                        Float rating,
                        String tagline,
                        String description,
                        String certificate,
                        String collection)
    {
        this.id = id;
        this.type = type;
        this.directoryType = directoryType;
        this.format = format;
        this.size = size;
        this.duration = duration;
        this.title = title;
        this.artist = artist;
        this.albumArtist = albumArtist;
        this.album = album;
        this.year = year;
        this.discNumber = discNumber;
        this.discSubtitle = discSubtitle;
        this.trackNumber = trackNumber;
        this.genre = genre;
        this.rating = rating;
        this.tagline = tagline;
        this.description = description;
        this.certificate = certificate;
        this.collection = collection;
    }

    public UUID getID()  {
        return id;
    }

    public void setID(UUID id) {
        this.id = id;
    }

    public Byte getType() {
        return type;
    }

    public void setType(Byte type) {
        this.type = type;
    }

    public Byte getDirectoryType() {
        return directoryType;
    }

    public void setDirectoryType(Byte directoryType) {
        this.directoryType = directoryType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public Short getYear() {
        return year;
    }

    public void setYear(Short year) {
        this.year = year;
    }

    public Short getDiscNumber() {
        return discNumber;
    }

    public void setDiscNumber(Short discNumber) {
        this.discNumber = discNumber;
    }

    public String getDiscSubtitle() {
        return discSubtitle;
    }

    public void setDiscSubtitle(String discSubtitle) {
        this.discSubtitle = discSubtitle;
    }

    public Short getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(Short trackNumber) {
        this.trackNumber = trackNumber;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Float getRating() {
        return rating;
    }

    public void setRating(Float rating) {
        this.rating = rating;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public static class MediaElementType {
        public static final byte AUDIO = 0;
        public static final byte VIDEO = 1;
        public static final byte DIRECTORY = 2;
    }

    public static class DirectoryMediaType {
        public static final byte AUDIO = 0;
        public static final byte VIDEO = 1;
        public static final byte MIXED = 2;
        public static final byte NONE = 3;
    }
}