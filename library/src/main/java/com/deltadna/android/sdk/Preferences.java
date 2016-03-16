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

/**
 * Wrapper around simple key/value pairs which need to be persisted.
 */
final class Preferences {
    
    private static final String FILE = "DELTADNA";
    
    private static final String KEY_USER_ID = "DDSDK_USER_ID";
    private static final String KEY_FIRST_RUN = "DDSDK_FIRST_RUN";
    private static final String KEY_REGISTRATION_ID =
            "DDSDK_ANDROID_REGISTRATION_ID";
    
    private final SharedPreferences prefs;
    
    Preferences(Context context) {
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
    
    @Nullable
    String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }
    
    Preferences setUserId(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
        return this;
    }
    
    int getFirstRun() {
        return prefs.getInt(KEY_FIRST_RUN, 1);
    }
    
    Preferences setFirstRun(int firstRun) {
        prefs.edit().putInt(KEY_FIRST_RUN, firstRun).apply();
        return this;
    }
    
    Preferences clearFirstRun() {
        prefs.edit().remove(KEY_FIRST_RUN).apply();
        return this;
    }
    
    @Nullable
    String getRegistrationId() {
        return prefs.getString(KEY_REGISTRATION_ID, null);
    }
    
    Preferences setRegistrationId(@Nullable String registrationId) {
        prefs.edit().putString(
                KEY_REGISTRATION_ID,
                registrationId)
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
}
