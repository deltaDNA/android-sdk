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
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner
import java.io.OutputStream
import java.net.HttpURLConnection

@RunWith(MockitoJUnitRunner::class)
class RequestBodyTest {
    
    @Test
    fun ctor() {
        val type = "type"
        val content = byteArrayOf(0)
        val uut = RequestBody(type, content)
        
        assertThat(uut.type).isEqualTo(type)
        assertThat(uut.content).isEqualTo(content)
    }
    
    @Test
    fun fill() {
        val uut = RequestBody("type", byteArrayOf(0))
        val conn = mock<HttpURLConnection>()
        val os = mock<OutputStream>()
        whenever(conn.outputStream).thenReturn(os)
        
        uut.fill(conn)
        
        verify(conn).setFixedLengthStreamingMode(eq(uut.content.size))
        verify(conn).setRequestProperty(eq("Content-Type"), eq(uut.type))
        verify(os).write(eq(uut.content))
    }
    
    @Test
    fun json() {
        val uut = RequestBody.json(JSONObject().put("field", 1))
        
        assertThat(uut.type).isEqualTo("application/json; charset=utf-8")
        assertThat(uut.content).isEqualTo("{\"field\":1}".toByteArray(charset("UTF-8")))
    }
}
