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

package com.deltadna.android.sdk.listeners;

public interface EventListener {
    
    void onStarted();
    void onStopped();
    
    void onNewSession();
    
    /**
     * Will be called when the session configuration will be successfully
     * retrieved.
     *
     * @param cached    {@code true} if the configuration response was cached,
     *                  else {@code false}
     */
    void onSessionConfigured(boolean cached);
    /**
     * Will be called when the session configuration will fail to be retrieved.
     *
     * @param cause the cause of the failure
     */
    void onSessionConfigurationFailed(Throwable cause);
    
    /**
     * Will be called when the image cache will be successfully populated.
     */
    void onImageCachePopulated();
    /**
     * Will be called when the image cache will fail to be populated.
     *
     * @param reason the reason for the failure
     */
    void onImageCachingFailed(Throwable reason);
}
