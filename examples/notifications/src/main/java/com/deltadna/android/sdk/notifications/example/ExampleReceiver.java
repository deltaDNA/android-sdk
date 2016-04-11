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

package com.deltadna.android.sdk.notifications.example;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import com.deltadna.android.sdk.notifications.DDNANotifications;

/**
 * Example {@link BroadcastReceiver} demonstrating how to listen for broadcast
 * notifications of changes to the GCM registration token retrieval.
 */
class ExampleReceiver extends BroadcastReceiver {
    
    private TextView view;
    
    ExampleReceiver(TextView view) {
        this.view = view;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(
                DDNANotifications.ACTION_TOKEN_RETRIEVAL_SUCCESSFUL)) {
            
            view.setText(context.getString(
                    R.string.registration_token,
                    intent.getStringExtra(
                            DDNANotifications.EXTRA_REGISTRATION_TOKEN)));
        } else if (intent.getAction().equals(
                DDNANotifications.ACTION_TOKEN_RETRIEVAL_FAILED)) {
            
            view.setText(context.getString(
                    R.string.registration_token,
                    intent.getSerializableExtra(
                            DDNANotifications.EXTRA_FAILURE_REASON)));
            
            /*
             * We may do something more here, such as set an alarm for a later
             * time to retry the registration.
             */
        }
    }
}
