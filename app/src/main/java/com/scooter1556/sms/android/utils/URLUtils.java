package com.scooter1556.sms.android.utils;

public class URLUtils {

    private static final String TAG = "URLUtils";

    public static String encodeURL(String url) {
        String result = url;

        // Encode special characters for URL
        result = result.replace("+", "%2B");

        return result;
    }
}
