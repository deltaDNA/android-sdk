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

import android.support.annotation.Nullable;

/**
 * Constructs an engagement {@link Event}.
 */
public class Engagement<T extends Engagement<T>> extends Event<T> {
    
    @Nullable
    final String flavour;
    
    /**
     * Creates a new instance.
     *
     * @param decisionPoint the decision point
     */
    public Engagement(String decisionPoint) {
        this(decisionPoint, null);
    }
    
    /**
     * Creates a new instance, with a flavour.
     *
     * @param decisionPoint the decision point
     * @param flavour       the flavour, may be {@code null}
     */
    public Engagement(String decisionPoint, @Nullable String flavour) {
        this(decisionPoint, flavour, new Params());
    }
    
    public Engagement(
            String decisionPoint,
            @Nullable String flavour,
            Params params) {
        
        super(decisionPoint, params);
        
        this.flavour = flavour;
    }
    
    @Override
    public T putParam(String key, JsonParams value) {
        return super.putParam(key, value);
    }
    
    @Override
    public T putParam(String key, Object value) {
        return super.putParam(key, value);
    }
}
