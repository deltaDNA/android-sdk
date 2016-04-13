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

import com.deltadna.android.sdk.Engagement;

/**
 * Listener to be used for Engage requests.
 */
public interface EngageListener<E extends Engagement> {
    
    /**
     * Notifies the listener that the request has completed.
     *
     * @param engagement the engagement with response attributes
     */
    void onCompleted(E engagement);
    
    /**
     * Notifies the listener that an error has happened during the request.
     * <p>
     * The error {@code t} could be an {@link java.io.IOException} if there was
     * a connectivity problem or a timeout, or an {@link InterruptedException}
     * if the request thread was interrupted during execution.
     * <p>
     * If this method is called {@link #onCompleted(Engagement)} will not be
     * called.
     *
     * @param t the cause of the error
     */
    void onError(Throwable t);
}
