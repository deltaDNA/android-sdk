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

package com.deltadna.android.sdk.notifications.example;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import com.deltadna.android.sdk.notifications.NotificationFactory;
import com.deltadna.android.sdk.notifications.PushMessage;

import java.io.IOException;
import java.net.URL;

/**
 * Example of a {@link NotificationFactory} which changes the look of the
 * notification after a push message is received.
 */
public class StyledNotificationFactory extends NotificationFactory {
    
    public StyledNotificationFactory(Context context) {
        super(context);
    }
    
    @Override
    public NotificationCompat.Builder configure(
            NotificationCompat.Builder builder,
            PushMessage message){

        return super.configure(builder, message);

    }
}
