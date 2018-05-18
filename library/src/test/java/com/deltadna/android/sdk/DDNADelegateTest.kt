/*
 * Copyright (c) 2018 deltaDNA Ltd. All rights reserved.
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

import android.os.Bundle
import com.deltadna.android.sdk.listeners.EngageListener
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DDNADelegateTest {
    
    private val application by lazy { RuntimeEnvironment.application }
    private val preferences by lazy { Preferences(application) }
    
    private lateinit var tracking: DDNA
    private lateinit var nonTracking: DDNA
    
    private lateinit var uut: DDNADelegate
    
    @Before
    fun before() {
        tracking = mock()
        nonTracking = mock()
        
        uut = DDNADelegate(
                DDNA.Configuration(
                        application,
                        "environmentKey",
                        "collectUrl",
                        "engageUrl"),
                tracking,
                nonTracking)
    }
    
    @After
    fun after() {
        preferences.clear()
    }
    
    @Test
    fun forwardsToTrackingInstance() {
        uut.startSdk()
        verify(tracking).startSdk()
        
        uut.stopSdk()
        verify(tracking).stopSdk()
        
        uut.isStarted
        verify(tracking).isStarted
        
        with("event") {
            uut.recordEvent(this)
            verify(tracking).recordEvent(same(this))
        }
        
        with(KEvent("event")) {
            uut.recordEvent(this)
            verify(tracking).recordEvent(same(this))
        }
        
        uut.recordNotificationOpened(true, Bundle.EMPTY)
        verify(tracking).recordNotificationOpened(eq(true), same(Bundle.EMPTY))
        
        uut.recordNotificationDismissed(Bundle.EMPTY)
        verify(tracking).recordNotificationDismissed(same(Bundle.EMPTY))
        
        with(Pair("point", mock<EngageListener<Engagement<*>>>())) {
            uut.requestEngagement(first, second)
            verify(tracking).requestEngagement(same(first), same(second))
        }
        
        with(Pair(KEngagement("point"), mock<EngageListener<Engagement<*>>>())) {
            uut.requestEngagement(first, second)
            verify(tracking).requestEngagement(same(first), same(second))
        }
        
        uut.upload()
        verify(tracking).upload()
        
        uut.registrationId
        verify(tracking).registrationId
        
        with("id") {
            uut.registrationId = this
            verify(tracking).setRegistrationId(same(this))
        }
        
        uut.clearRegistrationId()
        verify(tracking).clearRegistrationId()
        
        uut.engageStoragePath
        verify(tracking).engageStoragePath
        
        uut.iso4217
        verify(tracking).iso4217
        
        verifyZeroInteractions(nonTracking)
    }
    
    @Test
    fun forwardsToNonTrackingInstance() {
        preferences.isForgetMe = true
        
        uut.startSdk()
        verify(nonTracking).startSdk()
        
        uut.stopSdk()
        verify(nonTracking).stopSdk()
        
        uut.isStarted
        verify(nonTracking).isStarted
        
        with("event") {
            uut.recordEvent(this)
            verify(nonTracking).recordEvent(same(this))
        }
        
        with(KEvent("event")) {
            uut.recordEvent(this)
            verify(nonTracking).recordEvent(same(this))
        }
        
        uut.recordNotificationOpened(true, Bundle.EMPTY)
        verify(nonTracking).recordNotificationOpened(eq(true), same(Bundle.EMPTY))
        
        uut.recordNotificationDismissed(Bundle.EMPTY)
        verify(nonTracking).recordNotificationDismissed(same(Bundle.EMPTY))
        
        with(Pair("point", mock<EngageListener<Engagement<*>>>())) {
            uut.requestEngagement(first, second)
            verify(nonTracking).requestEngagement(same(first), same(second))
        }
        
        with(Pair(KEngagement("point"), mock<EngageListener<Engagement<*>>>())) {
            uut.requestEngagement(first, second)
            verify(nonTracking).requestEngagement(same(first), same(second))
        }
        
        uut.upload()
        verify(nonTracking).upload()
        
        uut.registrationId
        verify(nonTracking).registrationId
        
        with("id") {
            uut.registrationId = this
            verify(nonTracking).setRegistrationId(same(this))
        }
        
        uut.clearRegistrationId()
        verify(nonTracking).clearRegistrationId()
        
        uut.engageStoragePath
        verify(nonTracking).engageStoragePath
        
        uut.iso4217
        verify(nonTracking).iso4217
        
        verifyZeroInteractions(tracking)
    }
    
    @Test
    fun startSdkWithUserId() {
        uut.startSdk(null)
        verify(tracking).startSdk(isNull())
        
        with("userId") {
            preferences.userId = this
            
            uut.startSdk(this)
            verify(tracking).startSdk(same(this))
        }
        
        preferences.isForgetMe = true
        preferences.isForgotten = true
        preferences.userId = "userId"
        
        uut.startSdk(null)
        assertThat(preferences.isForgetMe).isTrue()
        assertThat(preferences.isForgotten).isTrue()
        verify(nonTracking).startSdk(isNull())
        
        with("newUserId") {
            uut.startSdk(this)
            verify(tracking).startSdk(same(this))
            assertThat(preferences.isForgetMe).isFalse()
            assertThat(preferences.isForgotten).isFalse()
        }
    }
    
    @Test
    fun clearPersistentData() {
        whenever(tracking.clearPersistentData()).thenReturn(tracking)
        whenever(nonTracking.clearPersistentData()).thenReturn(nonTracking)
        
        assertThat(uut.clearPersistentData()).isSameAs(tracking)
        verify(tracking).clearPersistentData()
        verifyZeroInteractions(nonTracking)
        
        preferences.isForgetMe = true
        preferences.isForgotten = true
        
        assertThat(uut.clearPersistentData()).isSameAs(tracking)
        verify(nonTracking).clearPersistentData()
        verify(tracking, times(2)).clearPersistentData()
    }
    
    @Test
    fun forgetMe() {
        uut.forgetMe()
        inOrder(tracking, nonTracking) {
            verify(tracking).forgetMe()
            verify(nonTracking).forgetMe()
        }
        
        preferences.isForgetMe = true
        
        assertThat(uut.forgetMe()).isSameAs(nonTracking)
        verifyNoMoreInteractions(tracking, nonTracking)
    }
    
    private class KEvent(name: String) : Event<KEvent>(name)
    private class KEngagement(point: String) : Engagement<KEngagement>(point)
}
