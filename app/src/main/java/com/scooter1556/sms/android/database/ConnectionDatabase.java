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
package com.scooter1556.sms.android.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Base64;

import com.scooter1556.sms.android.domain.Connection;

import java.util.ArrayList;
import java.util.List;

public class ConnectionDatabase extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "Connections";

    private static final String SQL_ENTRIES = ConnectionEntry._ID + "," +
                                              ConnectionEntry.COLUMN_NAME_TITLE + "," +
                                              ConnectionEntry.COLUMN_NAME_URL + "," +
                                              ConnectionEntry.COLUMN_NAME_USERNAME + "," +
                                              ConnectionEntry.COLUMN_NAME_PASSWORD;

    private static final String SQL_ENTRIES_WITH_TYPE = ConnectionEntry._ID + " INTEGER PRIMARY KEY," +
                                                        ConnectionEntry.COLUMN_NAME_TITLE + " TEXT," +
                                                        ConnectionEntry.COLUMN_NAME_URL + " TEXT," +
                                                        ConnectionEntry.COLUMN_NAME_USERNAME + " TEXT," +
                                                        ConnectionEntry.COLUMN_NAME_PASSWORD + " TEXT";

    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + DATABASE_NAME + " (" + SQL_ENTRIES_WITH_TYPE + ")";

    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + DATABASE_NAME;

    public ConnectionDatabase(Context context) {
        super(context, DATABASE_NAME + ".db", null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion == 1 && newVersion >= 2) {
            db.execSQL("ALTER TABLE " + DATABASE_NAME + " ADD COLUMN " + ConnectionEntry.COLUMN_NAME_ALT_URL + " TEXT");
        }

        if(oldVersion == 2 && newVersion >= 3) {
            db.execSQL("ALTER TABLE " + DATABASE_NAME + " RENAME TO " + DATABASE_NAME + "_old");
            db.execSQL(SQL_CREATE_ENTRIES);
            db.execSQL("INSERT INTO " + DATABASE_NAME + "(" + SQL_ENTRIES + ") SELECT "
                    + SQL_ENTRIES + " FROM " + DATABASE_NAME + "_old");
            db.execSQL("DROP TABLE " + DATABASE_NAME + "_old;");
        }
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    // Add a new connection to the database. Returns the connection ID or -1 if an error occurs.
    public long addConnection(Connection connection) {
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        // Populate values
        ContentValues values = new ContentValues();
        values.put(ConnectionEntry.COLUMN_NAME_TITLE, connection.getTitle());
        values.put(ConnectionEntry.COLUMN_NAME_URL, connection.getUrl());
        values.put(ConnectionEntry.COLUMN_NAME_USERNAME, connection.getUsername());
        values.put(ConnectionEntry.COLUMN_NAME_PASSWORD, Base64.encodeToString(connection.getPassword().getBytes(), Base64.DEFAULT));

        // Insert the new connection
        return db.insert(DATABASE_NAME, null, values);
    }

    public Connection getConnection(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Retrieve connection details
        Cursor cursor = db.query(DATABASE_NAME, new String[] { ConnectionEntry._ID, ConnectionEntry.COLUMN_NAME_TITLE, ConnectionEntry.COLUMN_NAME_URL, ConnectionEntry.COLUMN_NAME_USERNAME, ConnectionEntry.COLUMN_NAME_PASSWORD }, ConnectionEntry._ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);

        // Make sure we retrieved the information
        if (cursor == null) {
            return null;
        }

        // Populate values
        cursor.moveToFirst();

        Connection connection = new Connection();
        connection.setID(Integer.parseInt(cursor.getString(0)));
        connection.setTitle(cursor.getString(1));
        connection.setUrl(cursor.getString(2));
        connection.setUsername(cursor.getString(3));
        connection.setPassword(new String(Base64.decode(cursor.getString(4), Base64.DEFAULT)));

        cursor.close();

        return connection;
    }

    public List<Connection> getAllConnections() {
        List<Connection> connectionList = new ArrayList<>();

        // Select All Query
        SQLiteDatabase db = this.getWritableDatabase();

        // Retrieve connection details
        Cursor cursor = db.query(DATABASE_NAME, new String[]{ConnectionEntry._ID, ConnectionEntry.COLUMN_NAME_TITLE, ConnectionEntry.COLUMN_NAME_URL, ConnectionEntry.COLUMN_NAME_USERNAME, ConnectionEntry.COLUMN_NAME_PASSWORD}, null, null, null, null, null, null);

        // Add all connections to the list
        if (cursor.moveToFirst()) {
            do {
                Connection connection = new Connection();
                connection.setID(Integer.parseInt(cursor.getString(0)));
                connection.setTitle(cursor.getString(1));
                connection.setUrl(cursor.getString(2));
                connection.setUsername(cursor.getString(3));
                connection.setPassword(new String(Base64.decode(cursor.getString(4), Base64.DEFAULT)));

                connectionList.add(connection);

            } while (cursor.moveToNext());
        }

        cursor.close();

        // return contact list
        return connectionList;
    }

    public int getConnectionCount() {
        String countQuery = "SELECT * FROM " + DATABASE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        return cursor.getCount();
    }

    public int updateConnection(Connection connection) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ConnectionEntry.COLUMN_NAME_TITLE, connection.getTitle());
        values.put(ConnectionEntry.COLUMN_NAME_URL, connection.getUrl());
        values.put(ConnectionEntry.COLUMN_NAME_USERNAME, connection.getUsername());
        values.put(ConnectionEntry.COLUMN_NAME_PASSWORD, Base64.encodeToString(connection.getPassword().getBytes(), Base64.DEFAULT));

        // Update row
        return db.update(DATABASE_NAME, values, ConnectionEntry._ID + " = ?",
                new String[]{String.valueOf(connection.getID())});
    }

    public void deleteConnection(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DATABASE_NAME, ConnectionEntry._ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    // Database definition
    public static abstract class ConnectionEntry implements BaseColumns {
        public static final String COLUMN_NAME_TITLE = "Title";
        public static final String COLUMN_NAME_URL = "Url";
        public static final String COLUMN_NAME_USERNAME = "Username";
        public static final String COLUMN_NAME_PASSWORD = "Password";

        // Deprecated columns
        public static final String COLUMN_NAME_ALT_URL = "AltUrl";
    }
}
