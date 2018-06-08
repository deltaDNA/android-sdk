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

package com.deltadna.android.sdk;

import android.support.annotation.Nullable;
import android.util.Log;

import com.deltadna.android.sdk.listeners.EngageListener;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helps with creating the different types of action available from the Engage
 * service. It makes the request to Engage and notifies on a callback when the
 * request completes.
 */
public class EngageFactory {
    
    protected final DDNA analytics;
    
    protected EngageFactory(DDNA analytics) {
        this.analytics = analytics;
    }
    
    /**
     * Requests game parameters at {@code decisionPoint}.
     *
     * @param decisionPoint the decision point
     * @param callback      the callback for completion notification
     *
     * @throws IllegalArgumentException if the {@code decisionPoint} is null or
     *                                  empty
     */
    public void requestGameParameters(
            String decisionPoint,
            Callback<JSONObject> callback) {
        
        requestGameParameters(decisionPoint, null, callback);
    }
    
    /**
     * Requests game parameters at {@code decisionPoint} with {@code parameters}.
     *
     * @param decisionPoint the decision point
     * @param parameters    the parameters for the request
     * @param callback      the callback for completion notification
     *
     * @throws IllegalArgumentException if the {@code decisionPoint} is null or
     *                                  empty
     */
    public void requestGameParameters(
            String decisionPoint,
            @Nullable Params parameters,
            final Callback<JSONObject> callback) {
        
        final Engagement engagement = build(decisionPoint, parameters);
        analytics.requestEngagement(
                engagement,
                new EngageListener<Engagement>() {
                    @Override
                    public void onCompleted(Engagement engagement) {
                        callback.onCompleted(
                                (engagement.getJson() != null && engagement.getJson().has("parameters"))
                                        ? engagement.getJson().optJSONObject("parameters")
                                        : new JSONObject());
                    }
                    
                    @Override
                    public void onError(Throwable t) {
                        callback.onCompleted(new JSONObject());
                    }
                });
    }
    
    /**
     * Requests an {@link ImageMessage} at {@code decisionPoint}.
     * <p>
     * The image message needs to be prepared before being shown with
     * {@link ImageMessage#prepare(ImageMessage.PrepareListener)}.
     *
     * @param decisionPoint the decision point
     * @param callback      the callback for completion notification
     *
     * @throws IllegalArgumentException if the {@code decisionPoint} is null or
     *                                  empty
     */
    public void requestImageMessage(
            String decisionPoint,
            Callback<ImageMessage> callback) {
        
        requestImageMessage(decisionPoint, null, callback);
    }
    
    /**
     * Requests an {@link ImageMessage} at {@code decisionPoint} with
     * {@code parameters}.
     * <p>
     * The image message needs to be prepared before being shown with
     * {@link ImageMessage#prepare(ImageMessage.PrepareListener)}.
     *
     * @param decisionPoint the decision point
     * @param parameters    the parameters for the request
     * @param callback      the callback for completion notification
     *
     * @throws IllegalArgumentException if the {@code decisionPoint} is null or
     *                                  empty
     */
    public void requestImageMessage(
            String decisionPoint,
            @Nullable Params parameters,
            final Callback<ImageMessage> callback) {
        
        final Engagement engagement = build(decisionPoint, parameters);
        analytics.requestEngagement(
                engagement,
                new EngageListener<Engagement>() {
                    @Override
                    public void onCompleted(Engagement engagement) {
                        callback.onCompleted(ImageMessage.create(engagement));
                    }
                    
                    @Override
                    public void onError(Throwable t) {
                        callback.onCompleted(null);
                    }
                });
    }
    
    protected static Engagement build(
            String decisionPoint,
            @Nullable Params parameters) {
        
        if (parameters != null) {
            Params parametersCopy;
            try {
                parametersCopy = new Params(parameters);
            } catch (JSONException e) {
                Log.w(BuildConfig.LOG_TAG, "Failed to copy parameters", e);
                parametersCopy = new Params();
            }
            
            return new Engagement(decisionPoint, parametersCopy);
        } else {
            return new Engagement(decisionPoint);
        }
    }
    
    public interface Callback<T> {
        
        /**
         * Invoked when the request has completed with {@code action} being
         * returned, or {@code null} if no action has been returned or the
         * request has failed.
         *
         * @param action the returned action, or {@code null}
         */
        void onCompleted(@Nullable T action);
    }
}
