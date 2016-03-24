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

package com.deltadna.android.sdk.listeners;

import android.app.Activity;
import android.content.Intent;

import com.deltadna.android.sdk.DDNA;
import com.deltadna.android.sdk.ImageMessage;
import com.deltadna.android.sdk.ImageMessageActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An {@link EngageListener} to be used for Image Message requests, which will
 * prepare an {@link ImageMessage} for use to be shown by the
 * {@link ImageMessageActivity}.
 *
 * @see ImageMessageActivity
 */
public abstract class ImageMessageListener implements EngageListener {
    
    protected final Activity activity;
    protected final int requestCode;
    
    /**
     * Constructs a new listener.
     * 
     * @param activity      {@link Activity} from which the request is
     *                      being started
     * @param requestCode   the request code that will be used in
     *                      {@link Activity#onActivityResult(int, int, Intent)}
     *                      for the result
     */
    public ImageMessageListener(Activity activity, int requestCode) {
        this.activity = activity;
        this.requestCode = requestCode;
    }
    
    @Override
    public void onSuccess(JSONObject result) {
        if (result.has("image")) {
            final ImageMessage message;
            try {
                message = new ImageMessage(result);
            } catch (JSONException e) {
                onFailure(e);
                return;
            }
            
            message.prepare(
                    DDNA.instance().getNetworkManager(),
                    new ImageMessage.PrepareListener() {
                        @Override
                        public void onReady(ImageMessage src) {
                            onPrepared(src);
                        }
                        
                        @Override
                        public void onError(Throwable cause) {
                            onFailure(cause);
                        }
                    });
        } else {
            onFailure(new Exception("Image not found in response"));
        }
    }
    
    /**
     * Opens the {@link ImageMessageActivity} for showing {@code imageMessage}.
     *
     * @param imageMessage the image message to show
     */
    protected void show(ImageMessage imageMessage) {
        if (!imageMessage.prepared()) {
            onFailure(new Exception(imageMessage + " is not prepared"));
            return;
        }
        
        activity.startActivityForResult(
                ImageMessageActivity.createIntent(activity, imageMessage),
                requestCode);
    }
    
    /**
     * Invoked when the {@code imageMessage} has been prepared.
     * <p>
     * In most implementations {@link #show(ImageMessage)} should be called,
     * if the application is still in an appropriate state to do so.
     *
     * @param imageMessage the prepared image message
     */
    protected abstract void onPrepared(ImageMessage imageMessage);
}
