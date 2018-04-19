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

import com.deltadna.android.sdk.helpers.Settings
import com.deltadna.android.sdk.listeners.EventListener
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DDNAImplTest {
    
    private var uut = createUut()
    
    @Before
    fun before() {
        uut = createUut()
    }
    
    @Test
    fun startSdkEvents() {
        val listener = mock<EventListener>()
        uut.register(listener)
        uut.startSdk()
        
        inOrder(listener) {
            verify(listener).onNewSession()
            verify(listener).onStarted()
        }
    }
    
    @Test
    fun stopSdkEvents() {
        val listener = mock<EventListener>()
        uut.register(listener)
        uut.startSdk()
        uut.stopSdk()
        
        verify(listener).onStopped()
    }
    
    @Test
    fun sessionChanging() {
        val s1 = uut.newSession().sessionId
        assertThat(s1).isNotEmpty()
        
        val s2 = uut.newSession().sessionId
        assertThat(s2).isNotEmpty()
        assertThat(s2).isNotEqualTo(s1)
    }
    
    @Test
    fun sessionNotifications() {
        with(mock<EventListener>()) {
            uut.register(this)
            uut.newSession()
            uut.newSession()
            
            verify(this, times(2)).onNewSession()
            
            uut.unregister(this)
            uut.newSession()
            
            verifyNoMoreInteractions(this)
        }
    }
    
    @Test
    fun forgetMeStopsSdk() {
        uut.startSdk()
        assertThat(uut.isStarted).isTrue()
        
        uut.forgetMe()
        assertThat(uut.isStarted).isFalse()
    }
    
    private fun createUut() = DDNAImpl(
            RuntimeEnvironment.application,
            "environmentKey",
            "collectUrl",
            "engageUrl",
            Settings(),
            null,
            null,
            null,
            null)
}
