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

import androidx.annotation.Nullable;

import com.deltadna.android.sdk.helpers.Objects;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;

/**
 * HTTP response, including the code, body, and error if applicable.
 * <p>
 * The body is in byte form and {@link T} if a {@link ResponseBodyConverter}
 * has been provided.
 *
 * @param <T> type of the converted response body
 */
public final class Response<T> {
    
    /**
     * HTTP status code of the response.
     */
    public final int code;
    /**
     * Whether the response is from a cache.
     */
    public final boolean cached;
    /**
     * Response in plain bytes, may be the error message if the request was a
     * failure.
     */
    public final byte[] bytes;
    /**
     * Converted body of the response if the request was a success.
     */
    public final T body;
    /**
     * Error message of the response if the request was a failure.
     */
    public final String error;
    
    public Response(int code, boolean cached, byte[] bytes, T body, String error) {
        this.code = code;
        this.cached = cached;
        this.bytes = bytes;
        this.body = body;
        this.error = error;
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
                && cached == other.cached
                && Arrays.equals(bytes, other.bytes)
                && Objects.equals(body, other.body)
                && Objects.equals(error, other.error));
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] {
                code,
                cached,
                Arrays.hashCode(bytes),
                body,
                error});
    }
    
    @Override
    public String toString() {
        return new Objects.ToStringHelper(this)
                .add("code", code)
                .add("cached", cached)
                .add("body", body)
                .add("error", error)
                .toString();
    }
    
    static <T> Response<T> create(
            HttpURLConnection connection,
            @Nullable ResponseBodyConverter<T> converter) throws Exception {
        
        final int code = connection.getResponseCode();
        return create(
                code,
                connection.getContentLength(),
                isSuccess(code)
                        ? connection.getInputStream()
                        : connection.getErrorStream(),
                converter);
    }
    
    private static <T> Response<T> create(
            int code,
            int contentLength,
            InputStream stream,
            @Nullable ResponseBodyConverter<T> converter) throws Exception {
        
        final ByteArrayOutputStream buffer;
        //noinspection TryFinallyCanBeTryWithResources
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
        return new Response<>(
                code,
                false,
                bytes,
                (isSuccess(code) && converter != null)
                        ? converter.convert(bytes)
                        : null,
                !isSuccess(code)
                        ? ResponseBodyConverter.STRING.convert(bytes)
                        : null);
    }
    
    static boolean isSuccess(int code) {
        return (code >= 200 && code < 300);
    }
}
