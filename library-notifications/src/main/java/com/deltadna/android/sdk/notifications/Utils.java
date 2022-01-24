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

package com.deltadna.android.sdk.notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

class Utils {
    
    static Bundle convert(Map<String, String> payload) {
        final Bundle result = new Bundle(payload.size());
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            result.putString(entry.getKey(), entry.getValue());
        }
        
        return result;
    }
    
    static String convert(Bundle payload) {
        final JSONObject result = new JSONObject();
        
        for (final String key : payload.keySet()) {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    result.put(key, payload.get(key));
                } else {
                    result.put(key, JSONObject.wrap(payload.get(key)));
                }
            } catch (JSONException e) {
                Log.w(BuildConfig.LOG_TAG, e);
            }
        }
        
        return result.toString();
    }
    
    static Intent wrapWithReceiver(Context context, Intent intent) {
        synchronized (DDNANotifications.class) {
            if (DDNANotifications.receiver != null) {
                intent.setClass(context, DDNANotifications.receiver);
            }
        }
        
        return intent;
    }
    
    private Utils() {}
}
