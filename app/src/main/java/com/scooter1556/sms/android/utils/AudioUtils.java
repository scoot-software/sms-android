package com.scooter1556.sms.android.utils;

import android.media.AudioFormat;
import android.media.MediaCodecInfo;

import com.google.android.exoplayer2.audio.AudioCapabilities;

import java.util.ArrayList;
import java.util.List;

public class AudioUtils {

    public static String getSupportedMchAudioCodecs(AudioCapabilities capabilities) {
        if(capabilities == null) {
            return null;
        }

        List<String> codecs =  new ArrayList<>();

        if(capabilities.getMaxChannelCount() > 2) {
            if(capabilities.supportsEncoding(AudioFormat.ENCODING_AC3)) {
                codecs.add("ac3");
            }

            if(capabilities.supportsEncoding(AudioFormat.ENCODING_E_AC3)) {
                codecs.add("eac3");
            }

            if(capabilities.supportsEncoding(AudioFormat.ENCODING_DOLBY_TRUEHD)) {
                codecs.add("truehd");
            }

            if(capabilities.supportsEncoding(AudioFormat.ENCODING_DTS)) {
                codecs.add("dts");
            }
        }

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
}
