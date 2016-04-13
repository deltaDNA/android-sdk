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

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.deltadna.android.sdk.BuildConfig;
import com.deltadna.android.sdk.helpers.Settings;
import com.deltadna.android.sdk.listeners.RequestListener;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * TODO make package private after ads decoupling
 * TODO collapse functionality into event dispatcher
 */
public class NetworkManager {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + NetworkManager.class.getSimpleName();
    
    private final String collectUrl;
    private final String engageUrl;
    private final Settings settings;
    
    @Nullable
    private final String hash;
    @Nullable
    private final MessageDigest md5;
    
    private final NetworkDispatcher dispatcher;
    
    public NetworkManager(
            String envKey,
            String collectUrl,
            String engageUrl,
            Settings settings,
            @Nullable String hash) {
        
        this.collectUrl = collectUrl + '/' + envKey;
        this.engageUrl = engageUrl + '/' + envKey;
        this.settings = settings;
        
        this.hash = hash;
        MessageDigest md = null;
        if (hash != null && !hash.isEmpty()) {
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                Log.w(TAG, "Events will not be hashed", e);
            }
        }
        md5 = md;
        
        dispatcher = new NetworkDispatcher(
                new Handler(Looper.getMainLooper()));
    }
    
    public CancelableRequest collect(
            JSONObject payload,
            @Nullable RequestListener<Void> listener) {
        
        return dispatcher.enqueue(
                new Request.Builder<Void>()
                        .post(RequestBody.json(payload))
                        .url(payload.has("eventList")
                                ? buildHashedEndpoint(collectUrl + "/bulk", payload.toString())
                                : buildHashedEndpoint(collectUrl, payload.toString()))
                        .header("Accept", "application/json")
                        .maxRetries(settings.httpRequestMaxRetries())
                        .retryDelay(Math.round(settings.httpRequestRetryDelaySeconds() * 1000))
                        .build(),
                listener);
    }
    
    public CancelableRequest engage(
            JSONObject payload,
            RequestListener<JSONObject> listener) {
        
        // TODO tweak timeouts to make engage come back within the magic 5s
        return dispatcher.enqueue(
                new Request.Builder<JSONObject>()
                        .post(RequestBody.json(payload))
                        .url(buildHashedEndpoint(engageUrl, payload.toString()))
                        .header("Accept", "application/json")
                        .build(),
                ResponseBodyConverter.JSON,
                listener);
    }
    
    public CancelableRequest fetch(
            String url,
            final File dest,
            RequestListener<File> listener) {
        
        // TODO tweak timeouts as this should come back quickly as well
        return dispatcher.enqueue(
                new Request.Builder<File>().get().url(url).build(),
                new ResponseBodyConverter<File>() {
                    @Override
                    public File convert(byte[] input) throws Exception {
                        final FileOutputStream os = new FileOutputStream(dest);
                        os.write(input);
                        os.close();

                        return dest;
                    }
                },
                listener);
    }
    
    private String buildHashedEndpoint(String endpoint, String payload) {
        final StringBuilder builder = new StringBuilder(endpoint);
        
        if (hash != null && md5 != null) {
            builder.append("/hash/");
            
            final byte[] messageBytes;
            try {
                messageBytes = (payload + hash).getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
            
            final byte[] digest = md5.digest(messageBytes);
            for (final byte b : digest) {
                builder.append(String.format(Locale.US, "%02X", b));
            }
        }
        
        return builder.toString();
    }
}
