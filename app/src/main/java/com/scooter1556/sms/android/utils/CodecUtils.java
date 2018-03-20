package com.scooter1556.sms.android.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import com.google.android.exoplayer2.audio.AudioCapabilities;

import java.util.ArrayList;
import java.util.List;

public class CodecUtils {

    public static String getSupportedMchAudioCodecs(Context ctx) {
        if(ctx == null) {
            return null;
        }

        List<String> codecs =  new ArrayList<>();

        // Get audio capabilities of current hardware
        AudioCapabilities audioCapabilities = AudioCapabilities.getCapabilities(ctx);

        if(!(audioCapabilities.getMaxChannelCount() > 2)) {
            // Platform doesn't support more than 2 channels
            return null;
        }

        // Get list of platform supported codecs
        MediaCodecInfo[] mediaCodecInfo = new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos();

        for(MediaCodecInfo codecInfo : mediaCodecInfo) {
            // We aren't interested in encoders
            if(codecInfo.isEncoder()) {
                continue;
            }

            // Get audio capabilities of codec
            String type = codecInfo.getSupportedTypes()[0];
            MediaCodecInfo.AudioCapabilities capabilities = codecInfo.getCapabilitiesForType(type).getAudioCapabilities();

            // Check this is an audio codec
            if(capabilities == null) {
                continue;
            }

            // Check if this codec supports more than 2 channels
            if(!(capabilities.getMaxInputChannelCount() > 2)) {
                continue;
            }

            String[] sCodec = codecInfo.getName().toLowerCase().split("\\.");

            for(String codec : sCodec) {
                switch(codec) {
                    case "aac":
                    case "ac3":
                    case "eac3":
                    case "flac":
                    case "opus":
                    case "mp3":
                    case "vorbis":
                        appendToList(codecs, codec);
                        break;

                    case "raw":
                        appendToList(codecs, "pcm");
                        break;
                }
            }
        }

        // Get list of codecs supported by external hardware
        parseAudioCapabilities(codecs, audioCapabilities);

        if(codecs.isEmpty()) {
            return null;
        }

        StringBuilder list = new StringBuilder();

        for(String codec : codecs) {
            list.append(codec);
            list.append(",");
        }

        return list.toString();
    }

    public static String getSupportedCodecs(Context ctx)  {
        if(ctx == null) {
            return null;
        }

        List<String> codecs =  new ArrayList<>();

        // Get list of platform supported codecs
        MediaCodecInfo[] mediaCodecInfo = new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos();

        for(MediaCodecInfo codecInfo : mediaCodecInfo) {
            // We aren't interested in encoders
            if(codecInfo.isEncoder()) {
                continue;
            }

            String[] sCodec = codecInfo.getName().toLowerCase().split("\\.");

            for(String codec : sCodec) {
                switch(codec) {
                    case "aac":
                    case "ac3":
                    case "eac3":
                    case "flac":
                    case "h264":
                    case "hevc":
                    case "mp3":
                    case "opus":
                    case "vorbis":
                    case "vp8":
                        appendToList(codecs, codec);
                        break;

                    case "avc":
                        appendToList(codecs, "h264");
                        break;

                    case "mpeg2":
                        appendToList(codecs, "mpeg2video");
                        break;

                    case "raw":
                        appendToList(codecs, "pcm");
                        break;
                }
            }
        }

        // Check for additional codecs supported by external hardware
        AudioCapabilities audioCapabilities = AudioCapabilities.getCapabilities(ctx);
        parseAudioCapabilities(codecs, audioCapabilities);

        //  Check if we found any supported codecs
        if(codecs.isEmpty()) {
            return null;
        }

        StringBuilder list = new StringBuilder();

        for(String codec : codecs) {
            list.append(codec);
            list.append(",");
        }

        return list.toString();
    }

    private static void parseAudioCapabilities(List<String> list, AudioCapabilities capabilities) {
        if(capabilities == null || list == null) {
            return;
        }

        if(capabilities.supportsEncoding(AudioFormat.ENCODING_PCM_16BIT)) {
            appendToList(list, "pcm");
        }

        if(capabilities.supportsEncoding(AudioFormat.ENCODING_AC3)) {
            appendToList(list, "ac3");
        }

        if(capabilities.supportsEncoding(AudioFormat.ENCODING_E_AC3)) {
            appendToList(list, "eac3");
        }

        if(capabilities.supportsEncoding(AudioFormat.ENCODING_DOLBY_TRUEHD)) {
            appendToList(list, "truehd");
        }

        if(capabilities.supportsEncoding(AudioFormat.ENCODING_DTS)) {
            appendToList(list, "dts");
        }
    }

    private static void appendToList(List<String> list, String item) {
        if(list == null || item == null || item.isEmpty()) {
            return;
        }

        if(list.contains(item.trim().toLowerCase())) {
            return;
        }

        list.add(item.trim().toLowerCase());
    }
}
