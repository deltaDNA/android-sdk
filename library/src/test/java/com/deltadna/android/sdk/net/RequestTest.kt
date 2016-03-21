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

import com.deltadna.android.sdk.exceptions.ResponseException
import com.google.common.truth.Truth.assertThat
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException

@RunWith(JUnit4::class)
class RequestTest {
    
    private var server: MockWebServer? = null
    
    @Before
    fun before() {
        server = MockWebServer()
        server!!.start()
    }
    
    @After
    fun after() {
        server!!.shutdown()
        server = null
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun malformedUrl() {
        Request.Builder<Void>().url("fail")
    }
    
    @Test
    fun get() {
        server!!.enqueue(MockResponse().setResponseCode(200))
        
        val response = Request.Builder<Void>()
                .get()
                .url(server!!.url("/get").toString())
                .build()
                .call()
        
        with(server!!.takeRequest()) {
            assertThat(method).isEqualTo("GET")
            assertThat(path).isEqualTo("/get")
            assertThat(bodySize).isEqualTo(0)
        }
        
        assertThat(response).isEqualTo(Response(200, byteArrayOf(), null))
    }

    @Test
    fun post() {
        val responseBody = "response"
        server!!.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "text/plain"))
        
        val response = Request.Builder<String>()
                .post(RequestBody("text/plain", "request".toByteArray()))
                .header("Accept", "text/plain")
                .url(server!!.url("/post").toString())
                .build()
                .setConverter(ResponseBodyConverter.STRING)
                .call()
        
        with(server!!.takeRequest()) {
            assertThat(method).isEqualTo("POST")
            assertThat(path).isEqualTo("/post")
            assertThat(body.readUtf8()).isEqualTo("request")
            assertThat(getHeader("Content-Type")).isEqualTo("text/plain")
            assertThat(getHeader("Accept")).isEqualTo("text/plain")
        }
        
        assertThat(response).isEqualTo(
                Response(200, responseBody.toByteArray(), responseBody))
    }
    
    @Test
    fun httpError() {
        val responseBody = "not found"
        server!!.enqueue(MockResponse()
                .setResponseCode(404)
                .setBody(responseBody))
        
        try {
            Request.Builder<Void>()
                    .get()
                    .url(server!!.url("/fail").toString())
                    .build()
                    .call()
            
            fail("Exception not thrown")
        } catch (e: Exception) {
            with(e as ResponseException) {
                assertThat(response).isEqualTo(
                        Response(404, responseBody.toByteArray(), responseBody))
            }
        }
    }
    
    @Test(expected = IOException::class)
    fun failure() {
        server!!.shutdown()
        
        Request.Builder<Void>()
                .get()
                .url(server!!.url("/fail").toString())
                .build()
                .call()
    }
}
