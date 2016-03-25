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

import com.deltadna.android.sdk.helpers.Settings
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.mockito.runners.MockitoJUnitRunner
import java.io.File
import java.nio.charset.Charset

@RunWith(MockitoJUnitRunner::class)
class NetworkManagerTest {
    
    @Suppress("unused") // accessed by test framework
    @get:Rule
    val flaky = object : TestRule {
        val tries = 5
        
        override fun apply(
                base: Statement?,
                description: Description?): Statement? {
            
            return object : Statement() {
                override fun evaluate() {
                    var caught: Throwable? = null
                    
                    (1..tries).forEach {
                        try {
                            base!!.evaluate()
                            return
                        } catch (t: Throwable) {
                            caught = t
                            System.err.println(t)
                        }
                    }
                    
                    throw caught!!
                }
            }
        }
    }
    
    private var uut: NetworkManager? = null
    private var server: MockWebServer? = null
    
    @Before
    fun before() {
        val settings = mock<Settings>()
        whenever(settings.httpRequestMaxRetries()).thenReturn(1)
        
        server = MockWebServer()
        server!!.start()
        
        uut = NetworkManager(
                ENV_KEY,
                server!!.url(COLLECT).toString(),
                server!!.url(ENGAGE).toString(),
                settings,
                null)
    }
    
    @After
    fun after() {
        server!!.shutdown()
        uut = null
    }
    
    @Test
    fun collect() {
        server!!.enqueue(MockResponse().setResponseCode(200))
        
        uut!!.collect(JSONObject().put("field", 1), null)
        
        with(server!!.takeRequest()) {
            assertThat(path).isEqualTo(COLLECT + "/" + ENV_KEY)
            assertThat(method).isEqualTo("POST")
            assertThat(body.readUtf8()).isEqualTo("{\"field\":1}")
        }
    }
    
    @Test
    fun collectBulk() {
        server!!.enqueue(MockResponse().setResponseCode(200))
        
        uut!!.collect(JSONObject().put("eventList", "[]"), null)
        
        with(server!!.takeRequest()) {
            assertThat(path).isEqualTo("$COLLECT/$ENV_KEY/bulk")
        }
    }
    
    @Test
    fun collectBulkWithHash() {
        server!!.enqueue(MockResponse().setResponseCode(200))
        
        uut = NetworkManager(
                ENV_KEY,
                server!!.url(COLLECT).toString(),
                server!!.url(ENGAGE).toString(),
                mock(),
                "hash")
        uut!!.collect(JSONObject().put("eventList", "[]"), null)
        
        with(server!!.takeRequest()) {
            assertThat(path).startsWith("$COLLECT/$ENV_KEY/bulk/hash")
        }
    }
    
    @Test
    fun collectWithHash() {
        server!!.enqueue(MockResponse().setResponseCode(200))
        
        uut = NetworkManager(
                ENV_KEY,
                server!!.url(COLLECT).toString(),
                server!!.url(ENGAGE).toString(),
                mock(),
                "hash")
        uut!!.collect(JSONObject(), null)
        
        assertThat(server!!.takeRequest().path)
                .startsWith("$COLLECT/$ENV_KEY/hash")
    }
    
    @Test
    fun engage() {
        server!!.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody("{\"result\":1}"))
        
        uut!!.engage(JSONObject().put("field", 1), mock())
        
        with(server!!.takeRequest()) {
            assertThat(path).isEqualTo(ENGAGE + "/" + ENV_KEY)
            assertThat(method).isEqualTo("POST")
            assertThat(body.readUtf8()).isEqualTo("{\"field\":1}")
        }
    }
    
    @Test
    fun engageWithHash() {
        server!!.enqueue(MockResponse().setResponseCode(200))
        
        uut = NetworkManager(
                ENV_KEY,
                server!!.url(COLLECT).toString(),
                server!!.url(ENGAGE).toString(),
                mock(),
                "hash")
        uut!!.engage(JSONObject().put("field", 1), mock())
        
        assertThat(server!!.takeRequest().path)
                .startsWith("$ENGAGE/$ENV_KEY/hash")
    }
    
    @Test
    fun fetch() {
        val dst = File.createTempFile("ddnasdk-test-", ".tmp")
        
        server!!.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody("response\nline\nline"))
        
        uut!!.fetch(server!!.url("/file").toString(), dst, mock())
        
        with(server!!.takeRequest()) {
            assertThat(path).isEqualTo("/file")
            assertThat(method).isEqualTo("GET")
        }
        
        // FIXME flaky on Travis CI, most likely due to slow file writing
        assertThat(dst.readLines(Charset.forName("UTF-8")))
                .isEqualTo(listOf("response", "line", "line"))
        assertThat(dst.delete())
    }
    
    companion object {
        
        private val ENV_KEY = "env_key"
        private val COLLECT = "/collect"
        private val ENGAGE = "/engage"
    }
}
