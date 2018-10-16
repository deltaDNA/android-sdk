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
import com.nhaarman.mockito_kotlin.*
import org.junit.Before
import org.junit.Test
import java.lang.System.currentTimeMillis

class ActionStoreTest {
    
    private lateinit var database: DatabaseHelper
    private lateinit var uut: ActionStore
    
    @Before
    fun before() {
        database = mock()
        uut = ActionStore(database)
    }
    
    @Test
    fun `action is retrieved from db on get`() {
        uut.get(mock<EventTrigger>().apply { whenever(campaignId).then { 1L } })
        
        verify(database).getAction(eq(1L))
    }
    
    @Test
    fun `action is inserted into db on put`() {
        val action = jsonObject("a" to 1, "b" to 2).convert()
        
        uut.put(mock<EventTrigger>().apply {
            whenever(eventName).then { "name" }
            whenever(campaignId).then { 1L }
        }, action)
        
        verify(database).insertActionRow(
                eq("name"),
                eq(1L),
                argThat {  time >= currentTimeMillis() - 500
                        && time <= currentTimeMillis() + 500 },
                argThat { "$this" == "$action" })
    }
    
    @Test
    fun `action is removed from db on remove`() {
        uut.remove(mock<EventTrigger>().apply { whenever(campaignId).then { 1L } })
        
        verify(database).removeActionRow(eq(1L))
    }
    
    @Test
    fun `action rows are removed from db on clear`() {
        uut.clear()
        
        verify(database).removeActionRows()
    }
}
