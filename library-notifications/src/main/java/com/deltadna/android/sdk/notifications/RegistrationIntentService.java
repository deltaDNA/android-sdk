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

import android.app.IntentService;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.deltadna.android.sdk.DDNA;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/**
 * {@link IntentService} which handles the retrieval of a registration token
 * from GCM and setting it on the SDK.
 * <p>
 * The following entry will need to be added inside the {@code application} tag
 * of your manifest file:
 * <pre><code>
 * {@literal<}meta-data
 *     android:name="ddna_sender_id"
 *     android:value="@string/{@literal<}your-sender-id-resource{@literal>}"/{@literal>}
 * </code></pre>
 */
public final class RegistrationIntentService extends IntentService {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + RegistrationIntentService.class.getSimpleName();
    
    private Bundle metaData;
    private LocalBroadcastManager broadcasts;
    
    public RegistrationIntentService() {
        super(RegistrationIntentService.class.getSimpleName());
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        metaData = MetaData.get(this);
        broadcasts = LocalBroadcastManager.getInstance(this);
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Retrieving registration token");
        
        final String senderId;
        try {
            senderId = getString(metaData.getInt(MetaData.SENDER_ID));
        } catch (Resources.NotFoundException e) {
            throw new RuntimeException(String.format(
                    Locale.US,
                    "Failed to find %s, has it been defined in the manifest?",
                    MetaData.SENDER_ID),
                    e);
        }
        
        final String token;
        try {
            token = InstanceID.getInstance(this).getToken(
                    senderId,
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE);
            
            Log.d(TAG, "Retrieved registration token " + token);
            
            notifySuccess(token);
        } catch (IOException e) {
            Log.w(TAG, "Failed to retrieve registration token", e);
            notifyFailure(e);
        }
    }
    
    private void notifySuccess(String token) {
        if (UnityClasses.PLAYER != null) {
            notifyUnitySuccess(UnityClasses.PLAYER, token);
        } else {
            notifyAndroid(token);
        }
    }
    
    private void notifyFailure(Throwable t) {
        if (UnityClasses.PLAYER != null) {
            notifyUnityFailure(UnityClasses.PLAYER, t.getMessage());
        } else {
            broadcasts.sendBroadcast(new Intent(
                    DDNANotifications.ACTION_TOKEN_RETRIEVAL_FAILED)
                    .putExtra(DDNANotifications.EXTRA_FAILURE_REASON, t));
        }
    }
    
    private void notifyAndroid(String token) {
        DDNA.instance().setRegistrationId(token);
        broadcasts.sendBroadcast(new Intent(
                DDNANotifications.ACTION_TOKEN_RETRIEVAL_SUCCESSFUL)
                .putExtra(DDNANotifications.EXTRA_REGISTRATION_TOKEN, token));
    }
    
    private void notifyUnitySuccess(Class<?> player, String token) {
        sendUnityMessage(
                player,
                "DeltaDNA.AndroidNotifications",
                "DidRegisterForPushNotifications",
                token);
    }
    
    private void notifyUnityFailure(Class<?> player, String msg) {
        sendUnityMessage(
                player,
                "DeltaDNA.AndroidNotifications",
                "DidFailToRegisterForPushNotifications",
                msg);
    }
    
    private void sendUnityMessage(
            Class<?> receiver,
            String gameObject,
            String methodName,
            String message) {
        
        try {
            receiver.getDeclaredMethod(
                    "UnitySendMessage",
                    String.class,
                    String.class,
                    String.class)
                    .invoke(receiver,
                            gameObject,
                            methodName,
                            message);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Failed sending message to Unity", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Failed sending message to Unity", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Failed sending message to Unity", e);
        }
    }
}
