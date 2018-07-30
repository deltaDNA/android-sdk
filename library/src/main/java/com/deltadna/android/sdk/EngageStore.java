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

import android.database.Cursor;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.deltadna.android.sdk.helpers.Settings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.deltadna.android.sdk.DatabaseHelper.Engagements.Column.CACHED;
import static com.deltadna.android.sdk.DatabaseHelper.Engagements.Column.ID;
import static com.deltadna.android.sdk.DatabaseHelper.Engagements.Column.RESPONSE;

final class EngageStore {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + EngageStore.class.getSimpleName();
    
    private final DatabaseHelper database;
    private final Settings settings;
    
    EngageStore(DatabaseHelper database, File path, Settings settings) {
        this.database = database;
        this.settings = settings;
        
        new CleanUp(path).run();
    }
    
    void put(Engagement engagement) {
        if (engagement.isSuccessful()) {
            Log.v(TAG, "Inserting " + engagement);
            
            try {
                //noinspection ConstantConditions
                database.insertEngagementRow(
                        engagement.getDecisionPoint(),
                        engagement.getFlavour(),
                        new Date(),
                        Base64.encode(
                                engagement.getJson().toString().getBytes("UTF-8"),
                                Base64.DEFAULT));
            } catch (UnsupportedEncodingException e) {
                Log.w(  TAG,
                        "Failed serialising engagement response",
                        e);
            }
        }
    }
    
    @Nullable
    JSONObject get(Engagement engagement) {
        if (settings.getEngageCacheExpiry() == 0) return null;
        
        try (final Cursor cursor = database.getEngagement(
                engagement.name, engagement.flavour)) {
            if (cursor.moveToFirst()) {
                final Date cached = new Date(cursor.getLong(
                        cursor.getColumnIndex(CACHED.toString())));
                
                if ((   new Date().getTime() - cached.getTime())
                        > TimeUnit.SECONDS.toMillis(settings.getEngageCacheExpiry())) {
                    database.removeEngagementRow(cursor.getLong(
                            cursor.getColumnIndex(ID.toString())));
                } else {
                    try {
                        return new JSONObject(new String(
                                Base64.decode(
                                        cursor.getBlob(
                                                cursor.getColumnIndex(RESPONSE.toString())),
                                        Base64.DEFAULT),
                                "UTF-8"));
                    } catch (UnsupportedEncodingException | JSONException e) {
                        Log.w(  TAG,
                                "Failed deserialising engagement response from cache",
                                e);
                    }
                }
            }
        }
        
        return null;
    }
    
    void clear() {
        Log.v(TAG, "Clearing stored engagements");
        database.removeEngagementRows();
    }
    
    private static final class CleanUp implements Runnable {
        
        private final File oldArchive;
        
        CleanUp(File path) {
            this.oldArchive = new File(path, "ENGAGEMENTS");
        }
        
        @Override
        public void run() {
            if (oldArchive.exists() && oldArchive.delete()) {
                Log.d(BuildConfig.LOG_TAG, "Deleted " + oldArchive);
            }
        }
    }
}
