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
import android.os.AsyncTask;
import android.provider.BaseColumns;
import androidx.annotation.Nullable;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Locale;

final class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = BuildConfig.LOG_TAG + ' ' + "DatabaseHelper";
    private static final short VERSION = 5;

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
        db.execSQL("CREATE TABLE " + Engagements.TABLE + "("
                + Engagements.Column.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Engagements.Column.DECISION_POINT + " TEXT NOT NULL, "
                + Engagements.Column.FLAVOUR + " TEXT NOT NULL, "
                + Engagements.Column.CACHED + " INTEGER NOT NULL, "
                + Engagements.Column.RESPONSE + " BLOB NOT NULL, "
                + "UNIQUE("
                + Engagements.Column.DECISION_POINT + ','
                + Engagements.Column.FLAVOUR + ") ON CONFLICT REPLACE)");
        db.execSQL("CREATE TABLE " + ImageMessages.TABLE + "("
                + ImageMessages.Column.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ImageMessages.Column.URL + " TEXT NOT NULL UNIQUE, "
                + ImageMessages.Column.LOCATION + " TEXT NOT NULL, "
                + ImageMessages.Column.NAME + " TEXT NOT NULL UNIQUE, "
                + ImageMessages.Column.SIZE + " INTEGER NOT NULL, "
                + ImageMessages.Column.DOWNLOADED + " INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE " + Actions.TABLE + "("
                + Actions.Column.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Actions.Column.NAME + " TEXT NOT NULL, "
                + Actions.Column.CAMPAIGN_ID + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, "
                + Actions.Column.CACHED + " INTEGER NOT NULL, "
                + Actions.Column.PARAMETERS + " BLOB NOT NULL)");
        db.execSQL("CREATE INDEX " + Actions.TABLE + '_' + Actions.Column.CAMPAIGN_ID + "_idx "
                + "ON " + Actions.TABLE + '(' + Actions.Column.CAMPAIGN_ID + ')');
        db.execSQL("CREATE TABLE " + ETCExecutions.TABLE + "("
                + ETCExecutions.Column.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ETCExecutions.Column.VARIANT_ID + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, "
                + ETCExecutions.Column.EXECUTION_COUNT + " INTEGER NOT NULL )");
        db.execSQL("CREATE INDEX " + ETCExecutions.TABLE + '_' + ETCExecutions.Column.VARIANT_ID + "_idx "
                + "ON " + ETCExecutions.TABLE + '(' + ETCExecutions.Column.VARIANT_ID + ')');
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
            Log.v(TAG, "Upgrading schema to version " + version);

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

                case 3:
                    db.execSQL("CREATE TABLE " + Engagements.TABLE + "("
                            + Engagements.Column.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + Engagements.Column.DECISION_POINT + " TEXT NOT NULL, "
                            + Engagements.Column.FLAVOUR + " TEXT NOT NULL, "
                            + Engagements.Column.CACHED + " INTEGER NOT NULL, "
                            + Engagements.Column.RESPONSE + " BLOB NOT NULL, "
                            + "UNIQUE("
                            + Engagements.Column.DECISION_POINT + ','
                            + Engagements.Column.FLAVOUR + ") ON CONFLICT REPLACE)");
                    break;

                case 4:
                    db.execSQL("CREATE TABLE " + Actions.TABLE + "("
                            + Actions.Column.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + Actions.Column.NAME + " TEXT NOT NULL, "
                            + Actions.Column.CAMPAIGN_ID + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, "
                            + Actions.Column.CACHED + " INTEGER NOT NULL, "
                            + Actions.Column.PARAMETERS + " BLOB NOT NULL)");
                    db.execSQL("CREATE INDEX " + Actions.TABLE + '_' + Actions.Column.CAMPAIGN_ID + "_idx "
                            + "ON " + Actions.TABLE + '(' + Actions.Column.CAMPAIGN_ID + ')');
                    break;
                case 5:
                    db.execSQL("CREATE TABLE " + ETCExecutions.TABLE + "("
                            + ETCExecutions.Column.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + ETCExecutions.Column.VARIANT_ID + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, "
                            + ETCExecutions.Column.EXECUTION_COUNT + " INTEGER NOT NULL )");
                    db.execSQL("CREATE INDEX " + ETCExecutions.TABLE + '_' + ETCExecutions.Column.VARIANT_ID + "_idx "
                            + "ON " + ETCExecutions.TABLE + '(' + ETCExecutions.Column.VARIANT_ID + ')');

            }
        }
    }

    long getEventsSize() {
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery(
                    "SELECT SUM(" + Events.Column.SIZE + ") " +
                            "FROM " + Events.TABLE + ";",
                    new String[]{});

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
                new String[]{Long.toString(id)})
                == 1);
    }

    void removeEventRows() {
        getWritableDatabase().delete(Events.TABLE, null, null);
    }

    Cursor getEngagement(String decisionPoint, String flavour) {
        return getReadableDatabase().query(
                Engagements.TABLE,
                Engagements.Column.all(),
                String.format(
                        Locale.ENGLISH,
                        "%s = ? AND %s = ?",
                        Engagements.Column.DECISION_POINT,
                        Engagements.Column.FLAVOUR),
                new String[]{decisionPoint, flavour},
                null,
                null,
                null);
    }

    boolean insertEngagementRow(
            String decisionPoint,
            String flavour,
            Date cached,
            byte[] response) {

        final ContentValues values = new ContentValues(4);
        values.put(Engagements.Column.DECISION_POINT.toString(), decisionPoint);
        values.put(Engagements.Column.FLAVOUR.toString(), flavour);
        values.put(Engagements.Column.CACHED.toString(), cached.getTime());
        values.put(Engagements.Column.RESPONSE.toString(), response);
        new InsertEngagementAsyncTask().execute(values);
        return true;
    }

    public class InsertEngagementAsyncTask extends AsyncTask<ContentValues, Void, Void>{

        @Override
        protected Void doInBackground(ContentValues... contentValues) {
            ContentValues value = contentValues[0];
            getWritableDatabase().insert(Engagements.TABLE, null, value);
            return null;
        }
    }

    boolean removeEngagementRow(long id) {
        return (getWritableDatabase().delete(
                Engagements.TABLE,
                Engagements.Column.ID + " = ?",
                new String[]{Long.toString(id)})
                == 1);
    }

    void removeEngagementRows() {
        getWritableDatabase().delete(Engagements.TABLE, null, null);
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
                new String[]{url, other},
                null,
                null,
                null);
    }

    boolean removeImageMessage(long id) {
        return (getWritableDatabase().delete(
                ImageMessages.TABLE,
                ImageMessages.Column.ID + " = ?",
                new String[]{Long.toString(id)})
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

    @Nullable
    JSONObject getAction(long campaignId) {
        try (final Cursor cursor = getReadableDatabase().query(
                Actions.TABLE,
                Actions.Column.all(),
                Actions.Column.CAMPAIGN_ID + " = ?",
                new String[]{String.valueOf(campaignId)},
                null,
                null,
                null)) {
            if (cursor.moveToFirst()) {
                try {
                    return new JSONObject(new String(
                            cursor.getBlob(cursor.getColumnIndex(
                                    Actions.Column.PARAMETERS.toString())),
                            "UTF-8"));
                } catch (UnsupportedEncodingException | JSONException e) {
                    Log.w(TAG,
                            "Failed deserialising action into JSON for " + campaignId,
                            e);
                }
            }
        }

        return null;
    }

    boolean insertActionRow(
            String name,
            long campaignId,
            Date cached,
            JSONObject parameters) {

        final ContentValues values = new ContentValues(4);
        values.put(Actions.Column.NAME.toString(), name);
        values.put(Actions.Column.CAMPAIGN_ID.toString(), campaignId);
        values.put(Actions.Column.CACHED.toString(), cached.getTime());
        try {
            values.put(
                    Actions.Column.PARAMETERS.toString(),
                    parameters.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "Failed inserting action: " + parameters, e);
            return false;
        }
        new InsertActionRowAsyncTask().execute(values);
        return true;
    }


    public class InsertActionRowAsyncTask extends AsyncTask<ContentValues, Void, Void>{

        @Override
        protected Void doInBackground(ContentValues... values) {

            getWritableDatabase().insert(Actions.TABLE, null, values[0]);
            return null;
        }
    }

    boolean removeActionRow(long campaignId) {
        return (getWritableDatabase().delete(
                Actions.TABLE,
                Actions.Column.CAMPAIGN_ID + " = ?",
                new String[]{Long.toString(campaignId)})
                == 1);
    }

    void removeActionRows() {
        getWritableDatabase().delete(Actions.TABLE, null, null);
    }

    long getETCExecutionCount(long variantId) {
        long executions = 0;
        try (
                final Cursor cursor = getReadableDatabase().query(
                        ETCExecutions.TABLE,
                        new String[]{ETCExecutions.Column.EXECUTION_COUNT.name()},
                        ETCExecutions.Column.VARIANT_ID + " = ?",
                        new String[]{String.valueOf(variantId)},
                        null,
                        null,
                        null)) {
            if (cursor.moveToFirst()) {
                executions = cursor.getLong(0);
            }
        }

        return executions;
    }


    void recordETCExecution(long campaignId) {
        long etcExecutionCount = getETCExecutionCount(campaignId);
        SQLiteDatabase database = this.getWritableDatabase();
        if (etcExecutionCount == 0) {
            ContentValues values = new ContentValues();
            values.put(ETCExecutions.Column.EXECUTION_COUNT.name(), 1);
            values.put(ETCExecutions.Column.VARIANT_ID.name(), campaignId);
            database.insert(ETCExecutions.TABLE, null, values);
        } else {
            ContentValues values = new ContentValues();
            values.put(ETCExecutions.Column.EXECUTION_COUNT.name(), etcExecutionCount + 1);

            database.update(ETCExecutions.TABLE, values, ETCExecutions.Column.VARIANT_ID + " = ?", new String[]{String.valueOf(campaignId)});
        }

    }

    void clearETCExecutions() {
        getWritableDatabase().delete(ETCExecutions.TABLE, null, null);
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

        private Events() {
        }
    }

    static final class Engagements {

        static final String TABLE = "engagements";

        enum Column {
            ID {
                @Override
                public String toString() {
                    return BaseColumns._ID;
                }
            },
            DECISION_POINT,
            FLAVOUR,
            CACHED,
            RESPONSE;

            @Override
            public String toString() {
                return super.toString().toLowerCase(Locale.ENGLISH);
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

    private static final class Actions {

        static final String TABLE = "actions";

        enum Column {
            ID {
                @Override
                public String toString() {
                    return BaseColumns._ID;
                }
            },
            NAME,
            CAMPAIGN_ID,
            CACHED,
            PARAMETERS;

            @Override
            public String toString() {
                return name().toLowerCase();
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

    private static final class ETCExecutions {

        static final String TABLE = "etc_executions";

        enum Column {
            ID {
                @Override
                public String toString() {
                    return BaseColumns._ID;
                }
            },
            VARIANT_ID,
            EXECUTION_COUNT;

            @Override
            public String toString() {
                return name().toLowerCase();
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
