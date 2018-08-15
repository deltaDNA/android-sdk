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
import com.deltadna.android.sdk.helpers.Settings
import com.deltadna.android.sdk.listeners.EngageListener
import com.deltadna.android.sdk.listeners.EventListener
import com.deltadna.android.sdk.listeners.internal.IEventListener
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.*
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class DDNANonTrackingTest {
    
    private val application by lazy { RuntimeEnvironment.application }
    private val preferences by lazy { Preferences(application) }
    
    private lateinit var server: MockWebServer
    
    private lateinit var uut: DDNANonTracking
    
    @Before
    fun before() {
        server = MockWebServer()
        server.start()
        
        uut = DDNANonTracking(
                application,
                "environmentKey",
                server.url("/collect").toString(),
                server.url("/engage").toString(),
                Settings(),
                null,
                null,
                mutableSetOf(),
                mutableSetOf())
    }
    
    @After
    fun after() {
        server.shutdown()
        preferences.clear()
    }
    
    @Test
    fun `start sends forget me event when not forgotten`() {
        preferences.userId = "userId"
        preferences.advertisingId = "advertisingId"
        preferences.isForgetMe = true
        preferences.isForgotten = false
        
        // this is just to stop a shutdown() exception 
        server.enqueue(MockResponse().setResponseCode(200))
        uut.startSdk()
        
        with(server.takeRequest()) {
            assertThat(path).isEqualTo("/collect/environmentKey")
            with(JSONObject(body.readUtf8())) {
                assertThat(get("eventName")).isEqualTo("ddnaForgetMe")
                assertThat(has("eventTimestamp")).isTrue()
                assertThat(has("eventUUID")).isTrue()
                assertThat(get("sessionID")).isEqualTo(uut.getSessionId())
                assertThat(get("userID")).isEqualTo(preferences.userId)
                
                assertThat(has("eventParams")).isTrue()
                with(getJSONObject("eventParams")) {
                    assertThat(get("platform")).isEqualTo(uut.platform)
                    assertThat(get("sdkVersion")).isEqualTo(DDNA.SDK_VERSION)
                    assertThat(get("ddnaAdvertisingId")).isEqualTo(preferences.advertisingId)
                }
            }
            
           runTasks()
        }
    }
    
    @Test
    fun `start sends delayed forget me event when advertising id not set`() {
        preferences.userId = "userId"
        preferences.advertisingId = null
        preferences.isForgetMe = true
        preferences.isForgotten = false
        
        // this is just to stop a shutdown() exception 
        server.enqueue(MockResponse().setResponseCode(200))
        uut.startSdk()
        
        Robolectric.getForegroundThreadScheduler().advanceBy(5500, TimeUnit.SECONDS)
        
        with(server.takeRequest()) {
            assertThat(path).isEqualTo("/collect/environmentKey")
            with(JSONObject(body.readUtf8())) {
                assertThat(get("eventName")).isEqualTo("ddnaForgetMe")
                assertThat(has("eventTimestamp")).isTrue()
                assertThat(has("eventUUID")).isTrue()
                assertThat(get("sessionID")).isEqualTo(uut.getSessionId())
                assertThat(get("userID")).isEqualTo(preferences.userId)
                
                assertThat(has("eventParams")).isTrue()
                with(getJSONObject("eventParams")) {
                    assertThat(get("platform")).isEqualTo(uut.platform)
                    assertThat(get("sdkVersion")).isEqualTo(DDNA.SDK_VERSION)
                    assertThat(has("ddnaAdvertisingId")).isFalse()
                }
            }
            
            runTasks()
        }
    }
    
    @Test
    fun `start does not send forget me event when forgotten`() {
        preferences.isForgetMe = true
        preferences.isForgotten = true
        
        uut.startSdk()
        
        assertThat(Robolectric.getForegroundThreadScheduler().areAnyRunnable()).isFalse()
        assertThat(server.requestCount).isEqualTo(0)
    }
    
    @Test
    fun requestEngagement() {
        with(mock<EngageListener<Engagement<*>>>()) {
            uut.requestEngagement("point", this)
            verify(this).onCompleted(argThat {
                getDecisionPoint() == "point" &&
                isSuccessful() &&
                getError().isNullOrEmpty() &&
                getJson().toString() == "{}"
            })
        }
        
        with(Pair(KEngagement("point"), mock<EngageListener<KEngagement>>())) {
            uut.requestEngagement(first, second)
            verify(second).onCompleted(argThat {
                this === first &&
                isSuccessful &&
                error.isNullOrEmpty() &&
                json.toString() == "{}"
            })
        }
    }
    
    @Test
    fun requestSessionConfiguration() {
        val l1 = mock<IEventListener>()
        val l2 = mock<EventListener>()
        
        uut.register(l1)
        uut.register(l2)
        uut.requestSessionConfiguration()
        
        verify(l1).onSessionConfigured(eq(false), argThat { toString() == "{}" })
        verify(l2).onSessionConfigured(eq(false))
        verify(l2).onImageCachePopulated()
    }
    
    @Test
    fun upload() {
        uut.upload()
        
        assertThat(server.takeRequest(500, TimeUnit.MILLISECONDS)).isNull()
    }
    
    @Test
    fun downloadImageAssets() {
        with(mock<EventListener>()) {
            uut.register(this)
            uut.downloadImageAssets()
            
            verify(this).onImageCachePopulated()
            verifyNoMoreInteractions(this)
        }
        
        assertThat(server.takeRequest(500, TimeUnit.MILLISECONDS)).isNull()
    }
    
    @Test
    fun `forget me event is sent correctly`() {
        preferences.userId = "userId"
        preferences.advertisingId = "advertisingId"
        
        server.enqueue(MockResponse().setResponseCode(500))
        uut.forgetMe()
        
        assertThat(preferences.isForgetMe).isTrue()
        assertThat(preferences.isForgotten).isFalse()
        
        with(server.takeRequest()) {
            assertThat(path).isEqualTo("/collect/environmentKey")
            with(JSONObject(body.readUtf8())) {
                assertThat(get("eventName")).isEqualTo("ddnaForgetMe")
                assertThat(has("eventTimestamp")).isTrue()
                assertThat(has("eventUUID")).isTrue()
                assertThat(get("sessionID")).isEqualTo(uut.getSessionId())
                assertThat(get("userID")).isEqualTo(preferences.userId)
                
                assertThat(has("eventParams")).isTrue()
                with(getJSONObject("eventParams")) {
                    assertThat(get("platform")).isEqualTo(uut.platform)
                    assertThat(get("sdkVersion")).isEqualTo(DDNA.SDK_VERSION)
                    assertThat(get("ddnaAdvertisingId")).isEqualTo(preferences.advertisingId)
                }
            }
            
            runTasks()
        }
        assertThat(preferences.isForgetMe).isTrue()
        assertThat(preferences.isForgotten).isFalse()
        
        server.enqueue(MockResponse().setResponseCode(200))
        uut.forgetMe()
        
        with(server.takeRequest()) {
            assertThat(path).isEqualTo("/collect/environmentKey")
            with(JSONObject(body.readUtf8())) {
                assertThat(get("eventName")).isEqualTo("ddnaForgetMe")
                assertThat(has("eventTimestamp")).isTrue()
                assertThat(has("eventUUID")).isTrue()
                assertThat(get("sessionID")).isEqualTo(uut.getSessionId())
                assertThat(get("userID")).isEqualTo(preferences.userId)
                
                assertThat(has("eventParams")).isTrue()
                with(getJSONObject("eventParams")) {
                    assertThat(get("platform")).isEqualTo(uut.platform)
                    assertThat(get("sdkVersion")).isEqualTo(DDNA.SDK_VERSION)
                    assertThat(get("ddnaAdvertisingId")).isEqualTo(preferences.advertisingId)
                }
            }
            
            waitAndRunTasks()
        }
        assertThat(preferences.isForgetMe).isTrue()
        assertThat(preferences.isForgotten).isTrue()
        
        uut.forgetMe()
        
        assertThat(server.requestCount).isEqualTo(2)
    }
    
    @Test
    fun eventsNotRecorded() {
        uut.recordEvent("name")
        uut.recordEvent(KEvent("name"))
        uut.recordNotificationOpened(true, Bundle.EMPTY)
        uut.recordNotificationDismissed(Bundle.EMPTY)
        uut.upload()
        
        assertThat(server.requestCount).isEqualTo(0)
    }
    
    private class KEngagement(point: String) : Engagement<KEngagement>(point)
}
