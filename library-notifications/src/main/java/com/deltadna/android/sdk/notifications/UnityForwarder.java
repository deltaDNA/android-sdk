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

package com.deltadna.android.sdk.notifications;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.Queue;

final class UnityForwarder {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + UnityForwarder.class.getSimpleName();
    
    private static final Class<?> PLAYER_ACTIVITY;
    static {
        Class<?> playerActivity;
        try {
            playerActivity = Class.forName(
                    "com.unity3d.player.UnityPlayerActivity");
        } catch (ClassNotFoundException e) {
            playerActivity = null;
        }
        
        PLAYER_ACTIVITY = playerActivity;
    }
    
    private static final Class<?> PLAYER;
    static {
        Class<?> player;
        try {
            player = Class.forName("com.unity3d.player.UnityPlayer");
        } catch (ClassNotFoundException e) {
            player = null;
        }
        
        PLAYER = player;
    }
    
    private static UnityForwarder instance;
    
    static boolean isPresent() {
        return PLAYER_ACTIVITY != null;
    }
    
    static synchronized UnityForwarder getInstance() {
        if (instance == null) {
            instance = new UnityForwarder();
        }
        
        return instance;
    }
    
    private final Queue<Message> deferred = new LinkedList<>();
    private boolean loaded;
    
    private UnityForwarder() {}
    
    void markLoaded() {
        Log.d(TAG, "Marked as loaded");
        
        loaded = true;
        
        if (!deferred.isEmpty()) {
            final Message message = deferred.remove();
            sendMessage(
                    message.gameObject,
                    message.methodName,
                    message.message);
        }
    }
    
    void forward(String gameObject, String methodName, String message) {
        if (loaded) {
            sendMessage(gameObject, methodName, message);
        } else {
            Log.d(TAG, "Deferring message due to not loaded");
            deferred.add(new Message(gameObject, methodName, message));
        }
    }
    
    private static void sendMessage(
            String gameObject,
            String methodName,
            String message) {
        
        try {
            PLAYER.getDeclaredMethod(
                    "UnitySendMessage",
                    String.class,
                    String.class,
                    String.class)
                    .invoke(PLAYER,
                            gameObject,
                            methodName,
                            message);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Failed sending message to Unity", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Failed sending message to Unity", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Failed sending message to Unity", e);
        }
    }
    
    private static final class Message {
        
        final String gameObject;
        final String methodName;
        final String message;
        
        Message(String gameObject, String methodName, String message) {
            this.gameObject = gameObject;
            this.methodName = methodName;
            this.message = message;
        }
    }
}
