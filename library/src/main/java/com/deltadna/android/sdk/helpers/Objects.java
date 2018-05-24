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

package com.deltadna.android.sdk.helpers;

import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Helper functions that can operate on any {@link Object}.
 */
public final class Objects {
    
    public static boolean equals(Object a, Object b) {
        if (a != null && b != null) {
            return a.equals(b);
        } else {
            return (a == null && b == null);
        }
    }
    
    @Nullable
    public static JSONObject extract(@Nullable JSONObject from, String... path) {
        if (from == null) return null;
        
        JSONObject object = from;
        for (final String segment : path) {
            if (object == null) return null;
            
            object = object.optJSONObject(segment);
        }
        
        return object;
    }
    
    public static JSONArray extractArray(@Nullable JSONObject from, String... path) {
        final JSONObject parent = extract(
                from,
                Arrays.copyOfRange(path, 0, path.length - 1));
        
        return (parent != null)
                ? parent.optJSONArray(path[path.length - 1])
                : null;
    }
    
    public static final class ToStringHelper {
        
        private final StringBuilder builder;
        
        public ToStringHelper(Object instance) {
            builder = new StringBuilder();
            
            builder.append(instance.getClass().getSimpleName());
            builder.append('@');
            builder.append(instance.hashCode());
            builder.append('{');
        }
        
        public ToStringHelper add(String field, Object value) {
            if (builder.charAt(builder.length() - 1) != '{') {
                builder.append(", ");
            }
            
            builder.append(field);
            builder.append(": ");
            builder.append(value);
            return this;
        }
        
        @Override
        public String toString() {
            return new StringBuilder(builder).append('}').toString();
        }
    }
    
    private Objects() {}
}
