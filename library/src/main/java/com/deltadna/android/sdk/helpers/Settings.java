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

package com.deltadna.android.sdk.helpers;

/**
 * DeltaDNA runtime setting.
 *
 * Note: default values should be set here or initialised before start() is called.
 */
public class Settings{
    
	/**
	 * Controls whether a 'newPlayer' event is sent the first time the game is played.
	 */
	private boolean mOnFirstRunSendNewPlayerEvent = true;
	/**
	 * Controls whether a 'clientDevice' event is sent after the Init call.
	 */
	private boolean mOnInitSendClientDeviceEvent = true;
	/**
	 * Controls whether a 'gameStarted' event is sent after the Init call.
	 */
	private boolean mOnInitSendGameStartedEvent = true;
	/**
	 * Controls if additional debug is output to the console.
	 */
	private boolean mDebugMode = false;
	/**
	 * Controls if events are uploaded automatically in the background.
	 */
	private boolean mBackgroundEventUpload = true;
	/**
	 * Controls how long after the <see cref="Init"/> call we wait before
	 * sending the first event upload.
	 */
	private int mBackgroundEventUploadStartDelaySeconds = 1;
	/**
	 * Controls how fequently events are uploaded automatically.
	 */
	private int mBackgroundEventUploadRepeatRateSeconds = 60;
    
    private int sessionTimeout = 5 * 60 * 1000;
    
    private int httpRequestMaxRetries = 0;
    /**
     * In seconds.
     */
    private int httpRequestRetryDelay = 2;
    /**
     * In seconds.
     */
    private int httpRequestCollectTimeout = 55;
    /**
     * In seconds.
     */
    private int httpRequestEngageTimeout = 5;
    
    private boolean useInternalStorageForEvents;
    private boolean useInternalStorageForEngage;
    
	/**
	 * TRUE to send new player event on first run of application.
	 *
	 * @return TRUE if the new player event will be sent, FALSE otherwise.
	 */
	public boolean onFirstRunSendNewPlayerEvent(){
		return mOnFirstRunSendNewPlayerEvent;
	}
	/**
	 * Sets the first run event behaviour.
	 *
	 * @param b TRUE to send new player event on first run, FALSE otherwise.
	 */
	public void setOnFirstRunSendNewPlayerEvent(boolean b){
		mOnFirstRunSendNewPlayerEvent = b;
	}

	/**
	 * TRUE to send the client device event.
	 *
	 * @return TRUE if the client device event will be sent, FALSE otherwise.
	 */
	public boolean onInitSendClientDeviceEvent(){
		return mOnInitSendClientDeviceEvent;
	}
	/**
	 * Sets the client device event behaviour.
	 *
	 * @param b TRUE to send client device event, FALSE otherwise.
	 */
	public void setOnInitSendClientDeviceEvent(boolean b){
		mOnInitSendClientDeviceEvent = b;
	}

	/**
	 * TRUE to send the game started event.
	 *
	 * @return TRUE if the game started event will be sent, FALSE otherwise.
	 */
	public boolean onInitSendGameStartedEvent(){
		return mOnInitSendGameStartedEvent;
	}
	/**
	 * Sets the game started event behaviour.
	 *
	 * @param b TRUE to send game started event, FALSE otherwise.
	 */
	public void setOnInitSendGameStartedEvent(boolean b){
		mOnInitSendGameStartedEvent = b;
	}

	/**
	 * Gets the SDK debig mode.
	 *
	 * @return TRUE if the SDK is in debug mode, FALSE otherwise.
	 */
	public boolean debugMode(){
		return mDebugMode;
	}
	/**
	 * Sets the SDK debug mode.
	 *
	 * @param b The new SDK debug mode.
	 */
	public void setDebugMode(boolean b){
		mDebugMode = b;
	}
    
	/**
	 * Test if background event uploading is enabled.
	 *
	 * @return TRUE if background uploading is enabled, FALSE otherwise.
	 */
	public boolean backgroundEventUpload(){
		return mBackgroundEventUpload;
	}
	/**
	 * Sets the background uploading behaviour.
	 *
	 * @param b TRUE to enable background uploading, FALSE otherwise.
	 */
	public void setBackgroundEventUpload(boolean b){
		mBackgroundEventUpload = b;
	}

	/**
	 * The delay in seconds before background upload starts.
	 *
	 * @return The background upload delay in seconds.
	 */
	public int backgroundEventUploadStartDelaySeconds(){
		return mBackgroundEventUploadStartDelaySeconds;
	}
	/**
	 * Sets the delay before background upload starts.
	 *
	 * @param i The new delay in seconds.
	 */
	public void setBackgroundEventUploadStartDelaySeconds(int i){
		mBackgroundEventUploadStartDelaySeconds = i;
	}

