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

package com.deltadna.android.sdk.exceptions;

import com.deltadna.android.sdk.DDNA;

/**
 * {@link IllegalStateException} that will be thrown when an instance of
 * {@link com.deltadna.android.sdk.DDNA} is requested through
 * {@link DDNA#instance()} without having been initialised first by calling
 * {@link DDNA#initialise(DDNA.Configuration)}
 */
public final class NotInitialisedException extends IllegalStateException {
    
    public NotInitialisedException() {
        super("SDK has not been initialised prior to getting an instance");
    }
}
