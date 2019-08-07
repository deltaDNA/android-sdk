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

import android.app.Application
import android.os.Bundle
import com.deltadna.android.sdk.helpers.ClientInfo
import com.deltadna.android.sdk.helpers.Settings
import com.deltadna.android.sdk.listeners.EngageListener
import com.deltadna.android.sdk.listeners.internal.IEventListener
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.*

@RunWith(RobolectricTestRunner::class)
class DDNATest {
    
    private val application by lazy { RuntimeEnvironment.application }
    
    private lateinit var listener: DDNA
    
    private lateinit var uut: DDNA
    
    @Before
    fun before() {
        listener = mock()
        
        uut = DDNAI(listener, application)
    }
    
    @Test
    fun `user change returned when user id set`() {
        assertThat(uut.setUserId("a")).isFalse()
        assertThat(uut.setUserId("a")).isFalse()
        assertThat(uut.setUserId("b")).isTrue()
    }
    
    @Test
    fun `user id is persisted to preferences`() {
        val preferences = Preferences(application)
        
        uut.userId = "a"
        assertThat(preferences.userId).isEqualTo("a")
        
        uut.userId = "b"
        assertThat(preferences.userId).isEqualTo("b")
    }
    
    @Test
    fun `preference keys are cleared when user id changes`() {
        val preferences = Preferences(application)
        
        uut.userId = "a"
        preferences.firstRun = 0
        preferences.firstSession = Date(1)
        preferences.lastSession = Date(2)
        
        uut.userId = "b"
        
        assertThat(preferences.firstRun).isEqualTo(1)
        assertThat(preferences.firstSession).isNotEqualTo(Date(1))
        assertThat(preferences.lastSession).isNotEqualTo(Date(2))
    }
    
    @Test
    fun `cross game user id is cleared when user id changes`() {
        uut.crossGameUserId = "a"
        uut.userId = "a"
        
        assertThat(uut.crossGameUserId).isNull()
        assertThat(Preferences(application).crossGameUserId).isNull()
    }
    
    @Test
    fun newSessionChangesSession() {
        val s1 = uut.newSession().sessionId
        assertThat(s1).isNotEmpty()
        
        val s2 = uut.newSession().sessionId
        assertThat(s2).isNotEmpty()
        assertThat(s2).isNotEqualTo(s1)
    }
    
    @Test
    fun newSessionRequestsConfiguration() {
        uut.newSession()
        verify(listener).requestSessionConfiguration()
    }
    
    @Test
    fun `new session updates preferences with session times`() {
        val preferences = Preferences(application)
        
        assertThat(preferences.firstSession).isNull()
        assertThat(preferences.lastSession).isNull()
        
        uut.newSession()
        
        assertThat(preferences.firstSession).isNotNull()
        assertThat(preferences.lastSession).isNotNull()
    }
    
    @Test
    fun newSessionNotifiesCallbacks() {
        with(mock<IEventListener>()) {
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
    fun `advertising id is persisted to preferences`() {
        with("id") {
            uut.setAdvertisingId(this)
            assertThat(uut.preferences.advertisingId).isEqualTo(this)
        }
    }
    
    @Test
    fun `platform is returned`() {
        assertThat(uut.getPlatform()).isEqualTo(uut.platform)
        assertThat(uut.getPlatform()).isEqualTo(ClientInfo.platform())
    }
    
    private class DDNAI(private val listener: DDNA, application: Application)
        : DDNA(
            application,
            "environmentKey",
            "http://host.net/collectUrl",
            "http://host.net/engageUrl",
            Settings(),
            null,
            null,
            mutableSetOf(),
            mutableSetOf()) {

        override fun startSdk(): DDNA {
            listener.startSdk()
            return this
        }
        
        override fun startSdk(userId: String?): DDNA {
            listener.startSdk(userId)
            return this
        }
        
        override fun stopSdk(): DDNA {
            listener.stopSdk()
            return this
        }
        
        override fun isStarted(): Boolean {
            return listener.isStarted
        }
        
        override fun recordEvent(name: String): EventAction {
            listener.recordEvent(name)
            return EventAction.EMPTY
        }
        
        override fun recordEvent(event: Event<out Event<*>>): EventAction {
            listener.recordEvent(event)
            return EventAction.EMPTY
        }
        
        override fun recordNotificationOpened(launch: Boolean, payload: Bundle?): EventAction {
            listener.recordNotificationOpened(launch, payload)
            return EventAction.EMPTY
        }
        
        override fun recordNotificationDismissed(payload: Bundle?): EventAction {
            listener.recordNotificationDismissed(payload)
            return EventAction.EMPTY
        }
        
        override fun requestEngagement(decisionPoint: String?, listener: EngageListener<Engagement<*>>?): DDNA {
            this.listener.requestEngagement(decisionPoint, listener)
            return this
        }
        
        override fun <E : Engagement<out Engagement<*>>?> requestEngagement(engagement: E, listener: EngageListener<E>?): DDNA {
            this.listener.requestEngagement(engagement, listener)
            return this
        }
        
        override fun requestSessionConfiguration(): DDNA {
            listener.requestSessionConfiguration()
            return this
        }
        
        override fun upload(): DDNA {
            listener.upload()
            return this
        }
        
        override fun downloadImageAssets(): DDNA {
            listener.downloadImageAssets()
            return this
        }
        
        override fun getCrossGameUserId(): String? {
            return listener.crossGameUserId
        }
        
        override fun setCrossGameUserId(crossGameUserId: String?): DDNA {
            listener.crossGameUserId = crossGameUserId
            return this
        }
        
        override fun getRegistrationId(): String? {
            return listener.registrationId
        }
        
        override fun setRegistrationId(registrationId: String?): DDNA {
            listener.registrationId = registrationId
            return this
        }
        
        override fun clearRegistrationId(): DDNA {
            listener.clearRegistrationId()
            return this
        }
        
        override fun clearPersistentData(): DDNA {
            listener.clearPersistentData()
            return this
        }
        
        override fun forgetMe(): DDNA {
            listener.forgetMe()
            return this
        }

        override fun stopTrackingMe(): DDNA {
            listener.stopTrackingMe()
            return this
        }
        
        override fun getImageMessageStore(): ImageMessageStore {
            return listener.imageMessageStore
        }
        
        override fun getIso4217(): MutableMap<String, Int> {
            return listener.iso4217
        }
    }
}
