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
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.*
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [EventActionHandlerTest.ShadowImageMessage::class])
class EventActionHandlerTest {
    
    private lateinit var store: ActionStore
    
    @Before
    fun before() {
        store = mock()
    }
    
    @Test
    fun `game parameters handler only handles game parameter actions`() {
        val cbk = mock<EventActionHandler.Callback<JSONObject>>()
        
        with(EventActionHandler.GameParametersHandler(cbk)) {
            assertThat(type).isEqualTo("gameParameters")
            
            assertThat(handle(mock<EventTrigger>().apply {
                whenever(action).then { "imageMessage" }
            }, store)).isFalse()
            verifyNoMoreInteractions(cbk)
            
            val json = jsonObject("parameters" to jsonObject("a" to 1)).convert()
            assertThat(handle(mock<EventTrigger>().apply {
                whenever(action).then { "gameParameters" }
                whenever(response).then { json }
            }, store)).isTrue()
            verify(cbk).handle(argThat {
                toString() == jsonObject("a" to 1).toString()
            })
            
            assertThat(handle(mock<EventTrigger>().apply {
                whenever(action).then { "gameParameters" }
                whenever(response).then { JSONObject() }
            }, store)).isTrue()
            verify(cbk, times(2)).handle(isNotNull())
        }
        
        verify(store, never()).remove(any())
    }
    
    @Test
    fun `game parameters handler uses persisted action and removes it`() {
        val cbk = mock<EventActionHandler.Callback<JSONObject>>()
        val trigger = mock<EventTrigger>().apply {
            whenever(action).then { "gameParameters" }
            whenever(response).then { jsonObject("parameters" to
                    jsonObject("a" to 1)).convert() }
        }
        val params = jsonObject("b" to 2).convert()
        whenever(store.get(trigger)).then { params }
        
        with(EventActionHandler.GameParametersHandler(cbk)) {
            assertThat(handle(trigger, store)).isTrue()
            verify(store).remove(same(trigger))
            verify(cbk).handle(argThat { "$this" == "$params" })
        }
    }
    
    @Test
    fun `image message handler only handles image message actions`() {
        val cbk = mock<EventActionHandler.Callback<ImageMessage>>()
        
        with(EventActionHandler.ImageMessageHandler(cbk)) {
            assertThat(type).isEqualTo("imageMessage")
            
            assertThat(handle(mock<EventTrigger>().apply {
                whenever(action).then { "gameParameters" }
            }, store)).isFalse()
            verifyNoMoreInteractions(cbk)
            
            assertThat(handle(mock<EventTrigger>().apply {
                whenever(action).then { "imageMessage" }
                whenever(response).then { jsonObject(
                        "image" to jsonObject(),
                        "prepared" to true)
                        .convert() }
            }, store)).isTrue()
            verify(cbk).handle(notNull())
            
            assertThat(handle(mock<EventTrigger>().apply {
                whenever(action).then { "imageMessage" }
                whenever(response).then { jsonObject(
                        "image" to jsonObject(),
                        "prepared" to false)
                        .convert() }
            }, store)).isFalse()
            verifyNoMoreInteractions(cbk)
        }
    }
    
    @Test
    fun `image message handler uses persisted action and removes it`() {
        val cbk = mock<EventActionHandler.Callback<ImageMessage>>()
        val trigger = mock<EventTrigger>().apply {
            whenever(action).then { "imageMessage" }
            whenever(response).then { jsonObject(
                    "image" to jsonObject(),
                    "prepared" to true,
                    "parameters" to jsonObject("a" to 1))
                    .convert() }
        }
        val params = jsonObject("b" to 2).convert()
        whenever(store.get(trigger)).then { params }
        
        with(EventActionHandler.ImageMessageHandler(cbk)) {
            assertThat(handle(trigger, store)).isTrue()
            verify(store).remove(same(trigger))
            verify(cbk).handle(argThat { "${parameters()}" == "$params" })
        }
    }
    
    // to avoid needing to construct a valid image message json
    @Suppress("unused", "TestFunctionName")
    @Implements(ImageMessage::class)
    class ShadowImageMessage : Shadow() {
        
        private lateinit var json: JSONObject
        private var prepared = false
        
        @Implementation
        fun __constructor__(json: JSONObject) {
            this.json = json
            
            assertThat(json.has("image")).isTrue()
            prepared = json.optBoolean("prepared")
        }
        
        @Implementation
        fun prepared() = prepared
        
        @Implementation
        fun parameters(): JSONObject = json.getJSONObject("parameters")
    }
}
