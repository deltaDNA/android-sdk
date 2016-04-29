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

import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Statically loaded Unity classes, may be null if the library is not running
 * as part of the Unity SDK.
 */
final class UnityClasses {
    
    @Nullable
    static final Class<?> PLAYER_ACTIVITY;
    static {
        Class<?> playerActivity;
        try {
            playerActivity = Class.forName(
                    "com.unity3d.player.UnityPlayerActivity");
        } catch (ClassNotFoundException e) {
            Log.d(BuildConfig.LOG_TAG, "Unity not in classpath");
            playerActivity = null;
        }
        
        PLAYER_ACTIVITY = playerActivity;
    }
    
    @Nullable
    static final Class<?> PLAYER;
    static {
        Class<?> player;
        try {
            player = Class.forName("com.unity3d.player.UnityPlayer");
        } catch (ClassNotFoundException e) {
            Log.d(BuildConfig.LOG_TAG, "Unity not in classpath");
            player = null;
        }
        
        PLAYER = player;
    }
    
    private UnityClasses() {}
}
