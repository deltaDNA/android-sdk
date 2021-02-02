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

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import androidx.annotation.Nullable;
import android.util.Log;
import com.deltadna.android.sdk.DatabaseHelper.Events;
import com.deltadna.android.sdk.helpers.Settings;
import com.deltadna.android.sdk.util.CloseableIterator;

import java.io.*;
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
    static final int EVENTS_LIMIT = 1024 * 1024;
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
    private final DatabaseHelper db;
    private final Settings settings;
    private final Preferences prefs;

    @Nullable
    private final MessageDigest sha1;

    EventStore(
            Context context,
            DatabaseHelper db,
            Settings settings,
            Preferences prefs) {

        this.context = context;
        this.db = db;
        this.settings = settings;
        this.prefs = prefs;

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, "Hashing will be disabled", e);
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
        Log.v(TAG, "Adding " + content);

        final byte[] bytes = content.getBytes(UTF8);
        if (bytes.length > EVENTS_LIMIT) {
            Log.w(TAG, "Skipping " + content + " due to bulk events limit");
            return;
        } else {
            new CheckEventsLimitTask(db, bytes, STORE_LIMIT, content).execute();
        }

    }

    private class CheckEventsLimitTask extends AsyncTask<Void, Void, Boolean> {

        private DatabaseHelper db;
        private byte[] bytes;
        private int storeLimit;
        private String content;

        public CheckEventsLimitTask(DatabaseHelper db, byte[] bytes, int storeLimit, String content) {

            this.db = db;
            this.bytes = bytes;
            this.storeLimit = storeLimit;
            this.content = content;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return db.getEventsSize() + bytes.length < storeLimit;
        }

        @Override
        protected void onPostExecute(Boolean isEnoughSpace) {
            if (!isEnoughSpace) {
                Log.w(TAG, "Skipping " + content + " due to full event store");
            } else {
                new SaveTask(bytes).execute();
            }
        }
    }


    synchronized CloseableIterator<EventStoreItem> items() {
        return new EventIterator(db, context);
    }

    synchronized void clear() {
        db.removeEventRows();
        for (final Location location : Location.values()) {
            if (location.available()) {
                final File dir = location.storage(context, DIRECTORY);
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
                final File dir = location.storage(context, DIRECTORY);
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
                    location.storage(context, DIRECTORY),
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

            boolean dbInsertSucceeded;
            try {
                dbInsertSucceeded = db.insertEventRow(time, location, name, hash, file.length());
            } catch (SQLiteException e) {
                Log.e(TAG, "An error occurred when trying to insert event row into the database", e);
                dbInsertSucceeded = false;
            }

            if (!dbInsertSucceeded) {
                Log.w(TAG, "Failed inserting " + new String(content));
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            } else {
                Log.v(TAG, "Inserted " + new String(content));
            }

            return null;
        }
    }

    private static final class EventIterator implements
            CloseableIterator<EventStoreItem> {

        private final DatabaseHelper db;
        private final Context context;

        private final Cursor cursor;

        EventIterator(DatabaseHelper db, Context context) {
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
                            location.storage(context, DIRECTORY),
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
        public void close(Mode mode) {
            try {
                switch (mode) {
                    case ALL:
                        cursor.moveToFirst();
                        while (!cursor.isAfterLast()) {
                            removeRow();
                            cursor.moveToNext();
                        }

                        break;

                    case UP_TO_CURRENT:
                        final int position = cursor.getPosition();
                        cursor.moveToFirst();
                        while (cursor.getPosition() < position) {
                            removeRow();
                            cursor.moveToNext();
                        }

                        break;
                }
            } finally {
                cursor.close();
            }
        }

        private long getCurrentId() {
            return cursor.getLong(
                    cursor.getColumnIndex(Events.Column.ID.toString()));
        }

        private Location getCurrentLocation() {
            return Location.valueOf(cursor.getString(
                    cursor.getColumnIndex(Events.Column.LOCATION.toString())));
        }

        private String getCurrentName() {
            return cursor.getString(
                    cursor.getColumnIndex(Events.Column.NAME.toString()));
        }

        private void removeRow() {
            if (!db.removeEventRow(getCurrentId())) {
                Log.w(TAG, "Failed to remove event row");
            }

            final File file = new File(
                    getCurrentLocation().storage(context, DIRECTORY),
                    getCurrentName());
            if (!file.delete()) {
                Log.w(TAG, "Failed deleting " + file);
            }
        }
    }
}
