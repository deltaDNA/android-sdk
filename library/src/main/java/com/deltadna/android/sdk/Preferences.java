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

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.util.Date;
import java.util.Map;

/**
 * Wrapper around simple key/value pairs which need to be persisted.
 */
final class Preferences {
    
    private static final String FILE = "com.deltadna.android.sdk";
    
    private static final String USER_ID = "user_id";
    private static final String FIRST_RUN = "first_run";
    private static final String FIRST_SESSION = "first_session";
    private static final String LAST_SESSION = "last_session";
    private static final String CROSS_GAME_USER_ID = "cross_game_user_id";
    private static final String REGISTRATION_ID = "registration_id";
    private static final String ADVERTISING_ID = "advertising_id";
    private static final String FORGET_ME = "forget_me";
    private static final String STOP_TRACKING_ME = "stop_tracking_me";
    private static final String FORGOTTEN = "forgotten";
    
    private final SharedPreferences prefs;
    
    Preferences(Context context) {
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        
        new Migrate(context).run();
    }
    
    @Nullable
    String getUserId() {
        return prefs.getString(USER_ID, null);
    }
    
    Preferences setUserId(String userId) {
        prefs.edit().putString(USER_ID, userId).apply();
        return this;
    }
    
    int getFirstRun() {
        return prefs.getInt(FIRST_RUN, 1);
    }
    
    Preferences setFirstRun(int firstRun) {
        prefs.edit().putInt(FIRST_RUN, firstRun).apply();
        return this;
    }
    
    @Nullable
    Date getFirstSession() {
        return prefs.contains(FIRST_SESSION)
                ? new Date(prefs.getLong(FIRST_SESSION, System.currentTimeMillis()))
                : null;
    }
    
    Preferences setFirstSession(Date date) {
        prefs.edit().putLong(FIRST_SESSION, date.getTime()).apply();
        return this;
    }
    
    @Nullable
    Date getLastSession() {
        return prefs.contains(LAST_SESSION)
                ? new Date(prefs.getLong(LAST_SESSION, System.currentTimeMillis()))
                : null;
    }
    
    Preferences setLastSession(Date date) {
        prefs.edit().putLong(LAST_SESSION, date.getTime()).apply();
        return this;
    }
    
    @Nullable
    String getCrossGameUserId() {
        return prefs.getString(CROSS_GAME_USER_ID, null);
    }
    
    Preferences setCrossGameUserId(@Nullable String crossGameUserId) {
        prefs.edit().putString(CROSS_GAME_USER_ID, crossGameUserId).apply();
        return this;
    }
    
    @Nullable
    String getRegistrationId() {
        return prefs.getString(REGISTRATION_ID, null);
    }
    
    Preferences setRegistrationId(@Nullable String registrationId) {
        prefs.edit().putString(
                REGISTRATION_ID,
                registrationId)
                .apply();
        return this;
    }
    
    @Nullable
    String getAdvertisingId() {
        return prefs.getString(ADVERTISING_ID, null);
    }
    
    Preferences setAdvertisingId(@Nullable String advertisingId) {
        prefs.edit().putString(ADVERTISING_ID, advertisingId).apply();
        return this;
    }
    
    boolean isForgetMe() {
        return prefs.getBoolean(FORGET_ME, false);
    }
    
    Preferences setForgetMe(boolean value) {
        prefs.edit().putBoolean(FORGET_ME, value).apply();
        return this;
    }

    boolean isStopTrackingMe() {
        return prefs.getBoolean(STOP_TRACKING_ME, false);
    }

    Preferences setStopTrackingMe(boolean value) {
        prefs.edit().putBoolean(STOP_TRACKING_ME, value).apply();
        return this;
    }
    
    boolean isForgotten() {
        return prefs.getBoolean(FORGOTTEN, false);
    }
    
    Preferences setForgotten(boolean value) {
        prefs.edit().putBoolean(FORGOTTEN, value).apply();
        return this;
    }
    
    Preferences clearUserAssociatedKeys() {
        prefs   .edit()
                .remove(FIRST_RUN)
                .remove(FIRST_SESSION)
                .remove(LAST_SESSION)
                .remove(CROSS_GAME_USER_ID)
                .apply();
        return this;
    }
    
    Preferences clearForgetMeAndForgotten() {
        prefs   .edit()
                .remove(FORGET_ME)
                .remove(FORGOTTEN)
                .remove(STOP_TRACKING_ME)
                .apply();
        return this;
    }
    
    Preferences clear() {
        prefs.edit().clear().apply();
        return this;
    }
    
    @Deprecated
    SharedPreferences getPrefs() {
        return prefs;
    }
    
    private class Migrate implements Runnable {
        
        private static final String FILE = "DELTADNA";
        
        private static final String KEY_USER_ID = "DDSDK_USER_ID";
        private static final String KEY_FIRST_RUN = "DDSDK_FIRST_RUN";
        private static final String KEY_REGISTRATION_ID = "DDSDK_ANDROID_REGISTRATION_ID";
        private static final String KEY_FORGET_ME = "DDSDK_FORGET_ME";
        private static final String KEY_FORGOTTEN = "DDSDK_FORGOTTEN";
        
        private final Context context;
        
        Migrate(Context context) {
            this.context = context;
        }
        
        @Override
        public void run() {
            final SharedPreferences old =
                    context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
            if (old.getAll().isEmpty()) return;
            
            final SharedPreferences.Editor editor = prefs.edit();
            for (final Map.Entry<String, ?> entry : old.getAll().entrySet()) {
                switch (entry.getKey()) {
                    case KEY_USER_ID:
                        editor.putString(USER_ID, (String) entry.getValue());
                        break;
                    
                    case KEY_FIRST_RUN:
                        editor.putInt(FIRST_RUN, (Integer) entry.getValue());
                        break;
                    
                    case KEY_REGISTRATION_ID:
                        editor.putString(REGISTRATION_ID, (String) entry.getValue());
                        break;
                    
                    case KEY_FORGET_ME:
                        editor.putBoolean(FORGET_ME, (Boolean) entry.getValue());
                        break;
                    
                    case KEY_FORGOTTEN:
                        editor.putBoolean(FORGOTTEN, (Boolean) entry.getValue());
                        break;
                }
            }
            editor.apply();
            
            old.edit().clear().apply();
        }
    }
}
