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

/**
 * Contains statically loaded Unity classes, may be null if the library is not
 * running as part of the Unity SDK. Check through {@link #isPresent()} before
 * access.
 */
final class Unity {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + RegistrationIntentService.class.getSimpleName();
    
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
    
    static boolean isPresent() {
        return PLAYER_ACTIVITY != null;
    }
    
    static void sendMessage(
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
    
    private Unity() {}
}
