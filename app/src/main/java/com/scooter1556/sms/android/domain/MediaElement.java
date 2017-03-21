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

public class MediaElement implements Serializable {

    private Long id;
    private Byte type;
    private Byte directoryType;
    private String format;
    private Long size;
    private Integer duration;
    private Integer bitrate;
    private Short videoWidth;
    private Short videoHeight;
    private String videoCodec;
    private String audioCodec;
    private String audioSampleRate;
    private String audioConfiguration;
    private String audioLanguage;
    private String subtitleLanguage;
    private String subtitleFormat;
    private String subtitleForced;
    private String title;
    private String artist;
    private String albumArtist;
    private String album;
    private Short year;
    private Byte discNumber;
    private String discSubtitle;
    private Short trackNumber;
    private String genre;
    private Float rating;
    private String tagline;
    private String description;
    private String certificate;
    private String collection;

    public MediaElement() {};

    public MediaElement(Long id,
                        Byte type,
                        Byte directoryType,
                        String format,
                        Long size,
                        Integer duration,
                        Integer bitrate,
                        Short videoWidth,
                        Short videoHeight,
                        String videoCodec,
                        String audioCodec,
                        String audioSampleRate,
                        String audioConfiguration,
                        String audioLanguage,
                        String subtitleLanguage,
                        String subtitleFormat,
                        String subtitleForced,
                        String title,
                        String artist,
                        String albumArtist,
                        String album,
                        Short year,
                        Byte discNumber,
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
        this.bitrate = bitrate;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.videoCodec = videoCodec;
        this.audioCodec = audioCodec;
        this.audioSampleRate = audioSampleRate;
        this.audioConfiguration = audioConfiguration;
        this.audioLanguage = audioLanguage;
        this.subtitleLanguage = subtitleLanguage;
        this.subtitleFormat = subtitleFormat;
        this.subtitleForced = subtitleForced;
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

    public Long getID()  {
        return id;
    }

    public void setID(Long id) {
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

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getBitrate() {
        return bitrate;
    }

    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }

    public Short getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(Short videoWidth) {
        this.videoWidth = videoWidth;
    }

    public Short getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(Short videoHeight) {
        this.videoHeight = videoHeight;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public String getAudioSampleRate() {
        return audioSampleRate;
    }

    public void setAudioSampleRate(String audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public String getAudioConfiguration() {
        return audioConfiguration;
    }

    public void setAudioConfiguration(String audioConfiguration) {
        this.audioConfiguration = audioConfiguration;
    }

    public String getAudioLanguage() {
        return audioLanguage;
    }

    public void setAudioLanguage(String audioLanguage) {
        this.audioLanguage = audioLanguage;
    }

    public String getSubtitleLanguage() {
        return subtitleLanguage;
    }

    public void setSubtitleLanguage(String subtitleLanguage) {
        this.subtitleLanguage = subtitleLanguage;
    }

    public String getSubtitleFormat() {
        return subtitleFormat;
    }

    public void setSubtitleFormat(String subtitleFormat) {
        this.subtitleFormat = subtitleFormat;
    }

    public String getSubtitleForced() {
        return subtitleForced;
    }

    public void setSubtitleForced(String subtitleForced) {
        this.subtitleForced = subtitleForced;
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

    public Byte getDiscNumber() {
        return discNumber;
    }

    public void setDiscNumber(Byte discNumber) {
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

    //
    // Helper Functions
    //

    public List<AudioStream> getAudioStreams()
    {
        // Check parameters are available
        if(audioLanguage == null || audioCodec == null || audioSampleRate == null || audioConfiguration == null)
        {
            return null;
        }

        // Retrieve parameters as arrays
        String[] audioLanguages = getAudioLanguage().split(",");
        String[] audioCodecs = getAudioCodec().split(",");
        String[] audioSampleRates = getAudioSampleRate().split(",");
        String[] audioConfigurations = getAudioConfiguration().split(",");

        // Get the number of audio streams
        Integer streams = audioLanguages.length;

        // Check parameters are present for all streams
        if(audioCodecs.length != streams || audioSampleRates.length != streams || audioConfigurations.length != streams)
        {
            return null;
        }

        // Accumulate list of audio streams
        int count = 0;
        List<AudioStream> audioStreams = new ArrayList<AudioStream>();

        while(count < streams)
        {
            audioStreams.add(new AudioStream(count, audioLanguages[count], audioCodecs[count], Integer.parseInt(audioSampleRates[count]), audioConfigurations[count]));
            count ++;
        }

        return audioStreams;
    }

    public List<SubtitleStream> getSubtitleStreams()
    {
        // Check parameters are available
        if(subtitleLanguage == null || subtitleFormat == null || subtitleForced == null)
        {
            return null;
        }

        // Retrieve parameters as arrays
        String[] subtitleLanguages = getSubtitleLanguage().split(",");
        String[] subtitleFormats = getSubtitleFormat().split(",");
        String[] subtitleForcedFlags = getSubtitleForced().split(",");

        // Get the number of subtitle streams
        Integer streams = subtitleLanguages.length;

        // Check parameters are present for all streams
        if(subtitleFormats.length != streams || subtitleForcedFlags.length != streams)
        {
            return null;
        }

        // Accumulate list of subtitle streams
        int count = 0;
        List<SubtitleStream> subtitleStreams = new ArrayList<SubtitleStream>();

        while(count < streams)
        {
            subtitleStreams.add(new SubtitleStream(count, subtitleLanguages[count], subtitleFormats[count], Boolean.parseBoolean(subtitleForcedFlags[count])));
            count ++;
        }

        return subtitleStreams;
    }

    public static class AudioStream
    {
        private final Integer stream;
        private final String language;
        private final String codec;
        private final Integer sampleRate;
        private final String configuration;

        public AudioStream(Integer stream, String language, String codec, Integer sampleRate, String configuration)
        {
            this.stream = stream;
            this.language = language;
            this.codec = codec;
            this.sampleRate = sampleRate;
            this.configuration = configuration;
        }

        public Integer getStream() {
            return stream;
        }

        public String getLanguage() {
            return language;
        }

        public String getCodec() {
            return codec;
        }

        public Integer getSampleRate() {
            return sampleRate;
        }

        public String getConfiguration() {
            return configuration;
        }
    }

    public static class SubtitleStream
    {
        private final Integer stream;
        private final String language;
        private final String format;
        private final Boolean forced;

        public SubtitleStream(Integer stream, String language, String format, Boolean forced)
        {
            this.stream = stream;
            this.language = language;
            this.format = format;
            this.forced = forced;
        }

        public Integer getStream() {
            return stream;
        }

        public String getLanguage() {
            return language;
        }

        public String getFormat() {
            return format;
        }

        public Boolean getForced() {
            return forced;
        }
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