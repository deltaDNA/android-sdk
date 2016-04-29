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

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.deltadna.android.sdk.helpers.Settings;

class SessionRefreshHandler implements Application.ActivityLifecycleCallbacks {
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresher = new Runnable() {
        @Override
        public void run() {
            listener.onExpired();
        }
    };
    
    private final Application app;
    private final Settings settings;
    private final Listener listener;
    
    SessionRefreshHandler(
            Application app,
            Settings settings,
            Listener listener) {
        
        this.app = app;
        this.settings = settings;
        this.listener = listener;
    }
    
    void register() {
        app.registerActivityLifecycleCallbacks(this);
    }
    
    void unregister() {
        handler.removeCallbacks(refresher);
        app.unregisterActivityLifecycleCallbacks(this);
    }
    
    @Override
    public void onActivityCreated(
            Activity activity, Bundle savedInstanceState) {}
    
    @Override
    public void onActivityStarted(Activity activity) {
        handler.removeCallbacks(refresher);
    }
    
    @Override
    public void onActivityResumed(Activity activity) {}
    
    @Override
    public void onActivityPaused(Activity activity) {}
    
    @Override
    public void onActivityStopped(Activity activity) {
        handler.removeCallbacks(refresher);
        
        if (settings.getSessionTimeout() > 0) {
            handler.postDelayed(refresher, settings.getSessionTimeout());
        }
    }
    
    @Override
    public void onActivitySaveInstanceState(
            Activity activity, Bundle outState) {}
    
    @Override
    public void onActivityDestroyed(Activity activity) {}
    
    interface Listener {
        
        void onExpired();
    }
}
