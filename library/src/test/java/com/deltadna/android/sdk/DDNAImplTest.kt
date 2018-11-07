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

import com.deltadna.android.sdk.exceptions.NotStartedException
import com.deltadna.android.sdk.exceptions.SessionConfigurationException
import com.deltadna.android.sdk.helpers.Settings
import com.deltadna.android.sdk.listeners.EngageListener
import com.deltadna.android.sdk.listeners.EventListener
import com.deltadna.android.sdk.listeners.internal.IEventListener
import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.minus
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.*
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowLog
import java.util.*
import java.util.concurrent.TimeUnit
import com.deltadna.android.sdk.EventActionHandler.GameParametersHandler as GPH

@RunWith(RobolectricTestRunner::class)
class DDNAImplTest {
    
    private lateinit var server: MockWebServer
    
    private lateinit var uut: DDNAImpl
    
    @Before
    fun before() {
        ShadowLog.stream = System.out
        
        server = MockWebServer()
        server.start()
        
        uut = DDNAImpl(
                RuntimeEnvironment.application,
                "environmentKey",
                server.url("/collect").toString(),
                server.url("/engage").toString(),
                Settings(),
                null,
                null,
                null,
                null,
                mutableSetOf(),
                mutableSetOf())
    }
    
    @After
    fun after() {
        server.shutdown()
    }
    
    @Test
    fun `starting sdk sends default events`() {
        uut.startSdk()
        
        // session config
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest()
        
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest().run {
            assertThat(path).startsWith("/collect")
            with(body.readUtf8()) {
                assertThat(this).contains("\"eventName\":\"newPlayer\"")
                assertThat(this).contains("\"eventName\":\"gameStarted\"")
                assertThat(this).contains("\"eventName\":\"clientDevice\"")
            }
        }
    }
    
    @Test
    fun `default events are not sent when disabled`() {
        uut.settings.setOnFirstRunSendNewPlayerEvent(false)
        uut.settings.setOnInitSendClientDeviceEvent(false)
        uut.settings.setOnInitSendGameStartedEvent(false)
        uut.startSdk()
        
        // session config
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        assertThat(server.takeRequest().path).startsWith("/engage")
        
        assertThat(server.takeRequest(500, TimeUnit.MILLISECONDS)).isNull()
    }
    
    @Test
    fun `starting sdk with new user clears engage and action store`() {
        uut.settings.setBackgroundEventUpload(false)
        uut.userId = "id1"
        val database = DatabaseHelper(RuntimeEnvironment.application)
        database.insertEngagementRow("dp", "flavour", Date(), byteArrayOf())
        database.insertActionRow("name", 1L, Date(), JSONObject())
        
        uut.startSdk("id2")
        
        with(database.getEngagement("dp", "flavour")) {
            assertThat(this.count).isEqualTo(0)
        }
        assertThat(database.getAction(1L)).isNull()
        
        // session config
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest()
    }
    
    @Test
    fun `starting sdk notifies listener`() {
        uut.settings.setBackgroundEventUpload(false)
        
        with(mock<IEventListener>()) {
            uut.register(this)
            uut.startSdk()
            
            inOrder(this) {
                verify(this@with).onNewSession()
                verify(this@with).onStarted()
            }
        }
        
        // session config
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest()
    }
    
    @Test
    fun `stopping sdk sends event`() {
        uut.settings.setBackgroundEventUpload(false)
        
        uut.startSdk()
        uut.stopSdk()
        
        // session config
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest()
        
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest().run {
            assertThat(path).startsWith("/collect")
            assertThat(body.readUtf8()).contains("\"eventName\":\"gameEnded\"")
        }
    }
    
    @Test
    fun `stopping sdk notifies listener`() {
        uut.settings.setBackgroundEventUpload(false)
        
        with(mock<IEventListener>()) {
            uut.register(this)
            uut.startSdk()
            uut.stopSdk()
            
            verify(this).onStopped()
        }
        
        // session config
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest()
        // collect
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest()
    }
    
