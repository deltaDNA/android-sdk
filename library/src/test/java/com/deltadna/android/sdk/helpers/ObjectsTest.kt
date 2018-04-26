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

package com.deltadna.android.sdk.helpers

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject

@RunWith(JUnit4::class)
class ObjectsTest {
    
    @Test
    fun equals() {
        assertThat(Objects.equals(null, null)).isTrue()
        val obj = Object()
        assertThat(Objects.equals(obj, obj)).isTrue()
        
        assertThat(Objects.equals(null, Object())).isFalse()
        assertThat(Objects.equals(Object(), null)).isFalse()
        assertThat(Objects.equals(Object(), Object())).isFalse()
    }
    
    @Test
    fun extract() {
        assertThat(Objects.extract(null, "a")).isNull()
        assertThat(Objects.extract(JSONObject("{\"a\":null}"), "a"))
                .isNull()
        assertThat(Objects.extract(JSONObject("{\"a\":{}}"), "a").toString())
                .isEqualTo("{}")
        assertThat(Objects.extract(JSONObject("{\"a\":{\"b\":{}}}"), "a").toString())
                .isEqualTo("{\"b\":{}}")
        assertThat(Objects.extract(JSONObject("{\"a\":{\"b\":{}}}"), "a", "b").toString())
                .isEqualTo("{}")
    }
}