	/**
	 * The background upload repeat rate.
	 *
	 * @return The upload repeat rate in seconds.
	 */
	public int backgroundEventUploadRepeatRateSeconds(){
		return mBackgroundEventUploadRepeatRateSeconds;
	}
	/**
	 * Sets the background upload repeat rate.
	 *
	 * @param i The new background upload repeat rate in seconds.
	 */
	public void setBackgroundEventUploadRepeatRateSeconds(int i){
		mBackgroundEventUploadRepeatRateSeconds = i;
	}
    
    /**
     * Gets the session timeout.
     *
     * @return the session timeout, in milliseconds
     */
    public int getSessionTimeout() {
        return sessionTimeout;
    }
    
    /**
     * Sets the session timeout.
     * <p>
     * A timeout of 0 will disable automatic session refreshing.
     *
     * @param timeout the session timeout, in milliseconds
     *
     * @throws IllegalArgumentException if {@code timeout} is negative
     */
    public void setSessionTimeout(int timeout) {
        Preconditions.checkArg(timeout >= 0, "timeout cannot be negative");
        
        sessionTimeout = timeout;
    }
    
    /**
     * Gets the number of retries to perform when an HTTP request fails.
     * <p>
     * Only applies to Collect requests.
     *
     * @return the number of retries
     */
    public int getHttpRequestMaxRetries() {
        return httpRequestMaxRetries;
    }
    
    /**
     * Sets the number of retries to perform when an HTTP request fails. A
     * value of {@code 0} means no retries will be performed.
     * <p>
     * Only applies to Collect requests.
     *
     * @param retries the number of retries
     *
     * @throws IllegalArgumentException if the {@code retries} is negative
     */
    public void setHttpRequestMaxRetries(int retries) {
        Preconditions.checkArg(retries >= 0, "retries cannot be negative");
        
        httpRequestMaxRetries = retries;
    }
    
    /**
     * Gets the time to wait in seconds before retrying HTTP requests.
     * <p>
     * Only applies to Collect requests.
     *
     * @return the time to wait in seconds
     */
    public int getHttpRequestRetryDelay() {
        return httpRequestRetryDelay;
    }
    
    /**
     * Sets the time to wait in seconds before retrying HTTP requests.
     * <p>
     * Only applies to Collect requests.
     *
     * @param seconds the time to wait in seconds
     *
     * @throws IllegalArgumentException if the {@code seconds} is negative
     */
    public void setHttpRequestRetryDelay(int seconds) {
        Preconditions.checkArg(seconds >= 0, "value cannot be negative");
        
        httpRequestRetryDelay = seconds;
    }
    
    /**
     * Gets the connection timeout in seconds for Collect HTTP requests.
     *
     * @return the timeout in seconds
     */
    public int getHttpRequestCollectTimeout() {
        return httpRequestCollectTimeout;
    }

    /**
     * Sets the connection timeout in seconds for Collect HTTP requests.
     *
     * @param seconds the timeout in seconds
     *
     * @throws IllegalArgumentException if the {@code seconds} is negative
     */
    public void setHttpRequestCollectTimeout(int seconds) {
        Preconditions.checkArg(seconds >= 0, "value cannot be negative");
        
        httpRequestCollectTimeout = seconds;
    }
    
    /**
     * Gets the connection timeout in seconds for Engage HTTP requests.
     *
     * @return the timeout in seconds
     */
    public int getHttpRequestEngageTimeout() {
        return httpRequestEngageTimeout;
    }
    
    /**
     * Sets the connection timeout in seconds for Engage HTTP requests.
     *
     * @param seconds the timeout in seconds
     *
     * @throws IllegalArgumentException if the {@code seconds} is negative
     */
    public void setHttpRequestEngageTimeout(int seconds) {
        Preconditions.checkArg(seconds >= 0, "value cannot be negative");
        
        httpRequestEngageTimeout = seconds;
    }
    
    public boolean isUseInternalStorageForEvents() {
        return useInternalStorageForEvents;
    }
    
    public void setUseInternalStorageForEvents(boolean useInternal) {
        useInternalStorageForEvents = useInternal;
    }
    
    public boolean isUseInternalStorageForEngage() {
        return useInternalStorageForEngage;
    }
    
    public void setUseInternalStorageForEngage(boolean useInternal) {
        useInternalStorageForEngage = useInternal;
    }
}
