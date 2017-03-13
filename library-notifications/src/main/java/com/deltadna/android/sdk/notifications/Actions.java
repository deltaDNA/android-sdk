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

/**
 * Actions must match definitions in AndroidManifest.xml.
 */
class Actions {
    
    public static final String PREFIX = "com.deltadna.android.sdk.notifications.";
    public static final String REGISTERED = PREFIX + "REGISTERED";
    public static final String REGISTRATION_FAILED = PREFIX + "REGISTRATION_FAILED";
    public static final String MESSAGE_RECEIVED = PREFIX + "MESSAGE_RECEIVED";
    public static final String NOTIFICATION_POSTED = PREFIX + "NOTIFICATION_POSTED";
    public static final String NOTIFICATION_OPENED = PREFIX + "NOTIFICATION_OPENED";
    public static final String NOTIFICATION_DISMISSED = PREFIX + "NOTIFICATION_DISMISSED";
    
    public static final String REGISTRATION_FAILURE_REASON = "registration_failure_reason";
    public static final String REGISTRATION_TOKEN = "registration_token";
    public static final String PUSH_MESSAGE = "push_message";
    public static final String NOTIFICATION_INFO = "notification_info";
    
    static final String LAUNCH = "launch";
    
    private Actions() {}
}
