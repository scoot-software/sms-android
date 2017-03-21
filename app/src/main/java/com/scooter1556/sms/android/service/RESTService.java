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
package com.scooter1556.sms.android.service;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.loopj.android.http.*;
import com.scooter1556.sms.android.utils.URLUtils;
import com.scooter1556.sms.android.domain.Connection;
import com.scooter1556.sms.android.domain.Session;
import com.scooter1556.sms.android.domain.TranscodeProfile;

import java.util.UUID;

import cz.msebera.android.httpclient.client.utils.URIBuilder;

public class RESTService {
    private static final String TAG = "RESTService";


    public RESTService() {}

    public static final int MIN_SUPPORTED_SERVER_VERSION = 38;

    private static final int TIMEOUT = 20000;
    private static final RESTService instance = new RESTService();
    private static AsyncHttpClient client = new AsyncHttpClient();
    private Connection connection;

    public static RESTService getInstance() {
        return instance;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;

        if(connection != null) {
            client.setBasicAuth(connection.getUsername(), connection.getPassword());
            client.setMaxRetriesAndTimeout(2, TIMEOUT);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public String getAddress() {
        if(connection == null) {
            return null;
        }

        return connection.getUrl();
    }

    public String getBaseAddress() {
        if(connection == null) {
            return null;
        }

        // Strip 'http://'
        if(connection.getUrl().startsWith("http://")) {
            return connection.getUrl().replace("http://", "");
        } else {
            return connection.getUrl();
        }
    }

    public AsyncHttpClient getClient() {
        return client;
    }

    //
    // Settings
    //

    // Returns the server version (can also be used to test user authentication)
    public void getVersion(Context context, AsyncHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/settings/version", responseHandler);
        }
    }

    //
    // Media
    //

    // Returns a list of Media Folders
    public void getMediaFolders(Context context, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/media/folder", responseHandler);
        }
    }

