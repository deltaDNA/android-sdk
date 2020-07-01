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

import androidx.annotation.Nullable;
import android.util.Log;

import org.json.JSONObject;

import java.util.Date;
import java.util.Locale;

class ActionStore {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + ActionStore.class.getSimpleName();
    
    private final DatabaseHelper database;
    
    ActionStore(DatabaseHelper database) {
        this.database = database;
    }
    
    @Nullable
    JSONObject get(EventTrigger trigger) {
        return database.getAction(trigger.getCampaignId());
    }
    
    void put(EventTrigger trigger, JSONObject action) {
        Log.v(TAG, String.format(
                Locale.ENGLISH,
                "Adding %s for %s",
                trigger,
                action));
        database.insertActionRow(
                trigger.getEventName(),
                trigger.getCampaignId(),
                new Date(),
                action);
    }
    
    void remove(EventTrigger trigger) {
        Log.v(TAG, "Removing action for " + trigger);
        database.removeActionRow(trigger.getCampaignId());
    }
    
    void clear() {
        Log.v(TAG, "Clearing actions");
        database.removeActionRows();
    }
}
