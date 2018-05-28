/*
 * Copyright (c) 2018 deltaDNA Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.deltadna.android.sdk;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Date;
import java.util.Locale;

final class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String TAG = BuildConfig.LOG_TAG + ' ' + "DbHelper";
    private static final short VERSION = 2;
    
    DatabaseHelper(Context context) {
        super(context, "com.deltadna.android.sdk", null, VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Events.TABLE + "("
                + Events.Column.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Events.Column.TIME + " INTEGER NOT NULL, "
                + Events.Column.LOCATION + " TEXT NOT NULL, "
                + Events.Column.NAME + " TEXT NOT NULL UNIQUE, "
                + Events.Column.HASH + " TEXT, "
                + Events.Column.SIZE + " INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE " + ImageMessages.TABLE + "("
                + ImageMessages.Column.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ImageMessages.Column.URL + " TEXT NOT NULL UNIQUE, "
                + ImageMessages.Column.LOCATION + " TEXT NOT NULL, "
                + ImageMessages.Column.NAME + " TEXT NOT NULL UNIQUE, "
                + ImageMessages.Column.SIZE + " INTEGER NOT NULL, "
                + ImageMessages.Column.DOWNLOADED + " INTEGER NOT NULL)");
    }
    
    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {
        
        Log.d(TAG, String.format(
                Locale.ENGLISH,
                "Upgrading %s from version %d to %d",
                db,
                oldVersion,
                newVersion));
        
        int version = oldVersion;
        while (version++ < newVersion) {
            switch (version) {
                case 2:
                    db.execSQL("CREATE TABLE " + ImageMessages.TABLE + "("
                            + ImageMessages.Column.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + ImageMessages.Column.URL + " TEXT NOT NULL UNIQUE, "
                            + ImageMessages.Column.LOCATION + " TEXT NOT NULL, "
                            + ImageMessages.Column.NAME + " TEXT NOT NULL UNIQUE, "
                            + ImageMessages.Column.SIZE + " INTEGER NOT NULL, "
                            + ImageMessages.Column.DOWNLOADED + " INTEGER NOT NULL)");
                    break;
            }
        }
    }
    
    long getEventsSize() {
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery(
                    "SELECT SUM(" + Events.Column.SIZE + ") " +
                    "FROM " + Events.TABLE + ";",
                    new String[] {});
            
            return (cursor.moveToFirst()) ? cursor.getLong(0) : 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }
    
    Cursor getEventRows() {
        return getReadableDatabase().rawQuery(
                String.format(
                        Locale.US,
                        "SELECT e.%s, e.%s, e.%s, e.%s, e.%s, SUM(e1.%s) AS Total "
                                + "FROM %s e "
                                + "JOIN %s e1 ON e1.%s <= e.%s "
                                + "GROUP BY e.%s "
                                + "HAVING SUM(e1.%s) <= %d "
                                + "ORDER BY e.%s ASC;",
                        Events.Column.ID, Events.Column.TIME, Events.Column.LOCATION, Events.Column.NAME, Events.Column.SIZE, Events.Column.SIZE,
                        Events.TABLE,
                        Events.TABLE, Events.Column.ID, Events.Column.ID,
                        Events.Column.ID,
                        Events.Column.SIZE, EventStore.EVENTS_LIMIT,
                        Events.Column.TIME),
                new String[]{});
    }
    
    boolean insertEventRow(
            long time,
            Location location,
            String name,
            @Nullable String hash,
            long size) {
        
        final ContentValues values = new ContentValues(5);
        values.put(Events.Column.TIME.toString(), time);
        values.put(Events.Column.LOCATION.toString(), location.name());
        values.put(Events.Column.NAME.toString(), name);
        values.put(Events.Column.HASH.toString(), hash);
        values.put(Events.Column.SIZE.toString(), size);
        
        return (getWritableDatabase().insert(Events.TABLE, null, values)
                != -1);
    }
    
    boolean removeEventRow(long id) {
        return (getWritableDatabase().delete(
                Events.TABLE,
                Events.Column.ID + " = ?",
                new String[] { Long.toString(id) })
                == 1);
    }
    
    void removeEventRows() {
        getWritableDatabase().delete(Events.TABLE, null, null);
    }
    
    Cursor getImageMessages() {
        return getReadableDatabase().query(
                ImageMessages.TABLE,
                ImageMessages.Column.all(),
                null,
                null,
                null,
                null,
                null);
    }
    
    Cursor getImageMessage(String url) {
        final String other;
        if (url.startsWith("http://")) {
            other = "https" + url.substring("http".length(), url.length());
        } else if (url.startsWith("https://")) {
            other = "http" + url.substring("https".length(), url.length());
        } else {
            other = url;
        }
        
        return getReadableDatabase().query(
                ImageMessages.TABLE,
                ImageMessages.Column.all(),
                String.format(
                        Locale.ENGLISH,
                        "%s = ? OR %s = ?",
                        ImageMessages.Column.URL,
                        ImageMessages.Column.URL),
                new String[] { url, other },
                null,
                null,
                null);
    }
    
    boolean removeImageMessage(long id) {
        return (getWritableDatabase().delete(
                ImageMessages.TABLE,
                ImageMessages.Column.ID + " = ?",
                new String[] { Long.toString(id) })
                == 1);
    }
    
    void removeImageMessageRows() {
        getWritableDatabase().delete(ImageMessages.TABLE, null, null);
    }
    
    boolean insertImageMessage(
            String url,
            Location location,
            String name,
            long size,
            Date downloaded) {
        
        final ContentValues values = new ContentValues(4);
        values.put(ImageMessages.Column.URL.toString(), url);
        values.put(ImageMessages.Column.LOCATION.toString(), location.name());
        values.put(ImageMessages.Column.NAME.toString(), name);
        values.put(ImageMessages.Column.SIZE.toString(), size);
        values.put(ImageMessages.Column.DOWNLOADED.toString(), downloaded.getTime());
        
        return (getWritableDatabase().insert(ImageMessages.TABLE, null, values)
                != -1);
    }
    
    static final class Events {
        
        static final String TABLE = "Events";
        enum Column {
            ID {
                @Override
                public String toString() {
                    return BaseColumns._ID;
                }
            },
            TIME,
            NAME,
            LOCATION,
            HASH,
            SIZE;
            
            private final String value;
            
            Column() {
                value = name().substring(0, 1).toUpperCase(Locale.ENGLISH)
                        + name().substring(1).toLowerCase(Locale.ENGLISH);
            }
            
            @Override
            public String toString() {
                return value;
            }
        }
        
        private Events() {}
    }
    
    static final class ImageMessages {
        
        static final String TABLE = "ImageMessages";
        enum Column {
            ID {
                @Override
                public String toString() {
                    return BaseColumns._ID;
                }
            },
            URL,
            LOCATION,
            NAME,
            SIZE,
            DOWNLOADED;
            
            private final String value;
            
            Column() {
                value = name().substring(0, 1).toUpperCase(Locale.ENGLISH)
                        + name().substring(1).toLowerCase(Locale.ENGLISH);
            }
            
            @Override
            public String toString() {
                return value;
            }
            
            static String[] all() {
                final String[] result = new String[values().length];
                for (int i = 0; i < values().length; i++) {
                    result[i] = values()[i].toString();
                }
                return result;
            }
        }
    }
}
