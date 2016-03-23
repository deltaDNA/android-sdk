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
import android.util.Log;

import com.deltadna.android.sdk.BuildConfig;
import com.deltadna.android.sdk.ImageMessageActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An {@link EngageListener} to be used for Image Message requests,
 * which will start the {@link ImageMessageActivity} to show the message.
 * 
 * @see ImageMessageActivity
 */
public abstract class ImageMessageListener implements EngageListener {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + ImageMessageListener.class.getSimpleName();
    
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
            activity.startActivityForResult(
                    ImageMessageActivity.createIntent(activity, result),
                    requestCode);
        } else {
            Log.d(TAG, "Image Message will not be shown");
            
            final JSONObject parameters;
            try {
                parameters = result.getJSONObject("parameters");
            } catch (JSONException e) {
                onFailure(e);
                return;
            }
            
            onNoImageToShow(parameters);
        }
    }
    
    /**
     * Invoked when an image will not be shown.
     *
     * @param parameters    the parameters from the result
     */
    public void onNoImageToShow(
            @SuppressWarnings("UnusedParameters")
            JSONObject parameters) {}
}