    // Returns the contents of a Media Folder
    public void getMediaFolderContents(Context context, Long id, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/media/folder/" + id + "/contents", responseHandler);
        }
    }

    // Returns the contents of a Media Element directory
    public void getMediaElementContents(Context context, Long id, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/media/" + id + "/contents", responseHandler);
        }
    }

    // Returns a Media Element by ID
    public void getMediaElement(Context context, Long id, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/media/" + id, responseHandler);
        }
    }

    // Returns a random audio element
    public void getRandomAudioElement(Context context, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/media/audio/random", responseHandler);
        }
    }

    // Returns a list of recently added media elements
    public void getRecentlyAdded(Context context, int limit, Byte type, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/media/recentlyadded/" + limit + (type == null ? "" : "?type=" + type.toString()), responseHandler);
        }
    }

    // Returns a list of recently played media elements
    public void getRecentlyPlayed(Context context, int limit, Byte type, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/media/recentlyplayed/" + limit + (type == null ? "" : "?type=" + type.toString()), responseHandler);
        }
    }

    // Returns a list of artists
    public void getArtists(Context context, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/media/artist", responseHandler);
        }
    }

    // Returns a list of album artists
    public void getAlbumArtists(Context context, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/media/albumartist", responseHandler);
        }
    }

    // Returns a list of artists
    public void getAlbums(Context context, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/media/album", responseHandler);
        }
    }

    // Returns a list of albums for an artist
    public void getAlbumsByArtist(Context context, String artist, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            URIBuilder uri = new URIBuilder();
            uri.setScheme("http")
                    .setHost(getBaseAddress())
                    .setPath("/media/artist/" + artist + "/album");

            client.get(context, URLUtils.encodeURL(uri.toString()), responseHandler);
        }
    }

    // Returns a list of albums for an album artist
    public void getAlbumsByAlbumArtist(Context context, String artist, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            URIBuilder uri = new URIBuilder();
            uri.setScheme("http")
                    .setHost(getBaseAddress())
                    .setPath("/media/albumartist/" + artist + "/album");

            client.get(context, URLUtils.encodeURL(uri.toString()), responseHandler);
        }
    }

    // Returns a list of media elements for an artist and album
    public void getMediaElementsByArtistAndAlbum(Context context, String artist, String album, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            URIBuilder uri = new URIBuilder();
            uri.setScheme("http")
                    .setHost(getBaseAddress())
                    .setPath("/media/artist/" + artist + "/album/" + album + "/");

            client.get(context, URLUtils.encodeURL(uri.toString()), responseHandler);
        }
    }

    // Returns a list of media elements for an album artist and album
    public void getMediaElementsByAlbumArtistAndAlbum(Context context, String artist, String album, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            URIBuilder uri = new URIBuilder();
            uri.setScheme("http")
                    .setHost(getBaseAddress())
                    .setPath("/media/albumartist/" + artist + "/album/" + album + "/");

            client.get(context, URLUtils.encodeURL(uri.toString()), responseHandler);
        }
    }

    // Returns a list of media elements for an album
    public void getMediaElementsByAlbum(Context context, String album, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            URIBuilder uri = new URIBuilder();
            uri.setScheme("http")
                    .setHost(getBaseAddress())
                    .setPath("/media/album/" + album);

            client.get(context, URLUtils.encodeURL(uri.toString()), responseHandler);
        }
    }

    // Returns a list of collections
    public void getCollections(Context context, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/media/collection", responseHandler);
        }
    }

    // Returns a list of media elements for a collection
    public void getMediaElementsByCollection(Context context, String collection, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            URIBuilder uri = new URIBuilder();
            uri.setScheme("http")
                    .setHost(getBaseAddress())
                    .setPath("/media/collection/" + collection);

            client.get(context, URLUtils.encodeURL(uri.toString()), responseHandler);
        }
    }

    //
    // Stream
    //

    // Initialise Stream
    public void initialiseStream(Context context, UUID sessionId, long mediaElementId, String client_id, String files, String codecs, String mchCodecs, String format, int quality, int sampleRate, Integer aTrack, Integer sTrack, boolean direct, boolean update, AsyncHttpResponseHandler responseHandler) {
        if(connection != null) {
            String url = getAddress() + "/stream/initialise/" + sessionId.toString() + "/" + mediaElementId + "?";
            url += client_id == null ? "" : "client=" + client_id + "&";
            url += files == null ? "" : "files=" + files + "&";
            url += codecs == null ? "" : "codecs=" + codecs + "&";
            url += mchCodecs == null ? "" : "mchcodecs=" + mchCodecs + "&";
            url += format == null ? "" : "format=" + format + "&";
            url += "quality=" + String.valueOf(quality) + "&";
            url += "samplerate=" + String.valueOf(sampleRate) + "&";
            url += aTrack == null ? "" : "atrack=" + aTrack + "&";
            url += sTrack == null ? "" : "strack=" + sTrack + "&";
            url += "direct=" + String.valueOf(direct) + "&";
            url += "update=" + String.valueOf(update);

            client.get(context, url, responseHandler);
        }
    }

    //
    // Session
    //

    public void createSession(TextHttpResponseHandler handler) {
        if(connection != null) {
            client.get(getAddress() + "/session/create", handler);
        }
    }

    public void addSession(UUID id) {
        if(connection != null) {
            client.get(getAddress() + "/session/add/" + id, new TextHttpResponseHandler() {

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {

                }

                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {

                }
            });
        }
    }

    public void endSession(UUID id) {
        if(connection != null) {
            client.get(getAddress() + "/session/end/" + id, new TextHttpResponseHandler() {

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {

                }

                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {

                }
            });
        }
    }

    //
    // Job
    //

    // End Job
    public void endJob(UUID id) {
        if(connection != null) {
            client.get(getAddress() + "/job/end/" + id, new TextHttpResponseHandler() {

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {

                }

                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {

                }
            });
        }
    }

    //
    // Test Connection
    //
    public void testConnection(Connection connection, AsyncHttpResponseHandler responseHandler) {
        AsyncHttpClient testClient = new AsyncHttpClient();
        testClient.setBasicAuth(connection.getUsername(), connection.getPassword());
        testClient.get(connection.getUrl() + "/settings/version", responseHandler);
    }
}
