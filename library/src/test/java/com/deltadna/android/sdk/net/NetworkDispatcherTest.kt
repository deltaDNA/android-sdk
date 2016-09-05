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

package com.deltadna.android.sdk.net

import com.deltadna.android.sdk.listeners.RequestListener
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.*
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class NetworkDispatcherTest {
    
    private var uut: NetworkDispatcher? = null
    private var server: MockWebServer? = null
    
    @Before
    fun before() {
        uut = NetworkDispatcher()
        
        server = MockWebServer()
        server!!.start()
    }
    
    @After
    fun after() {
        server!!.shutdown()
        server = null
        
        uut = null
    }
    
    @Test
    fun successfulRequest() {
        val responseBody = "response"
        val listener = mock<RequestListener<String>>()
        
        server!!.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody(responseBody))
        
        uut!!.enqueue(
                Request.Builder<String>()
                        .get()
                        .url(server!!.url("/success").toString())
                        .build(),
                ResponseBodyConverter.STRING,
                listener)
        
        server!!.takeRequest()
        Thread.sleep(100)
        RuntimeEnvironment.getMasterScheduler().advanceToNextPostedRunnable()
        
        assertThat(server!!.requestCount).isEqualTo(1)
        verify(listener).onCompleted(eq(Response(
                200,
                responseBody.toByteArray(),
                responseBody,
                null)))
    }
    
    @Test
    fun failureRetriesRequest() {
        val responseBody = "not found"
        val listener = mock<RequestListener<Void>>()
        
        for (i in 0..MAX_RETRIES) {
            server!!.enqueue(MockResponse()
                    .setResponseCode(404)
                    .setBody(responseBody)
                    .setBodyDelay(150, TimeUnit.MILLISECONDS))
        }
        
        uut!!.enqueue(
                Request.Builder<Void>()
                        .get()
                        .url(server!!.url("/failure").toString())
                        .readTimeout(100)
                        .maxRetries(MAX_RETRIES)
                        .build(),
                listener)
        
        Thread.sleep(200L * MAX_RETRIES)
        for (i in 0..MAX_RETRIES) {
            server!!.takeRequest()
        }
        Thread.sleep(100)
        RuntimeEnvironment.getMasterScheduler().advanceToLastPostedRunnable()
        
        assertThat(server!!.requestCount).isEqualTo(MAX_RETRIES + 1)
        verify(listener).onError(isA<IOException>())
    }
    
    @Test
    fun retriesRespectDelay() {
        val listener = mock<RequestListener<Void>>()
        
        server!!.enqueue(MockResponse()
                .setResponseCode(404)
                .setBodyDelay(150, TimeUnit.MILLISECONDS))
        server!!.enqueue(MockResponse()
                .setResponseCode(200)
                .setBodyDelay(150, TimeUnit.MILLISECONDS))
        
        uut!!.enqueue(
                Request.Builder<Void>()
                        .get()
                        .url(server!!.url("/delay").toString())
                        .readTimeout(100)
                        .maxRetries(1)
                        .retryDelay(1000)
                        .build(),
                listener)
        
        server!!.takeRequest()
        val first = System.currentTimeMillis()
        server!!.takeRequest()
        val second = System.currentTimeMillis()
        Thread.sleep(100)
        RuntimeEnvironment.getMasterScheduler().advanceToLastPostedRunnable()
        
        assertThat(second - first).isGreaterThan(900L)
        assertThat(second - first).isAtMost(1100L)
        assertThat(server!!.requestCount).isEqualTo(2)
        verify(listener).onCompleted(any())
    }
    
    // FIXME test failing only on jenkins
    @Ignore
    @Test
    fun requestCancellation() {
        val listener = mock<RequestListener<Void>>()
        
        server!!.enqueue(MockResponse()
                .setResponseCode(200)
                .setBodyDelay(200, TimeUnit.MILLISECONDS))
        uut!!.enqueue(
                Request.Builder<Void>()
                        .get()
                        .url(server!!.url("/cancel").toString())
                        .build(),
                listener).cancel()
        server!!.takeRequest(100, TimeUnit.MILLISECONDS)
        Thread.sleep(500)
        
        assertThat(server!!.requestCount).isEqualTo(1)
        assertThat(RuntimeEnvironment.getMasterScheduler().areAnyRunnable())
                .isFalse()
        verifyZeroInteractions(listener)
    }
    
    companion object {
        
        private val MAX_RETRIES = 1
    }
}
