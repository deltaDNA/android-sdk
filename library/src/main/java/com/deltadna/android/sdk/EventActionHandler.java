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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handlers which can be registered on {@link EventAction}s for handling actions
 * of different types.
 * <p>
 * {@link GameParametersHandler} and {@link ImageMessageHandler} should be used.
 *
 * @param <T> the type of the action to handle
 */
public abstract class EventActionHandler<T> {
    
    protected final Callback<T> callback;
    
    private EventActionHandler(Callback<T> callback) {
        this.callback = callback;
    }
    
    abstract boolean handle(EventTrigger trigger, ActionStore store);
    
    abstract String getType();
    
    /**
     * {@link EventActionHandler} for handling game parameters, which will be
     * returned as a {@link JSONObject}.
     */
    public static class GameParametersHandler extends EventActionHandler<JSONObject> {
        
        public GameParametersHandler(Callback<JSONObject> callback) {
            super(callback);
        }
        
        @Override
        final boolean handle(EventTrigger trigger, ActionStore store) {
            if (trigger.getAction().equals(getType())) {
                final JSONObject response = trigger.getResponse();
                final JSONObject persistedParams = store.get(trigger);
                
                if (persistedParams != null) {
                    store.remove(trigger);
                    callback.handle(persistedParams);
                } else if (response.has("parameters")) {
                    callback.handle(response.optJSONObject("parameters"));
                } else {
                    callback.handle(new JSONObject());
                }
                
                return true;
            }
            
            return false;
        }
        
        @Override
        final String getType() {
            return "gameParameters";
        }
    }
    
    /**
     * {@link EventActionHandler} for handling {@link ImageMessage}s.
     */
    public static class ImageMessageHandler extends EventActionHandler<ImageMessage> {
        
        public ImageMessageHandler(Callback<ImageMessage> callback) {
            super(callback);
        }
        
        @Override
        final boolean handle(EventTrigger trigger, ActionStore store) {
            if (trigger.getAction().equals(getType())) {
                JSONObject response = trigger.getResponse();
                final JSONObject persistedParams = store.get(trigger);
                
                try {
                    if (persistedParams != null) {
                        // copy the json to avoid modifying original
                        response = new JSONObject(response.toString());
                        response.put("parameters", persistedParams);
                    }
                    
                    final ImageMessage imageMessage = new ImageMessage(response);
                    if (imageMessage.prepared()) {
                        if (persistedParams != null) {
                            store.remove(trigger);
                        }
                        
                        callback.handle(imageMessage);
                        
                        return true;
                    }
                } catch (JSONException ignored) {}
            }
            
            return false;
        }
        
        @Override
        final String getType() {
            return "imageMessage";
        }
    }
    
    public interface Callback<T> {
        
        void handle(T action);
    }
}
