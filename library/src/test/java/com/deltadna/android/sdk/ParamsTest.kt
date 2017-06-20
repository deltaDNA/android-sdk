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
import com.nhaarman.mockito_kotlin.*
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParamsTest {
    
    private var uut: Params? = null
    private val json = mock<JSONObject>()
    
    @Before
    fun before() {
        uut = Params(json)
    }
    
    @After
    fun after() {
        uut = null
        reset(json)
    }
    
    @Test
    fun put() {
        uut!!.put("key", "value")
        uut!!.put("keyNull", null)
        
        verify(json).put(eq("key"), eq("value"))
        verify(json).put(eq("keyNull"), isNull<JsonParams>())
    }
    
    @Test
    fun putNested() {
        with(JSONObject()) {
            uut!!.put("key", Params(this))
            
            verify(json).put(eq("key"), same(this))
        }
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun putKeyCannotBeNull() {
        uut!!.put(null, "value")
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun putKeyCannotBeEmpty() {
        uut!!.put("", "value")
    }
    
    @Test
    fun isEmpty() {
        whenever(json.length()).thenReturn(0, 1)
        
        assertThat(uut!!.isEmpty).isTrue()
        assertThat(uut!!.isEmpty).isFalse()
    }
}
