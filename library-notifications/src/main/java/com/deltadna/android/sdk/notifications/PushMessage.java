/*
 * Copyright (c) 2017 deltaDNA Ltd. All rights reserved.
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

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.util.Log;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Class representing a push message sent from the platform to the device.
 */
public class PushMessage implements Serializable {
    
    protected static final String TITLE = "title";
    protected static final String MESSAGE = "alert";
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + PushMessage.class.getSimpleName();
    
    /**
     * Id of the push message sender.
     */
    public final String from;
    /**
     * Raw payload of the push message.
     */
    public final Map<String, String> data;
    
    /**
     * Id of the push message.
     */
    public final long id;
    /**
     * Icon for the push message.
     */
    @DrawableRes
    public final int icon;
    /**
     * Title for the push message.
     */
    public final String title;
    /**
     * Message for the push message.
     */
    public final String message;
    
    /**
     * Creates a new instance.
     *
     * @param context   the context in which the instance is being created
     * @param from      the sender of the remote message
     * @param data      the data of the remote message
     */
    PushMessage(Context context, String from, Map<String, String> data) {
        this.from = from;
        this.data = new HashMap<>(data);
        
        id = getId(data);
        icon = getIcon(context);
        title = getTitle(context, data);
        message = getMessage(data);
    }
    
    /**
     * Extracts the id from a push payload to be used for posting a
     * notification.
     *
     * @param data  the notification payload
     *
     * @return      notification id
     */
    protected long getId(Map<String, String> data) {
        if (data.containsKey("_ddCampaign")) {
            final String value = data.get("_ddCampaign");
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Failed parsing _ddCampaign to a long", e);
            }
        }
        
        return -1;
    }
    
    /**
     * Extracts the icon from a push payload to be used for posting a
     * notification.
     *
     * @param context   the context
     *
     * @return          icon resource
     */
    @DrawableRes
    protected int getIcon(Context context) {
        final Bundle metaData = MetaData.get(context);
        
        if (metaData.containsKey(MetaData.NOTIFICATION_ICON)) {
            Log.w(TAG, "Use of ddna_notification_icon in the manifest has been deprecated");
            
            try {
                final int value = context.getResources().getIdentifier(
                        metaData.getString(MetaData.NOTIFICATION_ICON),
                        "drawable",
                        context.getPackageName());
                
                if (value == 0) {
                    throw new RuntimeException(
                            "Failed to find drawable resource for ddna_notification_icon");
                } else {
                    return value;
                }
            } catch (Resources.NotFoundException e) {
                throw new RuntimeException(
                        "Failed to find drawable resource for ddna_notification_icon",
                        e);
            }
        } else {
            try {
                final int res = context.getPackageManager()
                        .getApplicationInfo(
                                context.getPackageName(),
                                PackageManager.GET_META_DATA).icon;
                
                if (res == 0) {
                    Log.w(TAG, "Failed to find application's icon");
                    return R.drawable.ddna_ic_stat_logo;
                } else {
                    return res;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Failed to find application's icon", e);
                return R.drawable.ddna_ic_stat_logo;
            }
        }
    }
    
    /**
     * Extracts the title from a push payload to be used for posting a
     * notification.
     *
     * @param data  the notification payload
     *
     * @return      notification title
     */
    protected String getTitle(Context context, Map<String, String> data) {
        final Bundle metaData = MetaData.get(context);
        
        if (data.containsKey(TITLE)) {
            return data.get(TITLE);
        } else if (metaData.containsKey(MetaData.NOTIFICATION_TITLE)) {
            Log.w(TAG, "Use of ddna_notification_title in the manifest has been deprecated");
            
            final Object value = metaData.get(MetaData.NOTIFICATION_TITLE);
            if (value instanceof String) {
                return (String) value;
            } else if (value instanceof Integer) {
                try {
                    return context.getString((Integer) value);
                } catch (Resources.NotFoundException e) {
                    throw new RuntimeException(
                            "Failed to find string resource for ddna_notification_title",
                            e);
                }
            } else {
                throw new RuntimeException(String.format(
                        Locale.US,
                        "Found %s for %s, only string literals or string resources allowed",
                        value,
                        MetaData.NOTIFICATION_TITLE));
            }
        } else {
            return String.valueOf(context.getPackageManager()
                    .getApplicationLabel(context.getApplicationInfo()));
        }
    }
    
    /**
     * Extracts the message from a push payload to be used for posting a
     * notification.
     *
     * @param data  the notification payload
     *
     * @return      notification message
     */
    protected String getMessage(Map<String, String> data) {
        if (!data.containsKey(MESSAGE)) {
            Log.w(TAG, "Missing 'message' key in push message");
            return "";
        } else {
            return data.get(MESSAGE);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "%s{from: %s, data: %s, id: %d, icon: %s, title: %s, message: %s}",
                getClass().getSimpleName(),
                from,
                data,
                id,
                icon,
                title,
                message);
    }
}
