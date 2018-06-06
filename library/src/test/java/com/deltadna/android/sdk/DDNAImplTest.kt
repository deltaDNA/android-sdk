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

import com.deltadna.android.sdk.exceptions.SessionConfigurationException
import com.deltadna.android.sdk.helpers.Settings
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
import java.io.IOException

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
    fun startSdkEvents() {
        val listener = mock<EventListener>()
        uut.register(listener)
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
        
        server.takeRequest() // session config
        server.takeRequest().run {
            assertThat(path).startsWith("/collect")
            assertThat(body.readUtf8()).contains("\"eventName\":\"gameEnded\"")
        }
        verify(listener).onStopped()
    }
    
    /**
     * First request as part of a new session fails, the second request succeeds,
     * and the third request fails but comes back the the previously cached
     * configuration.
     */
    @Test
    fun requestSessionConfiguration() {
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
                    assertThat(this).contains("\"timeSinceFirstSession\":")
                    assertThat(this).contains("\"timeSinceLastSession\":")
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
    fun downloadImageAssets() {
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
    fun forgetMeStopsSdk() {
        uut.startSdk()
        assertThat(uut.isStarted).isTrue()
        
        uut.forgetMe()
        assertThat(uut.isStarted).isFalse()
    }
}
