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

package com.deltadna.android.sdk.notifications.example;

import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import android.widget.TextView;

import com.deltadna.android.sdk.DDNA;
import com.deltadna.android.sdk.notifications.DDNANotifications;

public class ExampleActivity extends AppCompatActivity {
    
    private LocalBroadcastManager broadcasts;
    private BroadcastReceiver receiver;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_example);
        
        DDNA.instance().startSdk();
        
        ((TextView) findViewById(R.id.user_id)).setText(getString(
                R.string.user_id,
                DDNA.instance().getUserId()));
        
        broadcasts = LocalBroadcastManager.getInstance(this);
        
        /*
         * Create the receiver and register it so that we can get notifications
         * when the token changes.
         */
        receiver = new ExampleReceiver(
                (TextView) findViewById(R.id.registration_token));
        broadcasts.registerReceiver(
                receiver,
                DDNANotifications.FILTER_TOKEN_RETRIEVAL);
    }
    
    @Override
    public void onDestroy() {
        /**
         * This activity will no longer be doing anything useful with the
         * token, however you may want to have a receiver that stays registered
         * for the entire lifecycle of the game.
         */
        broadcasts.unregisterReceiver(receiver);
        
        DDNA.instance().stopSdk();
        
        super.onDestroy();
    }
    
    public void onUploadEvents(View view) {
        DDNA.instance().upload();
    }
    
    /**
     * Register for push notifications.
     * <p>
     * If you would like to handle the retrieval of the GCM registration token
     * manually then you can call {@link DDNA#setRegistrationId(String)}
     * instead.
     */
    public void onRegister(View view) {
        DDNANotifications.register(this);
    }
    
    /**
     * Unregister from push notifications.
     * <p>
     * If you would like to handle the retrieval of the GCM registration token
     * manually then you can call {@link DDNA#clearRegistrationId()} instead.
     */
    public void onUnregister(View view) {
        DDNANotifications.unregister();
    }
    
    public void onNotificationOpened(View view) {
        // pretend the user opened a push notification
        DDNA.instance().recordNotificationOpened();
    }
    
    public void onNotificationDismissed(View view) {
        // pretend the user dismissed a push notification
        DDNA.instance().recordNotificationDismissed();
    }
    
    public void onStopSdk(View view) {
        DDNA.instance().stopSdk();
    }
    
    public void onStartSdk(View view) {
        DDNA.instance().startSdk();
    }
}
