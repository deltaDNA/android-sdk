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

import com.deltadna.android.sdk.helpers.Preconditions;
import com.deltadna.android.sdk.net.Response;

/**
 * {@link Exception} for a failed {@link Response}, where the code
 * does not fall within the 200-299 range.
 */
public class ResponseException extends Exception {
    
    public final Response response;
    
    ResponseException(Response response) {
        super("Failed with response " + response);
        
        Preconditions.checkArg(
                !response.isSuccessful(),
                "exception cannot be used for successful responses");
        
        this.response = response;
    }
}
