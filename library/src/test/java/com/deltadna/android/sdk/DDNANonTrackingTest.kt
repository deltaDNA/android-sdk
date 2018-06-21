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

import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import com.deltadna.android.sdk.helpers.Settings
import com.deltadna.android.sdk.listeners.EngageListener
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
import java.util.logging.Logger

@RunWith(RobolectricTestRunner::class)
class DDNANonTrackingTest {
    
    private val application by lazy { RuntimeEnvironment.application }
    private val preferences by lazy { Preferences(application) }
    private val broadcasts by lazy { LocalBroadcastManager.getInstance(application) }
    
    private lateinit var server: MockWebServer
    private lateinit var listener: BroadcastReceiver
    
    private lateinit var uut: DDNANonTracking
    
    @Before
    fun before() {
        server = MockWebServer()
        server.start()
        listener = mock()
        broadcasts.registerReceiver(listener, IntentFilter(Actions.FORGET_ME))
        
        uut = DDNANonTracking(
                application,
                "environmentKey",
                server.url("/collect").toString(),
                server.url("/engage").toString(),
                Settings(),
                null,
                null,
                mutableSetOf())
    }
    
    @After
    fun after() {
        broadcasts.unregisterReceiver(listener)
        server.shutdown()
        preferences.clear()
    }
    
    @Test
    fun startWhenNotForgotten() {
        preferences.userId = "userId"
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
                assertThat(has("eventUUID"))
                assertThat(get("sessionID")).isEqualTo(uut.getSessionId())
                assertThat(get("userID")).isEqualTo(preferences.userId)
                assertThat(has("eventParams"))
            }
            
            Robolectric.getForegroundThreadScheduler().runOneTask()
        }
    }
    
    @Test
    fun startWhenForgotten() {
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
    fun forgetMe() {
        preferences.userId = "userId"
        
        server.enqueue(MockResponse().setResponseCode(500))
        uut.forgetMe()
        
        verify(listener).onReceive(
                any(),
                argThat { action == Actions.FORGET_ME })
        assertThat(preferences.isForgetMe).isTrue()
        assertThat(preferences.isForgotten).isFalse()
        
        with(server.takeRequest()) {
            assertThat(path).isEqualTo("/collect/environmentKey")
            with(JSONObject(body.readUtf8())) {
                assertThat(get("eventName")).isEqualTo("ddnaForgetMe")
                assertThat(has("eventTimestamp")).isTrue()
                assertThat(has("eventUUID"))
                assertThat(get("sessionID")).isEqualTo(uut.getSessionId())
                assertThat(get("userID")).isEqualTo(preferences.userId)
                assertThat(has("eventParams"))
            }
            
            Robolectric.getForegroundThreadScheduler().runOneTask()
        }
        assertThat(preferences.isForgetMe).isTrue()
        assertThat(preferences.isForgotten).isFalse()
        
        server.enqueue(MockResponse().setResponseCode(200))
        uut.forgetMe()
        
        verify(listener, times(2)).onReceive(
                any(),
                argThat { action == Actions.FORGET_ME })
        with(server.takeRequest()) {
            assertThat(path).isEqualTo("/collect/environmentKey")
            with(JSONObject(body.readUtf8())) {
                assertThat(get("eventName")).isEqualTo("ddnaForgetMe")
                assertThat(has("eventTimestamp")).isTrue()
                assertThat(has("eventUUID"))
                assertThat(get("sessionID")).isEqualTo(uut.getSessionId())
                assertThat(get("userID")).isEqualTo(preferences.userId)
                assertThat(has("eventParams"))
            }
            
            Robolectric.getForegroundThreadScheduler().runOneTask()
            
            /*
             * Robolectric seems to have some issues with refreshing
             * SharedPreference values. For some reason the following two lines
             * seem to cause the value to get refreshed so that it's in the
             * correct state for the following assertions :/
             */
            Logger.getGlobal().info(preferences.isForgotten.toString())
            Robolectric.getForegroundThreadScheduler().runOneTask()
        }
        assertThat(preferences.isForgetMe).isTrue()
        assertThat(preferences.isForgotten).isTrue()
        
        uut.forgetMe()
        
        verifyNoMoreInteractions(listener)
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
    
    private class KEvent(name: String) : Event<KEvent>(name)
    private class KEngagement(point: String) : Engagement<KEngagement>(point)
}
