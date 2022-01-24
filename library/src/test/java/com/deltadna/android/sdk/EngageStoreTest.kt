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

import android.database.Cursor
import android.util.Base64
import com.deltadna.android.sdk.DatabaseHelper.Engagements.Column.*
import com.deltadna.android.sdk.helpers.Settings
import com.github.salomonbrys.kotson.jsonObject
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAsyncTask
import java.io.File
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [EngageStoreTest.ShadowBase64::class, ShadowAsyncTask::class])
class EngageStoreTest {
    
    private val settings = Settings()
    
    private lateinit var database: DatabaseHelper
    private lateinit var path: File
    
    private lateinit var uut: EngageStore
    
    @Before
    fun before() {
        database = mock()
        path = createTempDir()
        
        uut = EngageStore(database, path, settings)
    }
    
    @After
    fun after() {
        path.deleteRecursively()
    }
    
    @Test
    fun `old cache is cleaned up`() {
        val old = File(path, "ENGAGEMENTS").apply { createNewFile() }
        uut = EngageStore(database, path, settings)
        
        assertThat(old.exists()).isFalse()
    }
    
    @Test
    fun `inserts successful engagement into database`() {
        uut.put(mock<Engagement<*>>().apply {
            whenever(isSuccessful()).then { true }
            whenever(getDecisionPoint()).then { "dp" }
            whenever(getFlavour()).then { "flavour" }
            whenever(getJson()).then { jsonObject("a" to 1).convert() }
        })
        
        verify(database).insertEngagementRow(
                eq("dp"),
                eq("flavour"),
                argThat { Range
                        .closed(Date(System.currentTimeMillis() - 500),
                                Date(System.currentTimeMillis() + 500))
                        .contains(this) },
                eq(Base64.encode(
                        jsonObject("a" to 1).toString().toByteArray(),
                        Base64.DEFAULT)))
    }
    
    @Test
    fun `does not insert unsuccessful engagement`() {
        uut.put(mock<Engagement<*>>().apply {
            whenever(isSuccessful()).then { false }
        })

        verifyNoMoreInteractions(database)
    }
    
    @Test
    fun `returns null when engagement not cached`() {
        whenever(database.getEngagement(eq("dp"), eq("flavour"))).then {
            mock<Cursor>().apply { whenever(moveToFirst()).then { false } }}
        
        assertThat(uut.get(KEngagement("dp", "flavour"))).isNull()
    }
    
    @Test
    fun `returns null when cached engagement is stale and removes it`() {
        whenever(database.getEngagement(eq("dp"), eq("flavour"))).then {
            mock<Cursor>().apply {
                whenever(moveToFirst()).then { true }
                whenever(getColumnIndex(eq(CACHED.toString()))).then { 3 }
                whenever(getLong(eq(3))).then {
                    System.currentTimeMillis() - settings.engageCacheExpiry * 1000 - 500
                }
                whenever(getColumnIndex(eq(ID.toString()))).then { 0 }
                whenever(getLong(eq(0))).then { 1L }
            }}
        
        assertThat(uut.get(KEngagement("dp", "flavour"))).isNull()
        verify(database).removeEngagementRow(eq(1))
    }
    
    @Test
    fun `returns cached engagement`() {
        whenever(database.getEngagement(eq("dp"), eq("flavour"))).then {
            mock<Cursor>().apply {
                whenever(moveToFirst()).then { true }
                whenever(getColumnIndex(eq(CACHED.toString()))).then { 3 }
                whenever(getLong(eq(3))).then {
                    System.currentTimeMillis() - settings.engageCacheExpiry * 1000 + 500
                }
                whenever(getColumnIndex(eq(RESPONSE.toString()))).then { 4 }
                whenever(getBlob(eq(4))).then { Base64.encode(
                        jsonObject("a" to 1).toString().toByteArray(),
                        Base64.DEFAULT)
                }
            }}
        
        assertThat(uut.get(KEngagement("dp", "flavour")).toString())
                .isEqualTo(jsonObject("a" to 1).toString())
        verify(database, never()).removeEngagementRow(any())
    }
    
    @Test
    fun `disabled with expiry value of 0`() {
        uut = EngageStore(database, path, Settings().apply { engageCacheExpiry = 0 })
        
        assertThat(uut.get(KEngagement("dp", "flavour"))).isNull()
        verifyNoMoreInteractions(database)
    }
    
    @Test
    fun `clear removes stored engagements`() {
        uut.clear()
        
        verify(database).removeEngagementRows()
    }
    
    // no built-in shadow in Robolectric for Base64 class
    @Suppress("unused", "UNUSED_PARAMETER")
    @Implements(Base64::class)
    class ShadowBase64 : Shadow() {
        
        companion object {
            
            @Implementation
            fun encode(input: ByteArray, flags: Int) =
                    java.util.Base64.getEncoder().encode(input)!!
            
            @Implementation
            fun decode(input: ByteArray, flags: Int) =
                    java.util.Base64.getDecoder().decode(input)!!
        }
    }
}
