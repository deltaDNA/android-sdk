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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

abstract class SessionRefreshHandler {
    
    private final Listener listener;
    
    protected final Handler handler = new Handler(Looper.getMainLooper());
    protected final Runnable refresher = new Runnable() {
        @Override
        public void run() {
            listener.onExpired();
        }
    };
    
    private SessionRefreshHandler(Listener listener) {
        this.listener = listener;
    }
    
    abstract void register();
    
    abstract void unregister();
    
    static SessionRefreshHandler create(
            Listener listener,
            Application app,
            int expiry) {
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return new SessionRefreshHandlerIcs(listener, app, expiry);
        } else {
            return new SessionRefreshHandlerDefault(listener);
        }
    }
    
    interface Listener {
        
        void onExpired();
    }
    
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static final class SessionRefreshHandlerIcs
            extends SessionRefreshHandler
            implements Application.ActivityLifecycleCallbacks {
        
        private final Application app;
        private final int expiry;
        
        private SessionRefreshHandlerIcs(
                Listener listener,
                Application app,
                int expiry) {
            
            super(listener);
            
            this.app = app;
            this.expiry = expiry;
        }
        
        @Override
        void register() {
            app.registerActivityLifecycleCallbacks(this);
        }
        
        @Override
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
            handler.postDelayed(refresher, expiry);
        }
        
        @Override
        public void onActivitySaveInstanceState(
                Activity activity, Bundle outState) {}
        
        @Override
        public void onActivityDestroyed(Activity activity) {}
    }
    
    private static final class SessionRefreshHandlerDefault
            extends SessionRefreshHandler {
        
        private SessionRefreshHandlerDefault(Listener listener) {
            super(listener);
        }
        
        @Override
        void register() {
            Log.w(  BuildConfig.LOG_TAG,
                    "Automatic session refreshing not supported on APIs 9-13");
        }
        
        @Override
        void unregister() {}
    }
}
