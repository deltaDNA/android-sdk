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

import android.app.Application;
import android.util.Log;

import com.deltadna.android.sdk.DDNA;
import com.deltadna.android.sdk.consent.ConsentTracker;
import com.deltadna.android.sdk.notifications.DDNANotifications;

public class ExampleApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        DDNA.initialise(new DDNA.Configuration(
                this,
                "07575004106474324897044893014183",
                "http://collect3347ndrds.deltadna.net/collect/api",
                "http://engage3347ndrds.deltadna.net"));

        DDNA.instance().isPiplConsentRequired(new ConsentTracker.Callback() {
            @Override
            public void onSuccess(boolean requiresConsent) {
                if (requiresConsent) {
                    // In our example, we assume we have consent, but you should check to make sure this is the case!
                    DDNA.instance().setPiplConsent(true, true);
                }
            }

            @Override
            public void onFailure(Throwable exception) {
                Log.e("EXAMPLE", "Failed to check for PIPL consent", exception);
                // Try again later.
            }
        });
        
        // only needs to be called if targeting API 26 or higher
        DDNANotifications.setReceiver(ExampleReceiver.class);
    }
}
