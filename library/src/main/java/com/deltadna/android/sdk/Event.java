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

import com.deltadna.android.sdk.helpers.Preconditions;

/**
 * Constructs an event.
 */
public class Event<T extends Event<T>> {
    
    protected final String name;
    protected final Params params;
    
    /**
     * Creates a new instance.
     *
     * @param name the event name
     */
    public Event(String name) {
        this(name, new Params());
    }
    
    public Event(String name, Params params) {
        Preconditions.checkString(name, "name cannot be null or empty");
        Preconditions.checkArg(params != null, "params cannot be empty");
        
        this.name = name;
        this.params = params;
    }
    
    /**
     * Puts the key/value pair into the event parameters.
     *
     * @param key   the key
     * @param value the value
     *
     * @return this {@link T} instance
     */
    public T putParam(String key, Object value) {
        params.put(key, value);
        return (T) this;
    }
    
    /**
     * Puts nested parameters under the key.
     *
     * @param key   the key
     * @param value the value
     *
     * @return this {@link T} instance
     */
    public T putParam(String key, JsonParams value) {
        params.put(key, value);
        return (T) this;
    }
}
