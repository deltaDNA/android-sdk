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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.deltadna.android.sdk.DDNA;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * {@link FirebaseInstanceIdService} which gets notified when the token has
 * been updated.
 */
public final class InstanceIdListenerService extends FirebaseInstanceIdService {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + InstanceIdListenerService.class.getSimpleName();
    
    private LocalBroadcastManager broadcasts;

    @Override
    public void onCreate() {
        super.onCreate();
        
        broadcasts = LocalBroadcastManager.getInstance(this);
    }
    
    @Override
    public void onTokenRefresh() {
        final String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Registration token has been refreshed: " + token);
        
        notifySuccess(token);
    }
    
    private void notifySuccess(String token) {
        if (UnityForwarder.isPresent()) {
            UnityForwarder.getInstance().forward(
                    "DeltaDNA.AndroidNotifications",
                    "DidRegisterForPushNotifications",
                    token);
        } else {
            DDNA.instance().setRegistrationId(token);
            broadcasts.sendBroadcast(new Intent(
                    DDNANotifications.ACTION_TOKEN_RETRIEVAL_SUCCESSFUL)
                    .putExtra(DDNANotifications.EXTRA_REGISTRATION_TOKEN, token));
        }
    }
}
