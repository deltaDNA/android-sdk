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

package com.deltadna.android.sdk

import android.app.Activity
import android.os.Build
import com.deltadna.android.sdk.helpers.Settings
import com.nhaarman.mockito_kotlin.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(constants = BuildConfig::class,
        sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP))
class SessionRefreshHandlerTest {
    
    private companion object { val TIMEOUT = 1000 }
    
    private val settings = with(mock<Settings>()) {
        whenever(sessionTimeout).thenReturn(TIMEOUT)
        this
    }
    private val listener = mock<SessionRefreshHandler.Listener>()
    private val activity = Robolectric.buildActivity(Activity::class.java)
    
    private var uut = SessionRefreshHandler(
            RuntimeEnvironment.application,
            settings,
            listener)
    
    @Before
    fun before() {
        activity.create()
        uut = SessionRefreshHandler(
                RuntimeEnvironment.application,
                settings,
                listener)
    }
    
    @After
    fun after() {
        activity.destroy()
        reset(listener)
    }
    
    @Test
    fun expiresAfterStop() {
        uut.register()
        activity.start()
        activity.stop()
        
        Robolectric.getForegroundThreadScheduler().advanceBy(TIMEOUT.toLong())
        
        verify(listener).onExpired()
    }
    
    @Test
    fun doesNotExpireOnRestart() {
        uut.register()
        activity.stop()
        activity.restart()
        
        Robolectric.getForegroundThreadScheduler().advanceBy(TIMEOUT.toLong())
        
        verify(listener, never()).onExpired()
    }
    
    @Test
    fun doesNotExpireAfterUnregister() {
        uut.register()
        activity.stop()
        uut.unregister()
        
        Robolectric.getForegroundThreadScheduler().advanceBy(TIMEOUT.toLong())
        
        verify(listener, never()).onExpired()
    }
}
