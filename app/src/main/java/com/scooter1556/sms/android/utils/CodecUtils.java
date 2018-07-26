package com.scooter1556.sms.android.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.scooter1556.sms.android.SMS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CodecUtils {

    public static Integer[] getSupportedMchAudioCodecs(Context ctx) {
        if(ctx == null) {
            return null;
        }

        List<Integer> codecs =  new ArrayList<>();

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
                        codecs.add(SMS.Codec.AAC);
                        break;

                    case "ac3":
                        codecs.add(SMS.Codec.AC3);
                        break;

                    case "eac3":
                        codecs.add(SMS.Codec.EAC3);
                        break;

                    case "flac":
                        codecs.add(SMS.Codec.FLAC);
                        break;

                    case "vorbis":
                        codecs.add(SMS.Codec.VORBIS);
                        break;

                    case "raw":
                        codecs.add(SMS.Codec.PCM);
                        break;
                }
            }
        }

        // Get list of codecs supported by external hardware
        parseAudioCapabilities(codecs, audioCapabilities);

        if(codecs.isEmpty()) {
            return null;
        }

        // Remove duplicates
        HashSet<Integer> codecHashSet = new HashSet<>(codecs);
        codecs.clear();
        codecs.addAll(codecHashSet);

        return codecs.toArray(new Integer[codecs.size()]);
    }

    public static Integer[] getSupportedCodecs(Context ctx)  {
        if(ctx == null) {
            return null;
        }

        List<Integer> codecs =  new ArrayList<>();

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
                        codecs.add(SMS.Codec.AAC);
                        break;

                    case "ac3":
                        codecs.add(SMS.Codec.AC3);
                        break;

                    case "eac3":
                        codecs.add(SMS.Codec.EAC3);
                        break;

                    case "flac":
                        codecs.add(SMS.Codec.FLAC);
                        break;

                    case "h264": case "avc":
                        codecs.add(SMS.Codec.AVC_BASELINE);
                        codecs.add(SMS.Codec.AVC_MAIN);
                        codecs.add(SMS.Codec.AVC_HIGH);
                        break;

                    case "hevc":
                        codecs.add(SMS.Codec.HEVC_MAIN);
                        break;

                    case "mp3":
                        codecs.add(SMS.Codec.MP3);
                        break;

                    case "vorbis":
                        codecs.add(SMS.Codec.VORBIS);
                        break;

                    case "mpeg2":
                        codecs.add(SMS.Codec.MPEG2);
                        break;

                    case "raw":
                        codecs.add(SMS.Codec.PCM);
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

        // Remove duplicates
        HashSet<Integer> codecHashSet = new HashSet<>(codecs);
        codecs.clear();
        codecs.addAll(codecHashSet);

        return codecs.toArray(new Integer[codecs.size()]);
    }

    private static void parseAudioCapabilities(List<Integer> list, AudioCapabilities capabilities) {
        if(capabilities == null || list == null) {
            return;
        }

        if(capabilities.supportsEncoding(AudioFormat.ENCODING_PCM_16BIT)) {
            list.add(SMS.Codec.PCM);
        }

        if(capabilities.supportsEncoding(AudioFormat.ENCODING_AC3)) {
            list.add(SMS.Codec.AC3);
        }

        if(capabilities.supportsEncoding(AudioFormat.ENCODING_E_AC3)) {
            list.add(SMS.Codec.EAC3);
        }

        if(capabilities.supportsEncoding(AudioFormat.ENCODING_DOLBY_TRUEHD)) {
            list.add(SMS.Codec.TRUEHD);
        }

        if(capabilities.supportsEncoding(AudioFormat.ENCODING_DTS)) {
            list.add(SMS.Codec.DTS);
        }

        if(capabilities.supportsEncoding(AudioFormat.ENCODING_DTS_HD)) {
            list.add(SMS.Codec.DTSHD);
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
