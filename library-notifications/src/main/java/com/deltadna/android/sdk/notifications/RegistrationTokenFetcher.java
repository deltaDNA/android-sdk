/*
 * Copyright (c) 2017 deltaDNA Ltd. All rights reserved.
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
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.deltadna.android.sdk.DDNA;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;
import java.util.Locale;

final class RegistrationTokenFetcher {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + RegistrationTokenFetcher.class.getSimpleName();
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    
    static void fetch(final Context context) {
        Log.d(TAG, "Fetching registration token");
        
        final String senderId;
        try {
            senderId = context.getString(
                    MetaData.get(context).getInt(MetaData.SENDER_ID));
        } catch (final Resources.NotFoundException e) {
            Log.w(  TAG,
                    String.format(
                            Locale.US,
                            "Failed to find %s, has it been defined in the manifest?",
                            MetaData.SENDER_ID),
                    e);
            return;
        }
        
        final String token;
        try {
            token = FirebaseInstanceId.getInstance().getToken(
                    senderId,
                    FirebaseMessaging.INSTANCE_ID_SCOPE);
        } catch (final IOException e) {
            Log.w(TAG, "Failed to fetch registration token", e);
            
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    notifyFailure(context, e);
                }
            });
            
            return;
        }
        
        Log.d(TAG, "Registration token has been retrieved: " + token);
        
        HANDLER.post(new Runnable() {
            @Override
            public void run() {
                notifySuccess(context, token);
            }
        });
    }
    
    private static void notifySuccess(Context context, String token) {
        if (UnityForwarder.isPresent()) {
            UnityForwarder.getInstance().forward(
                    "DeltaDNA.AndroidNotifications",
                    "DidRegisterForPushNotifications",
                    token);
        } else {
            DDNA.instance().setRegistrationId(token);
            
            context.sendBroadcast(Utils.wrapWithReceiver(
                    context,
                    new Intent(Actions.REGISTERED)
                            .setPackage(context.getPackageName())
                            .putExtra(Actions.REGISTRATION_TOKEN, token)));
        }
    }
    
    private static void notifyFailure(Context context, Throwable t) {
        if (UnityForwarder.isPresent()) {
            UnityForwarder.getInstance().forward(
                    "DeltaDNA.AndroidNotifications",
                    "DidFailToRegisterForPushNotifications",
                    t.getMessage());
        } else {
            context.sendBroadcast(Utils.wrapWithReceiver(
                    context,
                    new Intent(Actions.REGISTRATION_FAILED)
                            .setPackage(context.getPackageName())
                            .putExtra(Actions.REGISTRATION_FAILURE_REASON, t)));
        }
    }
    
    private RegistrationTokenFetcher() {}
}
