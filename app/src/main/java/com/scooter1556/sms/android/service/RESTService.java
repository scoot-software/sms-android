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
import android.util.Log;

import com.google.gson.Gson;
import com.loopj.android.http.*;
import com.scooter1556.sms.android.domain.ClientProfile;
import com.scooter1556.sms.android.utils.URLUtils;
import com.scooter1556.sms.android.domain.Connection;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.entity.StringEntity;

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
    public void getMediaFolderContents(Context context, UUID id, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/media/folder/" + id + "/contents", responseHandler);
        }
    }

    // Returns the contents of a Media Element directory
    public void getMediaElementContents(Context context, UUID id, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/media/" + id + "/contents", responseHandler);
        }
    }

    // Returns a Media Element by ID
    public void getMediaElement(Context context, UUID id, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            String url = getAddress() + "/media/" + id;
            Log.d(TAG, url);
            client.get(context, url, responseHandler);
        }
    }

    // Returns random media elements
    public void getRandomMediaElements(Context context, int limit, Byte type, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/media/random/" + limit + "?" + (type == null ? "" : "type=" + type.toString()), responseHandler);
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
                    .setPath("/media/artist/" + artist + "/album/" + album);

            client.get(context, URLUtils.encodeURL(uri.toString()), responseHandler);
        }
    }

    // Returns a list of media elements for an album artist and album
    public void getMediaElementsByAlbumArtistAndAlbum(Context context, String artist, String album, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            URIBuilder uri = new URIBuilder();
            uri.setScheme("http")
                    .setHost(getBaseAddress())
                    .setPath("/media/albumartist/" + artist + "/album/" + album);

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
    // Playlists
    //

    // Returns playlists for current user
    public void getPlaylists(Context context, JsonHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(context, getAddress() + "/playlist", responseHandler);
        }
    }

    // Returns a list of media elements for a playlist
    public void getPlaylistContents(Context context, UUID id, boolean random, JsonHttpResponseHandler responseHandler) {
        if (connection != null) {
            URIBuilder uri = new URIBuilder();
            uri.setScheme("http")
                    .setHost(getBaseAddress())
                    .setPath("/playlist/" + id + "/contents?" + "random=" + random);

            client.get(context, URLUtils.encodeURL(uri.toString()), responseHandler);
        }
    }

    //
    // Session
    //

    public void addSession(Context context, UUID id, ClientProfile profile, TextHttpResponseHandler responseHandler) {
        if(connection != null) {
            String url = getAddress() + "/session/add" + (id == null ? "" : "?id=" + id);

            Gson gson = new Gson();
            String jProfile = gson.toJson(profile);
            StringEntity entity = null;

            try {
                entity = new StringEntity(jProfile);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, null, e);
                return;
            }

            client.get(context, url, entity, "application/json", responseHandler);
        }
    }

    public void updateClientProfile(Context context, UUID id, ClientProfile profile, BlackholeHttpResponseHandler responseHandler) {
        if(connection != null) {
            if(id == null || profile == null) {
                return;
            }

            String url = getAddress() + "/session/update/" + id.toString();

            Gson gson = new Gson();
            String jProfile = gson.toJson(profile);
            StringEntity entity = null;

            try {
                entity = new StringEntity(jProfile);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, null, e);
                return;
            }

            client.post(context, url, entity, "application/json", responseHandler);
        }
    }

    public void endSession(UUID id, TextHttpResponseHandler responseHandler) {
        if(connection != null) {
            client.get(getAddress() + "/session/end/" + id.toString(), responseHandler);
        }
    }

    // End Job
    public void endJob(UUID sid, UUID meid) {
        if(connection != null) {
            client.get(getAddress() + "/session/end/" + sid.toString() + "/" + meid.toString(), new TextHttpResponseHandler() {

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {

                }

                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {

                }
            });
        }
    }

    // End Job
    public void endJobs(UUID sid) {
        if(connection != null) {
            client.get(getAddress() + "/session/end/" + sid.toString() + "/all", new TextHttpResponseHandler() {

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
    // Stream
    //
    public void getStream(Context context, UUID sid, UUID meid, TextHttpResponseHandler responseHandler) {
        if (connection != null) {
            URIBuilder uri = new URIBuilder();
            uri.setScheme("http")
                    .setHost(getBaseAddress())
                    .setPath("/stream/" + sid + "/" + meid);

            client.head(context, URLUtils.encodeURL(uri.toString()), responseHandler);
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
