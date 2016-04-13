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

package com.deltadna.android.sdk.net;

import org.json.JSONObject;

import java.nio.charset.Charset;

/**
 * For converting a {@link Response} body in bytes to a more representative
 * form.
 * 
 * @param <T> type to convert the {@link Response} bytes to
 */
interface ResponseBodyConverter<T> {
    
    ResponseBodyConverter<Void> NULL = new ResponseBodyConverter<Void>() {
        @Override
        public Void convert(byte[] input) {
            return null;
        }
    };
    ResponseBodyConverter<String> STRING = new ResponseBodyConverter<String>() {
        @Override
        public String convert(byte[] input) {
            return new String(input, Charset.forName("UTF-8"));
        }
    };
    ResponseBodyConverter<JSONObject> JSON = new ResponseBodyConverter<JSONObject>() {
        @Override
        public JSONObject convert(byte[] input) throws Exception {
            return new JSONObject(ResponseBodyConverter.STRING.convert(input));
        }
    };
    
    T convert(byte[] input) throws Exception;
}
