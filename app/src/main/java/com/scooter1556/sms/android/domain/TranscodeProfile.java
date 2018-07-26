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
import java.util.Arrays;
import java.util.UUID;

public class TranscodeProfile implements Serializable {
    private UUID id;
    private byte type;
    private MediaElement element;
    private Integer[] formats, codecs, mchCodecs;
    private Integer format, quality, maxBitRate, maxSampleRate = 48000;
    private String url, mimeType, client;
    private Integer videoStream, audioStream, subtitleStream;
    private Integer offset = 0;
    private boolean directPlay = false;
    private boolean active = true;

    public TranscodeProfile() {}

    public TranscodeProfile(UUID id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return String.format("TranscodeProfile[ID=%s, Type=%s, %s, Client=%s, Supported Formats=%s, Supported Codecs=%s, Supported Multichannel Codecs=%s, Quality=%s, Max Sample Rate=%s, Max Bit Rate=%s, Format=%s, Mime Type=%s, Video Stream=%s, Audio Stream=%s, Subtitle Stream=%s, Offset=%s, Direct Play=%s",
                id == null ? "null" : id.toString(),
                String.valueOf(type),
                element == null ? "null" : element.toString(),
                client == null ? "null" : client,
                formats == null ? "null" : Arrays.toString(formats),
                codecs == null ? "null" : Arrays.toString(codecs),
                mchCodecs == null ? "null" : Arrays.toString(mchCodecs),
                quality == null ? "null" : quality.toString(),
                maxSampleRate == null ? "null" : maxSampleRate.toString(),
                maxBitRate == null ? "null" : maxBitRate.toString(),
                format == null ? "null" : format,
                mimeType == null ? "null" : mimeType,
                videoStream == null ? "null" : videoStream.toString(),
                audioStream == null ? "null" : audioStream.toString(),
                subtitleStream == null ? "null" : subtitleStream.toString(),
                offset == null ? "null" : offset.toString(),
                String.valueOf(directPlay));
    }

    public UUID getID() {
        return id;
    }

    public void setID(UUID id) {
        this.id = id;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public MediaElement getMediaElement() {
        return element;
    }

    public void setMediaElement(MediaElement element) {
        this.element = element;
    }

    public boolean isDirectPlayEnabled() {
        return directPlay;
    }

    public void setDirectPlayEnabled(boolean directPlay) {
        this.directPlay = directPlay;
    }

    public Integer[] getFormats() {
        return formats;
    }

    public void setFormats(Integer[] formats) {
        this.formats = formats;
    }

    public Integer[] getCodecs() {
        return codecs;
    }

    public void setCodecs(Integer[] codecs) {
        this.codecs = codecs;
    }

    public Integer[] getMchCodecs() {
        return mchCodecs;
    }

    public void setMchCodecs(Integer[] mchCodecs) {
        this.mchCodecs = mchCodecs;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getFormat() {
        return format;
    }

    public void setFormat(Integer format) {
        this.format = format;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public Integer getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public Integer getVideoStream() {
        return videoStream;
    }

    public void setVideoStream(Integer videoStream) {
        this.videoStream = videoStream;
    }

    public Integer getAudioStream() {
        return audioStream;
    }

    public void setAudioStream(Integer audioStream) {
        this.audioStream = audioStream;
    }

    public Integer getSubtitleStream() {
        return subtitleStream;
    }

    public void setSubtitleStream(Integer subtitleStream) {
        this.subtitleStream = subtitleStream;
    }

    public Integer getMaxSampleRate() {
        return maxSampleRate;
    }

    public void setMaxSampleRate(int maxSampleRate) {
        this.maxSampleRate = maxSampleRate;
    }

    public Integer getMaxBitRate() {
        return maxBitRate;
    }

    public void setMaxBitRate(int maxBitRate) {
        this.maxBitRate = maxBitRate;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public static class StreamType {
        public static final byte DIRECT = 0;
        public static final byte LOCAL = 1;
        public static final byte REMOTE = 2;
    }
}
