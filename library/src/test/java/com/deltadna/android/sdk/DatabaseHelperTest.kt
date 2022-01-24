/*
 * Copyright (c) 2018 deltaDNA Ltd. All rights reserved.
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

import com.github.salomonbrys.kotson.jsonObject
import com.google.common.truth.Truth.*
import org.json.JSONObject
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Ignore("Test relies on async behaviour running synchronously - needs to be refactored")
class DatabaseHelperTest {
    
    private lateinit var uut: DatabaseHelper
    
    @Before
    fun before() {
        uut = DatabaseHelper(RuntimeEnvironment.application)
    }
    
    @Test
    fun `null returned when action not found`() {
        assertThat(uut.getAction(1)).isNull()
    }
    
    @Test
    fun `action is inserted and retrieved`() {
        val parameters = jsonObject("a" to 1, "b" to 2).convert()
        
        assertThat(uut.insertActionRow("name", 1L, Date(), parameters)).isTrue()
        assertThat(uut.getAction(1L).toString()).isEqualTo("$parameters")
    }
    
    @Test
    fun `action is removed`() {
        uut.insertActionRow("name", 1L, Date(), JSONObject())
        
        assertThat(uut.removeActionRow(1L)).isTrue()
        assertThat(uut.getAction(1L)).isNull()
        assertThat(uut.removeActionRow(1L)).isFalse()
    }
    
    @Test
    fun `actions are removed`() {
        uut.insertActionRow("name1", 1L, Date(), JSONObject())
        uut.insertActionRow("name2", 2L, Date(), JSONObject())
        
        uut.removeActionRows()
        
        assertThat(uut.getAction(1L)).isNull()
        assertThat(uut.getAction(2L)).isNull()
    }
}