    @Test
    fun `request engagement`() {
        uut.settings.setBackgroundEventUpload(false)
        uut.startSdk()
        // session config
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest()
        runTasks()
        // engage response
        server.enqueue(MockResponse().setResponseCode(200).setBody(
                jsonObject("key" to "value").toString()))
        
        with(mock<EngageListener<Engagement<*>>>()) {
            uut.requestEngagement("point", this)
            
            with(server.takeRequest()) {
                assertThat(path).startsWith("/engage")
                assertThat(body.readUtf8()).contains("\"decisionPoint\":\"point\"")
            }
            waitAndRunTasks()
            verify(this).onCompleted(argThat {
                getDecisionPoint() == "point" &&
                getStatusCode() == 200 &&
                isSuccessful() &&
                !isCached() &&
                getJson()!!.toString() == jsonObject("key" to "value").toString() &&
                getError() == null
            })
        }
    }
    
    @Test
    fun `request engagement not successful`() {
        uut.settings.setBackgroundEventUpload(false)
        uut.startSdk()
        // session config
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest()
        runTasks()
        // engage response
        server.enqueue(MockResponse().setResponseCode(500).setBody("error"))
        
        with(mock<EngageListener<Engagement<*>>>()) {
            uut.requestEngagement("point", this)
            
            with(server.takeRequest()) {
                assertThat(path).startsWith("/engage")
                assertThat(body.readUtf8()).contains("\"decisionPoint\":\"point\"")
            }
            waitAndRunTasks()
            verify(this).onCompleted(argThat {
                getDecisionPoint() == "point" &&
                getStatusCode() == 500 &&
                !isSuccessful() &&
                !isCached() &&
                getJson() == null &&
                getError() == "error"
            })
        }
    }
    
    @Test
    fun `request engagement fails when sdk not started`() {
        with(mock<EngageListener<Engagement<*>>>()) {
            uut.requestEngagement("point", this)
            
            assertThat(server.takeRequest(500, TimeUnit.MILLISECONDS)).isNull()
            verify(this).onError(isA<NotStartedException>())
        }
    }
    
    @Test
    fun `event whitelisting`() {
        // session config
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(200))
        uut.settings.setBackgroundEventUpload(false)
        uut.startSdk()
        server.takeRequest()
        
        uut.recordEvent("a")
        uut.upload()
        
        server.takeRequest().run {
            assertThat(path).startsWith("/collect")
            assertThat(body.readUtf8()).contains("\"eventName\":\"a\"")
        }
        
