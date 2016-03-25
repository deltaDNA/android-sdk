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

import android.os.Handler
import com.deltadna.android.sdk.exceptions.ResponseException
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
import org.mockito.runners.MockitoJUnitRunner
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class NetworkDispatcherTest {
    
    private var uut: NetworkDispatcher? = null
    private var server: MockWebServer? = null
    
    @Before
    fun before() {
        val handler = mock<Handler>()
        whenever(handler.post(any())).then {
            (it.arguments[0] as Runnable).run()
        }
        
        uut = NetworkDispatcher(handler)
        
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
        val listener = mock<RequestListener<Response<String>>>()
        
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
        
        assertThat(server!!.requestCount).isEqualTo(1)
        verify(listener).onSuccess(
                eq(Response(200, responseBody.toByteArray(), responseBody)))
    }
    
    @Test
    fun failureRetriesRequest() {
        val responseBody = "not found"
        val listener = mock<RequestListener<Response<Void>>>()
        
        for (i in 0..1 + MAX_RETRIES - 1) {
            server!!.enqueue(MockResponse()
                    .setResponseCode(404)
                    .setBody(responseBody))
        }
        
        uut!!.enqueue(
                Request.Builder<Void>()
                        .get()
                        .url(server!!.url("/failure").toString())
                        .maxRetries(MAX_RETRIES)
                        .build(),
                listener)
        
        for (i in 0..1 + MAX_RETRIES - 1) {
            server!!.takeRequest()
        }
        Thread.sleep(100)
        
        assertThat(server!!.requestCount).isEqualTo(MAX_RETRIES + 1)
        verify(listener).onFailure(argThat {
                assertThat(this).isInstanceOf(ResponseException::class.java)
                assertThat((this as ResponseException).response).isEqualTo(
                        Response(404, responseBody.toByteArray(), responseBody))
                true
            })
    }
    
    @Test
    fun retriesRespectDelay() {
        val listener = mock<RequestListener<Response<Void>>>()
        
        server!!.enqueue(MockResponse().setResponseCode(404))
        server!!.enqueue(MockResponse().setResponseCode(200))
        
        uut!!.enqueue(
                Request.Builder<Void>()
                        .get()
                        .url(server!!.url("/delay").toString())
                        .maxRetries(1)
                        .retryDelay(1000)
                        .build(),
                listener)
        
        server!!.takeRequest()
        val first = System.currentTimeMillis()
        server!!.takeRequest()
        val second = System.currentTimeMillis()
        Thread.sleep(100)
        
        assertThat(second - first).isGreaterThan(900L)
        assertThat(second - first).isAtMost(1100L)
        assertThat(server!!.requestCount).isEqualTo(2)
        verify(listener).onSuccess(any())
    }
    
    // FIXME test failing only on jenkins
    @Ignore
    @Test
    fun requestCancellation() {
        val listener = mock<RequestListener<Response<Void>>>()
        
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
        verifyZeroInteractions(listener)
    }
    
    companion object {
        
        private val MAX_RETRIES = 1
    }
}
