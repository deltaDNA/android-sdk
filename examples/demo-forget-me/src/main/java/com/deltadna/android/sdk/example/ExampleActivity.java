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

package com.deltadna.android.sdk.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.deltadna.android.sdk.DDNA;
import com.deltadna.android.sdk.Engagement;
import com.deltadna.android.sdk.listeners.EngageListener;

import java.util.UUID;

public class ExampleActivity extends AppCompatActivity {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_example);
        
        /*
         * In this case the SDK will generate its own user id, but if you're
         * keeping your own user id then you may pass this into the overloaded
         * startSdk(String) method, or even provide it during initialisation.
         */
        DDNA.instance().startSdk();
        
        ((TextView) findViewById(R.id.user_id)).setText(getString(
                R.string.user_id,
                DDNA.instance().getUserId()));
    }
    
    @Override
    public void onDestroy() {
        DDNA.instance().stopSdk();
        
        super.onDestroy();
    }
    
    public void onForgetMe(View view) {
        DDNA.instance().forgetMe();
    }
    
    public void onClearPersistentData(View view) {
        DDNA.instance().clearPersistentData();
    }
    
    public void onUploadEvents(View view) {
        DDNA.instance().upload();
    }
    
    public void onSimpleEvent(View view) {
        DDNA.instance().recordEvent("simpleEvent");
    }
    
    public void onEngage(View view) {
        DDNA.instance().requestEngagement(
                "testEngage",
                new EngageListenerExample());
    }
    
    public void onImageMessage(View view) {
        DDNA.instance().getEngageFactory().requestImageMessage(
                "testImageMessage",
                action -> {
                    // prepare the action if not null
                });
    }
    
    public void onStartSdk(View view) {
        DDNA.instance().startSdk();
    }
    
    public void onStartSdkUserId(View view) {
        DDNA.instance().startSdk(UUID.randomUUID().toString());
    }
    
    public void onStopSdk(View view) {
        DDNA.instance().stopSdk();
    }
    
    /**
     * Example implementation of the {@link EngageListener} which prints the
     * result, or failure, to the log.
     */
    private class EngageListenerExample implements EngageListener<Engagement> {
        
        @Override
        public void onCompleted(Engagement engagement) {
            Log.d(BuildConfig.LOG_TAG, "Engagement success: " + engagement);
        }
        
        @Override
        public void onError(Throwable t) {
            Log.w(BuildConfig.LOG_TAG, "Engagement error", t);
        }
    }
}
