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
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream

@RunWith(JUnit4::class)
class ResponseTest {
    
    @Test
    fun ctor() {
        val code = 1
        val bytes = byteArrayOf(1)
        val body = Object()
        val uut = Response(code, bytes, body)
        
        assertThat(uut.code).isSameAs(code)
        assertThat(uut.bytes).isSameAs(bytes)
        assertThat(uut.body).isSameAs(body)
    }
    
    @Test
    fun isSuccessful() {
        assertThat(Response(Integer.MIN_VALUE, null, null).isSuccessful).isFalse()
        
        assertThat(Response(199, null, null).isSuccessful).isFalse()
        assertThat(Response(200, null, null).isSuccessful).isTrue()
        
        assertThat(Response(299, null, null).isSuccessful).isTrue()
        assertThat(Response(300, null, null).isSuccessful).isFalse()
        
        assertThat(Response(Integer.MAX_VALUE, null, null).isSuccessful).isFalse()
    }
    
    @Test
    fun equalsAndHashCode() {
        EqualsVerifier.forClass(Response::class.java).verify()
    }
    
    @Test
    fun create() {
        val code = 1
        val input = "lorem ipsum"
        val stream = ByteArrayInputStream(input.toByteArray())
        
        val uut = Response.create(
                code,
                input.toByteArray().size,
                stream,
                ResponseBodyConverter.STRING)
        
        assertThat(uut.code).isEqualTo(code)
        assertThat(uut.body).isEqualTo(input)
        assertThat(stream.available()).isEqualTo(0)
    }
    
    @Test
    fun createWithStreamingInput() {
        val input = arrayOf("lorem ", "ipsum")
        val stream = PipedInputStream()
        val out = PipedOutputStream(stream)
        
        Thread(Runnable {
            for (token in input) {
                Thread.sleep(100)
                out.write(token.toByteArray())
            }
            
            out.close()
        }).start()
        val uut = Response.create(
                1,
                -1,
                stream,
                ResponseBodyConverter.STRING)
        
        assertThat(uut.body).isEqualTo("lorem ipsum")
        try {
            stream.read()
            fail("stream has not been closed")
        } catch (e: IOException) {
            // expected
        }
    }
}