        // apply new session config
        server.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody(jsonObject("parameters" to jsonObject(
                        "eventsWhitelist" to jsonArray("b")))
                        .toString()))
        uut.newSession()
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(200))
        waitAndRunTasks(iterations = 2)
        
        uut.recordEvent("a")
        uut.recordEvent("b")
        uut.upload()
        
        server.takeRequest().run {
            assertThat(path).startsWith("/collect")
            with(body.readUtf8()) {
                assertThat(this).doesNotContain("\"eventName\":\"a\"")
                assertThat(this).contains("\"eventName\":\"b\"")
            }
        }
    }
    
    @Test
    fun `decision point whitelisting`() {
        val listenerA = mock<EngageListener<Engagement<*>>>()
        val listenerB = mock<EngageListener<Engagement<*>>>()
        
        // session config
        uut.settings.setBackgroundEventUpload(false)
        uut.startSdk()
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest()
        runTasks()
        // engagement response
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        
        uut.requestEngagement("a", listenerA)
        
        with(server.takeRequest()) {
            assertThat(path).startsWith("/engage")
            assertThat(body.readUtf8()).contains("\"decisionPoint\":\"a\"")
        }
        waitAndRunTasks()
        verify(listenerA).onCompleted(any())
        
        // apply new session config
        server.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody(jsonObject("parameters" to jsonObject(
                        "dpWhitelist" to jsonArray("b@engagement")))
                        .toString()))
        uut.newSession()
        server.takeRequest()
        waitAndRunTasks()
        // engagement response
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        
        uut.requestEngagement("a", listenerA)
        uut.requestEngagement("b", listenerB)
        
        with(server.takeRequest()) {
            assertThat(path).startsWith("/engage")
            assertThat(body.readUtf8()).contains("\"decisionPoint\":\"b\"")
        }
        assertThat(server.takeRequest(500, TimeUnit.MILLISECONDS)).isNull()
        runTasks()
        verify(listenerA, times(2)).onCompleted(argThat {
            getDecisionPoint() == "a" &&
            getStatusCode() == 200 &&
            isSuccessful() &&
            !isCached() &&
            getJson()!!.length() == 0 &&
            getError() == null
        })
        verify(listenerB).onCompleted(any())
    }
    
    /**
     * First request as part of a new session fails, the second request succeeds,
     * and the third request fails but comes back the the previously cached
     * configuration.
     */
    @Test
    fun `session configuration requests`() {
        uut.settings.setBackgroundEventUpload(false)
        val config = jsonObject("parameters" to jsonObject(
                "eventsWhitelist" to jsonArray(),
                "dpWhitelist" to jsonArray(),
                "triggers" to jsonArray(),
                "imageCache" to jsonArray()))
                .toString()
        val l1 = mock<IEventListener>()
        val l2 = mock<EventListener>()
        
        uut.register(l1)
        uut.register(l2)
        server.enqueue(MockResponse()
                .setResponseCode(500)
                .setBody("error"))
        server.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody(config))
        server.enqueue(MockResponse()
                .setResponseCode(500)
                .setBody("error"))
        
        uut.startSdk()
        
        server.takeRequest().run {
            assertThat(path).startsWith("/engage")
            with(body.readUtf8()) {
                assertThat(this).contains("\"decisionPoint\":\"config\"")
                assertThat(this).contains("\"flavour\":\"internal\"")
                assertThat(this).contains("\"timeSinceFirstSession\":0")
                assertThat(this).contains("\"timeSinceLastSession\":0")
            }

            waitAndRunTasks()
        }
        verify(l2).onSessionConfigurationFailed(
                isA<SessionConfigurationException>())
        verify(l1, never()).onSessionConfigured(any(), any())
        verify(l2, never()).onImageCachePopulated()
        verify(l2, never()).onImageCachingFailed(any())
        
        uut.requestSessionConfiguration()
        
        server.takeRequest().run {
            assertThat(path).startsWith("/engage")
            with(body.readUtf8()) {
                assertThat(this).contains("\"decisionPoint\":\"config\"")
                assertThat(this).contains("\"flavour\":\"internal\"")
                assertThat(this).contains("\"timeSinceFirstSession\":")
                assertThat(this).contains("\"timeSinceLastSession\":")
            }
            
            waitAndRunTasks()
        }
        verify(l1).onSessionConfigured(eq(false), argThat { toString() == config })
        verify(l2).onSessionConfigured(eq(false))
        verify(l2).onImageCachePopulated()
        
        uut.requestSessionConfiguration()
        
        server.takeRequest().run {
            assertThat(path).startsWith("/engage")
            with(body.readUtf8()) {
                assertThat(this).contains("\"decisionPoint\":\"config\"")
                assertThat(this).contains("\"flavour\":\"internal\"")
                assertThat(this).contains("\"timeSinceFirstSession\":")
                assertThat(this).contains("\"timeSinceLastSession\":")
            }
            
            waitAndRunTasks()
        }
        verify(l1).onSessionConfigured(
                eq(true),
                argThat { toString() == "${config.dropLast(1)},\"isCachedResponse\":true}"})
        verify(l2).onSessionConfigured(eq(true))
        verify(l2, times(2)).onImageCachePopulated()
    }
    
    /**
     * Downloads image assets at first as part of a new session, failing on the
     * second file it tries. On the second try we succeed to download the second
     * file, and on the third try we will have both of the cached.
     */
    @Test
    fun `image asset caching`() {
        uut.settings.setBackgroundEventUpload(false)
        
        val images = mutableListOf("/1.png", "/2.png")
        val listener = mock<EventListener>()
        uut.register(listener)
        
        // will be downloaded as part of a new session
        server.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody(jsonObject("parameters" to jsonObject(
                        "dpWhitelist" to jsonArray(),
                        "eventsWhitelist" to jsonArray(),
                        "imageCache" to jsonArray(*images
                                .map { server.url(it).toString() }
                                .toTypedArray())))
                        .toString()))
        server.enqueue(MockResponse().setResponseCode(200).setBody("a"))
        server.enqueue(MockResponse().setResponseCode(500).setBody("error"))
        
        uut.startSdk()
        
        server.takeRequest().run {
            assertThat(path).startsWith("/engage")
            with(body.readUtf8()) {
                assertThat(this).contains("\"decisionPoint\":\"config\"")
                assertThat(this).contains("\"flavour\":\"internal\"")
            }

            runTasks()
        }
        with(server.takeRequest().path) {
            assertThat(this).isIn(images)
            images.remove(this)
        }
        assertThat(server.takeRequest().path).isIn(images)
        waitAndRunTasks(iterations = 2)
        verify(listener).onImageCachingFailed(any())
        
        server.enqueue(MockResponse().setResponseCode(200).setBody("b"))
        
        uut.downloadImageAssets()
        
        assertThat(server.takeRequest().path).isIn(images)
        assertThat(server.requestCount).isEqualTo(4)
        waitAndRunTasks(iterations = 2)
        verify(listener).onImageCachePopulated()
        
        uut.downloadImageAssets()
        
        // images will be cached now so not expecting any network requests
        assertThat(server.requestCount).isEqualTo(4)
        waitAndRunTasks()
        verify(listener, times(2)).onImageCachePopulated()
    }
    
    @Test
    fun `event triggers are read out from configuration and evaluated`() {
        uut.settings.setBackgroundEventUpload(false)
        
        uut.startSdk()
        server.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody(jsonObject("parameters" to jsonObject(
                        "eventsWhitelist" to jsonArray("a"),
                        "triggers" to jsonArray(
                                jsonObject(
                                        "eventName" to "b",
                                        "condition" to jsonArray(
                                                jsonObject("p" to "c"),
                                                jsonObject("i" to 1),
                                                jsonObject("o" to "equal to")),
                                        "response" to jsonObject(
                                                "parameters" to jsonObject("e" to 3))),
                                jsonObject(
                                        "eventName" to "a",
                                        "condition" to jsonArray(
                                                jsonObject("p" to "d"),
                                                jsonObject("i" to 1),
                                                jsonObject("o" to "equal to")),
                                        "response" to jsonObject(
                                                "parameters" to jsonObject("e" to 4))),
                                jsonObject(
                                        "eventName" to "a",
                                        "condition" to jsonArray(
                                                jsonObject("p" to "c"),
                                                jsonObject("i" to 2),
                                                jsonObject("o" to "equal to")),
                                        "response" to jsonObject(
                                                "parameters" to jsonObject("e" to 5))))))
                        .toString()))
        server.takeRequest()
        waitAndRunTasks()
        
        with(mock<EventActionHandler.Callback<JSONObject>>()) {
            uut.recordEvent(KEvent("a").putParam("c", 2)).add(GPH(this)).run()
            
            verify(this).handle(argThat {
                toString() == jsonObject("e" to 5).toString()
            })
        }
    }
    
    @Test
    fun `persistent actions are persisted from session config`() {
        val database = DatabaseHelper(RuntimeEnvironment.application)
        val params = jsonObject("ddnaIsPersistent" to true, "a" to 1)
        server.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody(jsonObject("parameters" to jsonObject(
                        "triggers" to jsonArray(
                                jsonObject(
                                        "eventName" to "name",
                                        "condition" to jsonArray(),
                                        "response" to jsonObject("parameters" to params),
                                        "campaignID" to 1L),
                                jsonObject(
                                        "eventName" to "name",
                                        "condition" to jsonArray(),
                                        "response" to jsonObject("parameters" to params
                                                .minus("ddnaIsPersistent")),
                                        "campaignID" to 2L))))
                        .toString()))
        uut.settings.setBackgroundEventUpload(false)
        
        uut.startSdk()
        server.takeRequest()
        waitAndRunTasks()
        
        assertThat("${database.getAction(1L)}").isEqualTo("$params")
        assertThat(database.getAction(2L)).isNull()
    }
    
    @Test
    fun `cross game user id is set and recorded`() {
        uut.settings.setBackgroundEventUpload(false)
        uut.startSdk()
        
        // session config
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest()
        
        assertThat(uut.crossGameUserId).isNull()
        
        // collect
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        
        uut.crossGameUserId = "id"
        uut.upload()
        
        assertThat(uut.crossGameUserId).isEqualTo("id")
        server.takeRequest().run {
            assertThat(path).startsWith("/collect")
            with(body.readUtf8()) {
                assertThat(this).contains("\"eventName\":\"ddnaRegisterCrossGameUserID\"")
                assertThat(this).contains("\"ddnaCrossGameUserID\":\"id\"")
            }
        }
    }
    
    @Test
    fun `cross game user id is sent on game started event`() {
        uut.settings.setOnFirstRunSendNewPlayerEvent(false)
        uut.settings.setOnInitSendClientDeviceEvent(false)
        uut.preferences.crossGameUserId = "id"
        uut.startSdk()
        uut.upload()
        
        // session config
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest()
        
        // collect
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest().run {
            assertThat(path).startsWith("/collect")
            with(body.readUtf8()) {
                assertThat(this).contains("\"eventName\":\"gameStarted\"")
                assertThat(this).contains("\"ddnaCrossGameUserID\":\"id\"")
            }
        }
    }
    
    @Test
    fun `cross game user id cannot be null or empty`() {
        uut.crossGameUserId = "id"
        
        uut.crossGameUserId = null as String? // workaround for compiler ambiguity
        assertThat(uut.crossGameUserId).isNotNull()
        
        uut.crossGameUserId = ""
        assertThat(uut.crossGameUserId).isNotEqualTo("")
    }
    
    @Test
    fun `persistent data is cleared`() {
        val database = DatabaseHelper(RuntimeEnvironment.application).apply {
            insertEventRow(1L, Location.INTERNAL, "name", null, 2L)
            insertEngagementRow("dp", "flavour", Date(), byteArrayOf())
            insertActionRow("name", 1L, Date(), JSONObject())
            insertImageMessage("url", Location.INTERNAL, "name", 1L, Date())
        }
        uut.settings.setBackgroundEventUpload(false)
        uut.startSdk()
        
        uut.clearPersistentData()
        
        assertThat(uut.isStarted).isFalse()
        assertThat(uut.preferences.prefs.all).isEmpty()
        assertThat(database.eventsSize).isEqualTo(0)
        assertThat(database.getEngagement("dp", "flavour").count).isEqualTo(0)
        assertThat(database.getAction(1L)).isNull()
        assertThat(database.imageMessages.count).isEqualTo(0)
        
        // session config
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest()
    }
    
    @Test
    fun `forget me stops sdk`() {
        uut.settings.setBackgroundEventUpload(false)
        
        uut.startSdk()
        assertThat(uut.isStarted).isTrue()
        
        // session config
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest()
        
        uut.forgetMe()
        assertThat(uut.isStarted).isFalse()
        
        // collect
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.takeRequest()
    }
}
