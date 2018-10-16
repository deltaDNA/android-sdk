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

import org.json.JSONObject;

/**
 * Listener which can be used with
 * {@link com.deltadna.android.sdk.ImageMessageActivity#handleResult(int, android.content.Intent, ImageMessageResultListener)}
 * to handle the result of an Image Messaging request.
 */
public interface ImageMessageResultListener {
    
    /**
     * Will be invoked when the user selects an element set to use an action.
     *
     * @param value     the action value
     * @param params    the parameters for the action
     */
    default void onAction(String value, JSONObject params) {}
    
    /**
     * Will be invoked when the user selects an element set to use a link
     * action.
     *
     * @param value     the link value
     * @param params    the parameters for the action
     */
    default void onLink(String value, JSONObject params) {}
    
    /**
     * Will be invoked when the user selects an element set to use a store
     * action. The value appropriate for the current platform will be returned.
     *
     * @param value     the platform-specific store value
     * @param params    the parameters for the action
     */
    default void onStore(String value, JSONObject params) {}
    
    /**
     * Will be invoked when the user selects an element set to dismiss the
     * image message.
     */
    default void onCancelled() {}
}
