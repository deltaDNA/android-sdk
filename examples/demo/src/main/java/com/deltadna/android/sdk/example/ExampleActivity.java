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

package com.deltadna.android.sdk.example;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.deltadna.android.sdk.DDNA;
import com.deltadna.android.sdk.Engagement;
import com.deltadna.android.sdk.Event;
import com.deltadna.android.sdk.ImageMessageActivity;
import com.deltadna.android.sdk.ImageMessage;
import com.deltadna.android.sdk.Product;
import com.deltadna.android.sdk.Transaction;
import com.deltadna.android.sdk.listeners.EngageListener;
import com.deltadna.android.sdk.listeners.ImageMessageResultListener;

public class ExampleActivity extends AppCompatActivity {
    
    private static final int REQUEST_CODE_IMAGE_MSG = 1;
    
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
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // handle the request code which we used for the Image Message engagement
        if (requestCode == REQUEST_CODE_IMAGE_MSG) {
            ImageMessageActivity.handleResult(
                    resultCode,
                    data,
                    new ImageMessageResultListener() {
                        @Override
                        public void onAction(String value, String params) {
                            showImageMessageDialog(getString(
                                    R.string.image_message_action,
                                    value,
                                    params));
                        }
                        
                        @Override
                        public void onCancelled() {
                            showImageMessageDialog(getString(
                                    R.string.image_message_cancelled));
                        }
                    });
        }
    }
    
    public void onUploadEvents(View view) {
        DDNA.instance().upload();
    }
    
    public void onSimpleEvent(View view) {
        DDNA.instance().recordEvent("simpleEvent");
    }
    
    public void onBasicEvent(View view) {
        DDNA.instance().recordEvent(new Event("basicEvent")
                .putParam("clientVersion", "1.03.00")
                .putParam("serverVersion", "1.02.00")
                .putParam("dataVersion", "1.02.05"));
    }
    
    public void onComplexEvent(View view) {
        DDNA.instance().recordEvent(new Transaction(
                "IAP - Large Treasure Chest",
                "PURCHASE",
                new Product()
                        .addItem("Golden Battle Axe", "Weapon", 1)
                        .addItem("Mighty Flaming Sword of the First Age", "Legendary Weapon", 1)
                        .addItem("Jewel Encrusted Shield", "Armour", 1)
                        .addVirtualCurrency("Gold", "PREMIUM", 100),
                new Product().setRealCurrency(
                        "USD",
                        Product.convertCurrency(
                                DDNA.instance(),
                                "USD",
                                4.99f))) // $4.99
                .setId("47891208312996456524019-178.149.115.237:51787")
                .setProductId("4019")
                .setTransactorId("62.212.91.84:15116")
                .setServer("GOOGLE")
                .setReceipt("ewok9Ja81............991KS=="));
    }
    
    public void onEngage(View view) {
        DDNA.instance().requestEngagement(
                "testEngage",
                new EngageListenerExample());
    }
    
    public void onImageMessage(View view) {
        DDNA.instance().requestEngagement(
                new Engagement("testImageMessage"),
                new ImageMessageListener());
    }
    
    public void onNotificationOpened(View view) {
        /*
         * Pretend the user opened a push notification, triggered with an
         * empty payload.
         */
        DDNA.instance().recordNotificationOpened(false, new Bundle());
    }
    
    public void onStartSdk(View view) {
        DDNA.instance().startSdk();
    }
    
    public void onStopSdk(View view) {
        DDNA.instance().stopSdk();
    }
    
    private void showImageMessageDialog(String msg) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.image_message)
                .setMessage(msg)
                .setPositiveButton(R.string.dismiss, null)
                .show();
    }
    
    /**
     * Example implementation of the {@link EngageListener} which just
     * prints the result, or failure, to the log.
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
    
    /**
     * Example implementation of the {@link EngageListener} for an Image
     * Messaging request, which prepares the {@link ImageMessage}, and finally
     * shows it to the user.
     *
     * @see #onActivityResult(int, int, Intent)
     */
    private class ImageMessageListener implements EngageListener<Engagement> {
        
        @Override
        public void onCompleted(Engagement engagement) {
            ImageMessage imageMessage = ImageMessage.create(engagement);
            if (imageMessage != null) {
                imageMessage.prepare(new ImageMessage.PrepareListener() {
                    @Override
                    public void onPrepared(ImageMessage src) {
                        src.show(ExampleActivity.this, REQUEST_CODE_IMAGE_MSG);
                    }
                    
                    @Override
                    public void onError(Throwable cause) {
                        Log.w(  BuildConfig.LOG_TAG,
                                "Image Message preparation error",
                                cause);
                    }
                });
            }
        }
        
        @Override
        public void onError(Throwable t) {
            Log.w(BuildConfig.LOG_TAG, "Image Message error", t);
        }
    }
}
