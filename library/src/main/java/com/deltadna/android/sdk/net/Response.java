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

import android.support.annotation.Nullable;

import com.deltadna.android.sdk.helpers.Objects;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;

/**
 * HTTP response, including the code and the body. The body is in
 * byte form and {@link V} if a {@link ResponseBodyConverter} has
 * been provided.
 * 
 * @param <V> type of the converted response body
 */
public final class Response<V> {
    
    public final int code;
    public final byte[] bytes;
    public final V body;
    
    public Response(int code, byte[] bytes, V body) {
        this.code = code;
        this.bytes = bytes;
        this.body = body;
    }
    
    public boolean isSuccessful() {
        return isSuccess(code);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Response)) {
            return false;
        } else if (this == o) {
            return true;
        }
        
        final Response other = (Response) o;
        
        return (code == other.code
                && Arrays.equals(bytes, other.bytes)
                && Objects.equals(body, other.body));
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] {
                code,
                Arrays.hashCode(bytes),
                body});
    }
    
    @Override
    public String toString() {
        return new Objects.ToStringHelper(this)
                .add("code", code)
                .add("body", body)
                .toString();
    }
    
    static <V> Response<V> create(
            int code,
            int contentLength,
            InputStream stream,
            @Nullable ResponseBodyConverter<V> converter) throws Exception {
        
        final ByteArrayOutputStream buffer;
        try {
            buffer = (contentLength != -1)
                    ? new ByteArrayOutputStream(contentLength)
                    : new ByteArrayOutputStream();
            int read;
            while ((read = stream.read()) != -1) {
                buffer.write(read);
            }
        } finally {
            stream.close();
        }
        
        final byte[] bytes = buffer.toByteArray();
        return new Response<V>(
                code,
                bytes,
                (converter != null)
                        ? converter.convert(bytes)
                        : null);
    }
    
    static boolean isSuccess(int code) {
        return (code >= 200 && code < 300);
    }
}
