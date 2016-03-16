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

import com.deltadna.android.sdk.exceptions.ResponseExceptionFactory;
import com.deltadna.android.sdk.helpers.Objects;
import com.deltadna.android.sdk.helpers.Preconditions;
import com.deltadna.android.sdk.listeners.RequestListener;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Encapsulates the required details and logic for performing an
 * HTTP request.
 * 
 * @param <V> type of the result
 */
final class Request<V> implements Callable<Response<V>> {
    
    private static final int CONNECTION_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 10000;
    
    private final URL url;
    private final RequestMethod method;
    private final Map<String, String> headers;
    @Nullable
    private final RequestBody body;
    
    private final int connectionTimeout;
    private final int readTimeout;
    private final int maxRetries;
    
    @Nullable
    private ResponseBodyConverter<V> converter;
    
    // TODO following members should perhaps be moved out
    final int retryDelay;
    @Nullable
    RequestListener<Response<V>> listener;
    int runs;
    
    private Request(
            URL url,
            RequestMethod method,
            Map<String, String> headers,
            @Nullable RequestBody body,
            int connectionTimeout,
            int readTimeout,
            int maxRetries,
            int retryDelay) {
        
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.body = body;
        
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
    }
    
    Request<V> setConverter(
            @Nullable ResponseBodyConverter<V> converter) {
        
        this.converter = converter;
        return this;
    }
    
    Request<V> setRequestListener(
            @Nullable RequestListener<Response<V>> listener) {
        
        this.listener = listener;
        return this;
    }
    
    boolean shouldRetry() {
        return (runs <= maxRetries);
    }
    
    @Override
    public Response<V> call() throws Exception {
        runs++;
        
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            
            connection.setConnectTimeout(connectionTimeout);
            connection.setReadTimeout(readTimeout);

            method.set(connection);

            for (final String header : headers.keySet()) {
                connection.setRequestProperty(header, headers.get(header));
            }

            if (body != null) {
                body.fill(connection);
            }
            
            connection.connect();
            
            final int code = connection.getResponseCode();
            if (Response.isSuccess(code)) {
                return Response.create(
                        code,
                        connection.getContentLength(),
                        connection.getInputStream(),
                        converter);
            } else {
                throw ResponseExceptionFactory.create(Response.create(
                        code,
                        connection.getContentLength(),
                        connection.getErrorStream(),
                        ResponseBodyConverter.STRING));
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    @Override
    public String toString() {
        return new Objects.ToStringHelper(this)
                .add("url", url)
                .add("method", method)
                .add("headers", headers)
                .add("body", body)
                .toString();
    }

    /**
     * Builder providing a fluid API for creating a {@link Request}.
     * 
     * @param <V> type of the result
     */
    static final class Builder<V> {
        
        private RequestMethod method;
        private URL url;
        private Map<String, String> headers;
        private RequestBody body;
        
        private int connectionTimeout = CONNECTION_TIMEOUT;
        private int readTimeout = READ_TIMEOUT;
        private int maxRetries;
        private int retryDelay;
        
        Builder() {
            method = RequestMethod.GET;
            headers = new HashMap<String, String>();
        }
        
        Builder<V> get() {
            return method(RequestMethod.GET, null);
        }
        
        Builder<V> post(RequestBody body) {
            Preconditions.checkArg(body != null, "body cannot be empty");
            return method(RequestMethod.POST, body);
        }
        
        Builder<V> url(String url) {
            try {
                this.url = new URL(url);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
            return this;
        }
        
        Builder<V> header(String name, String value) {
            headers.put(name, value);
            return this;
        }
        
        Builder<V> connectionTimeout(int milliseconds) {
            Preconditions.checkArg(milliseconds >= 0, "timeout cannot be < 0");
            connectionTimeout = milliseconds;
            return this;
        }
        
        Builder<V> readTimeout(int milliseconds) {
            Preconditions.checkArg(milliseconds >= 0, "timeout cannot be < 0");
            readTimeout = milliseconds;
            return this;
        }
        
        Builder<V> maxRetries(int retries) {
            Preconditions.checkArg(retries >= 0, "retries cannot be < 0");
            maxRetries = retries;
            return this;
        }
        
        Builder<V> retryDelay(int milliseconds) {
            Preconditions.checkArg(milliseconds >= 0, "delay cannot be < 0");
            retryDelay = milliseconds;
            return this;
        }
        
        Request<V> build() {
            Preconditions.checkArg(url != null, "url has not been specified");
            return new Request<V>(
                    url,
                    method,
                    headers,
                    body,
                    connectionTimeout,
                    readTimeout,
                    maxRetries,
                    retryDelay);
        }
        
        private Builder<V> method(
                RequestMethod method,
                @Nullable RequestBody body) {
            
            this.method  = method;
            this.body = body;
            return this;
        }
    }
}
