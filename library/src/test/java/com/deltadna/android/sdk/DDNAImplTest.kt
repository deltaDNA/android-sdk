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
import com.deltadna.android.sdk.test.runTasks
import com.deltadna.android.sdk.test.waitAndRunTasks
import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.*
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowLog
import java.io.IOException
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class DDNAImplTest {
    
    private lateinit var server: MockWebServer
    
    private lateinit var uut: DDNAImpl
    
    @Before
    fun before() {
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
                null)
    }
    
    @After
    fun after() {
        try {
            server.shutdown()
        } catch (_: IOException) {}
    }
    
    @Test
    fun `starting sdk sends default events`() {
        uut.startSdk()
        
        server.takeRequest() // session config
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
        
        server.takeRequest() // session config
        assertThat(server.takeRequest(500, TimeUnit.MILLISECONDS)).isNull()
    }
    
    @Test
    fun `starting sdk notifies listener`() {
        with(mock<EventListener>()) {
            uut.register(this)
            uut.startSdk()
            
            server.takeRequest() // session config
            server.takeRequest() // collect
            inOrder(this) {
                verify(this@with).onNewSession()
                verify(this@with).onStarted()
            }
        }
    }
    
    @Test
    fun `stopping sdk sends event`() {
        uut.startSdk()
        uut.stopSdk()
        
        server.takeRequest() // session config
        server.takeRequest().run {
            assertThat(path).startsWith("/collect")
            assertThat(body.readUtf8()).contains("\"eventName\":\"gameEnded\"")
        }
    }
    
    @Test
    fun `stopping sdk notifies listener`() {
        with(mock<EventListener>()) {
            uut.register(this)
            uut.startSdk()
            uut.stopSdk()
            
            server.takeRequest() // session config
            server.takeRequest() // collect
            verify(this).onStopped()
        }
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
        ShadowLog.stream = System.out
        
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
        
        with(mock<EventListener>()) {
            uut.register(this)
            server.enqueue(MockResponse()
                    .setResponseCode(500)
                    .setBody("error"))
            server.enqueue(MockResponse()
                    .setResponseCode(200)
                    .setBody(jsonObject("parameters" to jsonObject(
                            "dpWhitelist" to jsonArray(),
                            "eventsWhitelist" to jsonArray(),
                            "imageCache" to jsonArray()))
                            .toString()))
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
            verify(this).onSessionConfigurationFailed(
                    isA<SessionConfigurationException>())
            verify(this, never()).onImageCachePopulated()
            verify(this, never()).onImageCachingFailed(any())
            
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
            verify(this).onSessionConfigured(eq(false))
            verify(this).onImageCachePopulated()
            
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
            verify(this).onSessionConfigured(eq(true))
            verify(this, times(2)).onImageCachePopulated()
        }
    }
    
    /**
     * Downloads image assets at first as part of a new session, failing on the
     * second file it tries. On the second try we succeed to download the second
     * file, and on the third try we will have both of the cached.
     */
    @Test
    fun `image asset caching`() {
        val images = mutableListOf("/1.png", "/2.png")
        val listener = mock<EventListener>()
        uut.settings.setBackgroundEventUpload(false)
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
    fun `forget me stops sdk`() {
        uut.startSdk()
        assertThat(uut.isStarted).isTrue()
        
        uut.forgetMe()
        assertThat(uut.isStarted).isFalse()
    }
}
