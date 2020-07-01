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
import android.content.pm.PackageManager;
import android.os.Bundle;

class MetaData {
    
    static Bundle values;
    
    static final String APPLICATION_ID = "ddna_application_id";
    static final String SENDER_ID = "ddna_sender_id";
    static final String PROJECT_ID = "ddna_fcm_project_id";
    static final String FCM_API_KEY = "ddna_fcm_api_key";

    static final String NOTIFICATION_ICON = "ddna_notification_icon";
    static final String NOTIFICATION_TITLE = "ddna_notification_title";
    
    @Deprecated
    static final String START_LAUNCH_INTENT = "ddna_start_launch_intent";
    
    static synchronized Bundle get(Context context) {
        if (values == null) {
            try {
                values = context
                        .getPackageManager()
                        .getApplicationInfo(
                                context.getPackageName(),
                                PackageManager.GET_META_DATA)
                        .metaData;
                
                if (values == null) {
                    values = Bundle.EMPTY;
                }
            } catch (PackageManager.NameNotFoundException e) {
                // will never happen
                throw new RuntimeException(e);
            }
        }
        
        return values;
    }
    
    private MetaData() {}
}
