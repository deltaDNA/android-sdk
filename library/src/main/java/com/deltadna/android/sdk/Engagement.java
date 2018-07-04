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

package com.deltadna.android.sdk;

import android.support.annotation.Nullable;

import com.deltadna.android.sdk.helpers.Objects;
import com.deltadna.android.sdk.helpers.Preconditions;
import com.deltadna.android.sdk.net.Response;

import org.json.JSONObject;

/**
 * Constructs an engagement {@link Event}.
 */
public class Engagement<T extends Engagement<T>> extends Event<T> {
    
    private static final String DEFAULT_FLAVOUR = "engagement";
    
    final String flavour;
    
    private Response<JSONObject> response;
    private int statusCode;
    private boolean cached;
    @Nullable
    private JSONObject json;
    @Nullable
    private String error;
    
    /**
     * Creates a new instance.
     *
     * @param decisionPoint the decision point
     */
    public Engagement(String decisionPoint) {
        this(decisionPoint, DEFAULT_FLAVOUR);
    }
    
    /**
     * Creates a new instance, with parameters.
     *
     * @param decisionPoint the decision point
     * @param params        the parameters
     */
    public Engagement(String decisionPoint, Params params) {
        this(decisionPoint, DEFAULT_FLAVOUR, params);
    }
    
    /**
     * Creates a new instance, with a flavour.
     *
     * @param decisionPoint the decision point
     * @param flavour       the flavour
     */
    public Engagement(String decisionPoint, String flavour) {
        this(decisionPoint, flavour, new Params());
    }
    
    /**
     * Creates a new instance, with a flavour and parameters.
     *
     * @param decisionPoint the decision point
     * @param flavour       the flavour
     * @param params        the parameters
     */
    public Engagement(
            String decisionPoint,
            String flavour,
            Params params) {
        
        super(decisionPoint, params);
        
        Preconditions.checkString(flavour, "flavour cannot be null or empty");
        
        this.flavour = flavour;
    }
    
    @Override
    public T putParam(String key, Object value) {
        return super.putParam(key, value);
    }
    
    @Override
    public T putParam(String key, JsonParams value) {
        return super.putParam(key, value);
    }
    
    @Override
    public String toString() {
        return new Objects.ToStringHelper(this)
                .add("decisionPoint", name)
                .add("flavour", flavour)
                .add("params", params)
                .add("response", response)
                .toString();
    }
    
    /**
     * Gets the status code of the response after the Engage request has
     * completed.
     * <p>
     * The status code may be outside of the successful HTTP range of 2xx in
     * the case where the request has failed, but a previous response was
     * successfully retrieved from the cache.
     *
     * @return  the status code of the response, or {@code 0} if the request
     *          hasn't completed yet
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * Gets whether the response was retrieved from the cache.
     *
     * @return {@code true} if the response is cached, else {@code false}
     */
    public boolean isCached() {
        return cached;
    }
    
    /**
     * Gets whether the response was successful after the Engage request has
     * completed.
     *
     * @return {@code true} if the response was a success, else {@code false}
     */
    public boolean isSuccessful() {
        return (json != null);
    }
    
    /**
     * Gets the JSON body of the response after the Engage request has
     * completed with a success.
     *
     * @return  the JSON body of the response, or {@code null} if the request
     *          failed
     */
    @Nullable
    public JSONObject getJson() {
        return json;
    }
    
    /**
     * Gets the error message of the response after the Engage request has
     * completed with a failure.
     *
     * @return  the error message of the response, or {@code null} if the
     *          request succeeded
     */
    @Nullable
    public String getError() {
        return error;
    }
    
    public String getDecisionPoint() {
        return name;
    }
    
    String getFlavour() {
        return flavour;
    }
    
    String getDecisionPointAndFlavour() {
        return name + '@' + flavour;
    }
    
    T setResponse(Response<JSONObject> response) {
        this.response = response;
        // unpack response for easy access
        this.statusCode = response.code;
        this.cached = response.cached;
        this.json = response.body;
        this.error = response.error;
        
        return (T) this;
    }
}
