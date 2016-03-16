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

import com.deltadna.android.sdk.helpers.Objects;
import com.deltadna.android.sdk.helpers.Preconditions;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;

/**
 * HTTP message body for a {@link Request}.
 */
final class RequestBody {
    
    private static final Charset UTF8 = Charset.forName("UTF-8");
    
    final String type;
    final byte[] content;
    
    RequestBody(String type, byte[] content) {
        Preconditions.checkArg(
                !type.isEmpty(),
                "type cannot be null or empty");
        Preconditions.checkArg(
                content != null && content.length > 0,
                "content cannot be null or empty");
        
        this.type = type;
        this.content = content;
    }
    
    void fill(HttpURLConnection connection) throws IOException {
        connection.setFixedLengthStreamingMode(content.length);
        connection.setRequestProperty("Content-Type", type);
        
        OutputStream output = null;
        try {
            output = connection.getOutputStream();
            output.write(content);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
    
    @Override
    public String toString() {
        return new Objects.ToStringHelper(this)
                .add("type", type)
                .add("content", new String(content))
                .toString();
    }
    
    static RequestBody json(JSONObject content) {
        return new RequestBody(
                "application/json; charset=utf-8",
                content.toString().getBytes(UTF8));
    }
}
