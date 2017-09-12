/*
 * Copyright (c) 2016 deltaDNA Ltd. All rights reserved.
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

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.util.Log;

import com.deltadna.android.sdk.helpers.Settings;
import com.deltadna.android.sdk.util.CloseableIterator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class EventStore extends BroadcastReceiver {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + EventStore.class.getSimpleName();
    private static final String DIRECTORY = "events" + File.separator;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int EVENTS_LIMIT = 1024 * 1024;
    private static final int STORE_LIMIT = 5 * EVENTS_LIMIT;
    
    private static final IntentFilter FILTER;
    static {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addDataScheme("file");
        
        FILTER = filter;
    }
    
    private static final Lock LEGACY_MIGRATION_LOCK = new ReentrantLock();
    
    private final Context context;
    private final Settings settings;
    private final Preferences prefs;
    
    private final DbHelper db;
    
    @Nullable
    private final MessageDigest sha1;
    
    EventStore(Context context, Settings settings, Preferences prefs) {
        this.context = context;
        this.settings = settings;
        this.prefs = prefs;
        
        db = new DbHelper(context);
        
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, "Hashing will be disabled" , e);
        } finally {
            sha1 = digest;
        }
        
        context.registerReceiver(this, FILTER);
        
        prepare();
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            Log.d(TAG, "Received media mounted broadcast");
            prepare();
        } else {
            Log.w(TAG, "Unexpected broadcast action: " + action);
        }
    }
    
    /**
     * Adds content to the store in a non-blocking manner.
     *
     * @param content the content to be saved
     */
    synchronized void add(String content) {
        final byte[] bytes = content.getBytes(UTF8);
        if (bytes.length > EVENTS_LIMIT) {
            Log.w(TAG, "Skipping " + content + " due to bulk events limit");
            return;
        } else if (db.getEventsSize() + bytes.length > STORE_LIMIT) {
            Log.w(TAG, "Skipping " + content + " due to full event store");
            return;
        }
        
        new SaveTask(bytes).execute();
    }

    synchronized CloseableIterator<EventStoreItem> items() {
        return new EventIterator(db, context);
    }

    synchronized void clear() {
        db.removeEventRows();
        for (final Location location : Location.values()) {
            if (location.available()) {
                final File dir = location.directory(context, DIRECTORY);
                for (final File file : dir.listFiles()) {
                    if (!file.delete()) {
                        Log.w(TAG, "Failed to clear " + file);
                    }
                }
            } else {
                Log.w(TAG, location + " not available for clearing");
            }
        }
    }
    
    private void prepare() {
        for (final Location location : Location.values()) {
            if (location.available()) {
                final File dir = location.directory(context, DIRECTORY);
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        Log.w(TAG, "Failed creating " + dir);
                    } else {
                        Log.d(TAG, "Created " + dir);
                    }
                }
            } else {
                Log.w(TAG, location + " not available");
            }
        }
        
        new MigrateLegacyStore(prefs).execute();
    }
    
    @Nullable
    private String md5(byte[] content) {
        if (sha1 == null) return null;
        
        final StringBuilder hex = new StringBuilder();
        for (final byte aByte : sha1.digest(content)) {
            hex.append(String.format("%02x", aByte));
        }
        return hex.toString();
    }
    
    private final class MigrateLegacyStore extends AsyncTask<Void, Void, Void> {
        
        private final Preferences prefs;
        
        private final File directory;
        private final LegacyEventStore store;
        
        MigrateLegacyStore(Preferences prefs) {
            this.prefs = prefs;
            
            directory = new File(
                    context.getExternalFilesDir(null),
                    "/ddsdk/events/");
            store = new LegacyEventStore(
                    directory.getPath(),
                    prefs,
                    false,
                    true);
        }
        
        @Override
        protected Void doInBackground(Void... params) {
            LEGACY_MIGRATION_LOCK.lock();
            try {
                if (!directory.exists()) {
                    return null;
                } else {
                    Log.d(TAG, "Migrating legacy store");
                }
                
                // migrate
                store.swap();
                for (final String item : store.read()) {
                    add(item);
                }
                store.clearOutfile();
                store.clear();
                
                // clean files
                for (final File file : directory.listFiles()) {
                    if (!file.delete()) {
                        Log.w(TAG, "Failed to delete legacy " + file);
                    }
                }
                if (!directory.delete()) {
                    Log.w(TAG, "Failed to delete legacy files in " + directory);
                } else {
                    Log.d(TAG, "Deleted legacy files in " + directory);
                }
                
                // clean prefs
                final SharedPreferences.Editor editor = prefs.getPrefs().edit();
                editor.remove("DDSDK_EVENT_IN_FILE");
                editor.remove("DDSDK_EVENT_OUT_FILE");
                editor.apply();
                
                return null;
            } finally {
                LEGACY_MIGRATION_LOCK.unlock();
            }
        }
    }
    
    private final class SaveTask extends AsyncTask<Void, Void, Void> {
        
        private final byte[] content;
        private final long time;
        
        SaveTask(byte[] content) {
            this.content = content;
            time = System.currentTimeMillis();
        }
        
        @Override
        protected Void doInBackground(Void... params) {
            final String name = UUID.randomUUID().toString();
            final Location location;
            if (settings.isUseInternalStorageForEvents()) {
                location = Location.INTERNAL;
            } else if (Location.EXTERNAL.available()) {
                location = Location.EXTERNAL;
            } else {
                Log.w(TAG, String.format(
                        Locale.US,
                        "%s not available, falling back to %s",
                        Location.EXTERNAL,
                        Location.INTERNAL));
                location = Location.INTERNAL;
            }
            final String hash = md5(content);
            
            final File file = new File(
                    location.directory(context, DIRECTORY),
                    name);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(file);
                out.write(content);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Failed opening stream for " + file, e);
                return null;
            } catch (IOException e) {
                Log.e(TAG, "Failed writing to stream for " + file, e);
                //noinspection ResultOfMethodCallIgnored
                file.delete();
                return null;
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Failed closing stream for " + file, e);
                    }
                }
            }
            
            if (!db.insertEventRow(time, location, name, hash, file.length())) {
                Log.e(TAG, "Failed inserting " + new String(content));
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
            
            return null;
        }
    }
    
    private static final class DbHelper extends SQLiteOpenHelper {
        
        private static final String TABLE_EVENTS = "Events";
        
        private static final String EVENTS_ID = BaseColumns._ID;
        private static final String EVENTS_TIME = "Time";
        private static final String EVENTS_NAME = "Name";
        private static final String EVENTS_LOCATION = "Location";
        private static final String EVENTS_HASH = "Hash";
        private static final String EVENTS_SIZE = "Size";
        
        DbHelper(Context context) {
            super(context, "com.deltadna.android.sdk", null, 1);
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_EVENTS + "("
                    + EVENTS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + EVENTS_TIME + " INTEGER NOT NULL, "
                    + EVENTS_LOCATION + " TEXT NOT NULL, "
                    + EVENTS_NAME + " TEXT NOT NULL UNIQUE, "
                    + EVENTS_HASH + " TEXT, "
                    + EVENTS_SIZE + " INTEGER NOT NULL)");
        }
        
        @Override
        public void onUpgrade(
                SQLiteDatabase db,
                int oldVersion,
                int newVersion) {}
        
        long getEventsSize() {
            final Cursor cursor = getReadableDatabase().rawQuery(
                    "SELECT SUM(" + EVENTS_SIZE + ") FROM " + TABLE_EVENTS + ";",
                    new String[] {});
            final long result = (cursor.moveToFirst()) ? cursor.getLong(0) : 0;
            cursor.close();
            return result;
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
                            EVENTS_ID, EVENTS_TIME, EVENTS_LOCATION, EVENTS_NAME, EVENTS_SIZE, EVENTS_SIZE,
                            TABLE_EVENTS,
                            TABLE_EVENTS, EVENTS_ID, EVENTS_ID,
                            EVENTS_ID,
                            EVENTS_SIZE, EVENTS_LIMIT,
                            EVENTS_TIME),
                    new String[]{});
        }
        
        boolean insertEventRow(
                long time,
                Location location,
                String name,
                @Nullable String hash,
                long size) {
            
            final ContentValues values = new ContentValues(4);
            values.put(EVENTS_TIME, time);
            values.put(EVENTS_LOCATION, location.name());
            values.put(EVENTS_NAME, name);
            values.put(EVENTS_HASH, hash);
            values.put(EVENTS_SIZE, size);
            
            return (getWritableDatabase().insert(TABLE_EVENTS, null, values)
                    != -1);
        }
        
        boolean removeEventRow(long id) {
            return (getWritableDatabase().delete(
                    TABLE_EVENTS,
                    EVENTS_ID + " = ?",
                    new String[] { Long.toString(id) })
                    == 1);
        }
        
        void removeEventRows() {
            getWritableDatabase().delete(TABLE_EVENTS, null, null);
        }
    }
    
    private static final class EventIterator implements
            CloseableIterator<EventStoreItem> {
        
        private final DbHelper db;
        private final Context context;
        
        private final Cursor cursor;
        
        EventIterator(DbHelper db, Context context) {
            this.db = db;
            this.context = context;
            
            cursor = db.getEventRows();
        }
        
        @Override
        public boolean hasNext() {
            return (cursor.getCount() > 0 && !cursor.isLast());
        }
        
        @Override
        public EventStoreItem next() {
            if (!cursor.moveToNext()) throw new NoSuchElementException();
            
            final Location location = getCurrentLocation();
            
            return new EventStoreItem() {
                @Override
                public boolean available() {
                    return location.available();
                }
                
                @Override
                @Nullable
                public String get() {
                    final File file = new File(
                            location.directory(context, DIRECTORY),
                            getCurrentName());
                    final StringBuilder builder = new StringBuilder();
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(
                                new FileInputStream(file)));
                        
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "Failed opening stream for " + file, e);
                        return null;
                    } catch (IOException e) {
                        Log.e(TAG, "Failed reading stream for " + file, e);
                        return null;
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                Log.w(TAG, "Failed closing stream for " + file, e);
                            }
                        }
                    }
                    
                    return builder.toString();
                }
            };
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void close(boolean clear) {
            if (clear) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    if (!db.removeEventRow(getCurrentId())) {
                        Log.w(TAG, "Failed to remove event row");
                    }
                    
                    final File file = new File(
                            getCurrentLocation().directory(context, DIRECTORY),
                            getCurrentName());
                    if (!file.delete()) {
                        Log.w(TAG, "Failed deleting " + file);
                    }
                    
                    cursor.moveToNext();
                }
            }
            
            cursor.close();
        }
        
        private long getCurrentId() {
            return cursor.getLong(
                    cursor.getColumnIndex(DbHelper.EVENTS_ID));
        }
        
        private Location getCurrentLocation() {
            return Location.valueOf(cursor.getString(
                    cursor.getColumnIndex(DbHelper.EVENTS_LOCATION)));
        }
        
        private String getCurrentName() {
            return cursor.getString(
                    cursor.getColumnIndex(DbHelper.EVENTS_NAME));
        }
    }
}
