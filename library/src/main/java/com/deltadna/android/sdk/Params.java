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

package com.deltadna.android.sdk;

import android.util.Log;

import com.deltadna.android.sdk.helpers.Preconditions;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Constructs parameters which can be used with events.
 */
public class Params implements JsonParams {
    
    final JSONObject json;
    
    /**
     * Creates a new instance.
     */
    public Params() {
        this(new JSONObject());
    }
    
    /**
     * Copy constructor creating a new instance.
     *
     * @param params    the params to copy from
     *
     * @throws JSONException if {@code params} is not valid JSON
     */
    public Params(Params params) throws JSONException {
        this(new JSONObject(params.json.toString()));
    }
    
    public Params(JSONObject json) {
        this.json = json;
    }
    
    @Override
    public JSONObject toJson() {
        return json;
    }
    
    /**
     * Puts nested parameters under the key.
     *
     * @param key   the key
     * @param value the parameters
     *
     * @return this {@link Params} instance
     *
     * @throws IllegalArgumentException if the {@code key} is null or empty
     */
    public Params put(String key, JsonParams value) {
        return put(key, (value != null) ? value.toJson() : null);
    }
    
    /**
     * Puts the key/value pair into these parameters.
     *
     * @param key   the key
     * @param value the value
     *
     * @return this {@link Params} instance
     *
     * @throws IllegalArgumentException if the {@code key} is null or empty
     */
    public Params put(String key, Object value) {
        Preconditions.checkString(key, "key cannot be null or empty");
        if (value == null) {
            Log.w(BuildConfig.LOG_TAG, "null value for key: " + key);
        }
        
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
        
        return this;
    }
    
    boolean isEmpty() {
        return (json.length() == 0);
    }
}
