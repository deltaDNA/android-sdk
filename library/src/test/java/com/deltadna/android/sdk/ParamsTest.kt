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

package com.deltadna.android.sdk

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.*

@RunWith(JUnit4::class)
class ParamsTest {
    
    private lateinit var json: JSONObject
    
    private lateinit var uut: Params
    
    @Before
    fun before() {
        json = JSONObject()
        
        uut = Params(json, mutableMapOf())
    }
    
    @Test
    fun `value is added into JSON`() {
        uut.put("value", 1)
        assertThat(json["value"]).isEqualTo(1)
    }
    
    @Test
    fun `nested value is added into JSON`() {
        with(Params().put("a", 1)) {
            uut.put("value", this)
            assertThat(this@ParamsTest.json["value"]).isEqualTo(json)
        }
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `cannot add value with null key`() {
        uut.put(null, "value")
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `cannot add value with empty key`() {
        uut.put("", "value")
    }
    
    @Test
    fun `correct types are captured for added values`() {
        uut.put("boolean", true)
        uut.put("Boolean", java.lang.Boolean.TRUE)
        uut.put("int", 1)
        uut.put("Integer", java.lang.Integer(1))
        uut.put("long", 1L)
        uut.put("Long", java.lang.Long(1L))
        uut.put("float", 1F)
        uut.put("Float", java.lang.Float(1F))
        uut.put("double", 1.0)
        uut.put("Double", java.lang.Double(1.0))
        uut.put("String", "value")
        uut.put("Date", Date())
        
        assertThat(uut.typeOf("boolean")).isEqualTo(java.lang.Boolean::class.java)
        assertThat(uut.typeOf("Boolean")).isEqualTo(java.lang.Boolean::class.java)
        assertThat(uut.typeOf("int")).isEqualTo(java.lang.Integer::class.java)
        assertThat(uut.typeOf("Integer")).isEqualTo(java.lang.Integer::class.java)
        assertThat(uut.typeOf("long")).isEqualTo(java.lang.Long::class.java)
        assertThat(uut.typeOf("Long")).isEqualTo(java.lang.Long::class.java)
        assertThat(uut.typeOf("float")).isEqualTo(java.lang.Float::class.java)
        assertThat(uut.typeOf("Float")).isEqualTo(java.lang.Float::class.java)
        assertThat(uut.typeOf("double")).isEqualTo(java.lang.Double::class.java)
        assertThat(uut.typeOf("Double")).isEqualTo(java.lang.Double::class.java)
        assertThat(uut.typeOf("String")).isEqualTo(String::class.java)
        assertThat(uut.typeOf("Date")).isEqualTo(Date::class.java)
    }
    
    @Test
    fun `check whether parameters are empty or not`() {
        assertThat(uut.isEmpty).isTrue()
        
        uut.put("a", 1)
        assertThat(uut.isEmpty).isFalse()
    }
}
