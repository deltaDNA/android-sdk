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

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.*
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.HttpURLConnection

@RunWith(JUnit4::class)
class ResponseTest {
    
    @Test
    fun ctor() {
        val code = 1
        val cached = true
        val bytes = byteArrayOf(1)
        val body = Object()
        val error = "error"
        val uut = Response(code, cached, bytes, body, error)
        
        assertThat(uut.code).isSameAs(code)
        assertThat(uut.cached).isSameAs(cached)
        assertThat(uut.bytes).isSameAs(bytes)
        assertThat(uut.body).isSameAs(body)
        assertThat(uut.error).isSameAs(error)
    }
    
    @Test
    fun isSuccessful() {
        assertThat(Response(Integer.MIN_VALUE, false, null, null, null).isSuccessful)
                .isFalse()
        
        assertThat(Response(199, false, null, null, null).isSuccessful)
                .isFalse()
        assertThat(Response(200, false, null, null, null).isSuccessful)
                .isTrue()
        
        assertThat(Response(299, false, null, null, null).isSuccessful)
                .isTrue()
        assertThat(Response(300, false, null, null, null).isSuccessful)
                .isFalse()
        
        assertThat(Response(Integer.MAX_VALUE, false, null, null, null).isSuccessful)
                .isFalse()
    }
    
    @Test
    fun equalsAndHashCode() {
        EqualsVerifier.forClass(Response::class.java).verify()
    }
    
    @Test
    fun inputStreamReadAndClosed() {
        with(mock<HttpURLConnection>()) {
            val input = "input".toByteArray()
            val stream = spy(ByteArrayInputStream(input))
            whenever(this.responseCode).thenReturn(200)
            whenever(this.contentLength).thenReturn(input.size)
            whenever(this.inputStream).thenReturn(stream)
            
            with(Response.create<String>(this, ResponseBodyConverter.STRING)) {
                assertThat(code).isEqualTo(200)
                assertThat(bytes).isEqualTo(input)
                assertThat(body).isEqualTo(String(bytes))
                assertThat(error).isNull()
            }
            verify(stream, times(input.size + 1)).read()
            verify(stream).close()
        }
    }
    
    @Test
    fun errorStreamReadAndClosed() {
        with(mock<HttpURLConnection>()) {
            val input = "input".toByteArray()
            val stream = spy(ByteArrayInputStream(input))
            whenever(this.responseCode).thenReturn(300)
            whenever(this.contentLength).thenReturn(input.size)
            whenever(this.errorStream).thenReturn(stream)
            
            with(Response.create<String>(this, ResponseBodyConverter.STRING)) {
                assertThat(code).isEqualTo(300)
                assertThat(bytes).isEqualTo(input)
                assertThat(body).isNull()
                assertThat(error).isEqualTo(String(bytes))
            }
            verify(stream, times(input.size + 1)).read()
            verify(stream).close()
        }
    }
    
    @Test
    fun createWithStreamingInput() {
        val input = arrayOf("lorem ", "ipsum")
        val stream = PipedInputStream()
        val connection = with(mock<HttpURLConnection>()) {
            whenever(this.responseCode).thenReturn(200)
            whenever(this.contentLength).thenReturn(-1)
            whenever(this.inputStream).thenReturn(stream)
            this
        }
        
        val out = PipedOutputStream(stream)
        Thread(Runnable {
            for (token in input) {
                Thread.sleep(100)
                out.write(token.toByteArray())
            }
            out.close()
        }).start()
        val uut = Response.create(connection, ResponseBodyConverter.STRING)
        
        assertThat(uut.body).isEqualTo("lorem ipsum")
        try {
            stream.read()
            fail("stream has not been closed")
        } catch (expected: IOException) {}
    }
}
