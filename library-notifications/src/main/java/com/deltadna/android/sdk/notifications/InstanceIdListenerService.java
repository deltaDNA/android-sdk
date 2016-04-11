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

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * {@link InstanceIDListenerService} which gets notified when the
 * {@link com.google.android.gms.iid.InstanceID} has been refreshed, resulting
 * in a need to refresh the GCM registration token.
 */
public final class InstanceIdListenerService
        extends InstanceIDListenerService {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + InstanceIdListenerService.class.getSimpleName();
    
    @Override
    public void onTokenRefresh() {
        Log.d(TAG, "InstanceID token has been updated");
        
        startService(new Intent(this, RegistrationIntentService.class));
    }
}
