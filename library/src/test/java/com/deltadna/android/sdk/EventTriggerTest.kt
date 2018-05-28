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

import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.google.common.truth.Truth.*
import com.nhaarman.mockito_kotlin.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.*

@RunWith(JUnit4::class)
class EventTriggerTest {
    
    private lateinit var ddna: DDNA
    
    @Before
    fun before() {
        ddna = mock()
    }
    
    @Test
    fun `attributes are extracted at construction`() {
        with(EventTrigger(
                ddna,
                jsonObject(
                        "eventName" to "name",
                        "response" to jsonObject("a" to 1),
                        "campaignID" to 1,
                        "variantID" to 2)
                        .convert())) {
            
            assertThat(eventName).isEqualTo("name")
            assertThat(response.convert()).isEqualTo(jsonObject("a" to 1))
            assertThat(campaignId).isEqualTo(1)
            assertThat(variantId).isEqualTo(2)
        }
    }
    
    @Test
    fun `missing attributes are replaced with sane defaults`() {
        with(EventTrigger(ddna, jsonObject().convert())) {
            assertThat(eventName).isEmpty()
            assertThat(response.length()).isEqualTo(0)
            assertThat(campaignId).isEqualTo(-1)
            assertThat(variantId).isEqualTo(-1)
        }
    }
    
    @Test
    fun `action returns game parameters by default`() {
        with(EventTrigger(
                ddna,
                jsonObject(
                        "response" to jsonObject("image" to jsonObject()))
                        .convert())) {
            
            assertThat(action).isEqualTo("gameParameters")
        }
    }
    
    @Test
    fun `action returns image message when present`() {
        with(EventTrigger(
                ddna,
                jsonObject(
                        "response" to jsonObject("image" to jsonObject("a" to 1)))
                        .convert())) {
            
            assertThat(action).isEqualTo("imageMessage")
        }
    }
    
    @Test
    fun `triggers are ordered according to their priorities`() {
        val top = EventTrigger(ddna, jsonObject("priority" to 2).convert())
        val middle = EventTrigger(ddna, jsonObject("priority" to 1).convert())
        val bottom = EventTrigger(ddna, jsonObject("priority" to 0).convert())
        val expected = listOf(top, middle, bottom)
        
        with(mutableListOf(middle, bottom, top)) {
            sort()
            
            assertThat(this).isEqualTo(expected)
        }
    }
    
    @Test
    fun `trigger is evaluated indefinitely by default`() {
        with(EventTrigger(ddna, jsonObject("eventName" to "a").convert())) {
            val event = KEvent("a")
            
            for (i in 0..10) assertThat(evaluate(event)).isTrue()
        }
    }
    
    @Test
    fun `trigger is evaluated up to the limit number of times`() {
        val limit = 3
        with(EventTrigger(
                ddna,
                jsonObject(
                        "eventName" to "a",
                        "limit" to limit)
                        .convert())) {
            
            val event = KEvent("a")
            
            for (i in 0..limit+1) {
                if (i < limit) assertThat(evaluate(event)).isTrue()
                else assertThat(evaluate(event)).isFalse()
            }
        }
    }
    
    @Test
    fun `trigger sends conversion event when evaluated`() {
        val eventName = "a"
        val campaignId = 1L
        val priority = 2
        val variantId = 3L
        
        EventTrigger(
                ddna,
                jsonObject(
                        "eventName" to eventName,
                        "campaignID" to campaignId,
                        "priority" to priority,
                        "variantID" to variantId)
                        .convert())
                .evaluate(KEvent(eventName))
        
        verify(ddna).recordEvent(argThat<Event<KEvent>> {
            name == "ddnaEventTriggeredAction" &&
            with(params.json) {
                get("ddnaEventTriggeredCampaignID") == campaignId &&
                get("ddnaEventTriggeredCampaignPriority") == priority &&
                get("ddnaEventTriggeredVariantID") == variantId &&
                get("ddnaEventTriggeredActionType") == "gameParameters" &&
                get("ddnaEventTriggeredSessionCount") == 1
            }
        })
    }
    
    @Test
    fun `trigger does not send a conversion event when evaluation fails`() {
        EventTrigger(ddna, jsonObject("eventName" to "a").convert())
                .evaluate(KEvent("b"))
        
        verifyZeroInteractions(ddna)
    }
    
    @Test
    fun `evaluation fails against event with different name`() {
        assertThat(EventTrigger(ddna, jsonObject("eventName" to "a").convert())
                .evaluate(KEvent("b"))).isFalse()
    }
    
    @Test
    fun `empty condition evaluates successfully`() {
        assertThat(cond(KEvent())).isTrue()
    }
    
    @Test
    fun `evaluation fails on invalid operator`() {
        assertThat(cond(KEvent(), true.b(), true.b(), "equalz to".o()))
                .isFalse()
    }
    
    @Test
    fun `evaluation of logical operators`() {
        assertThat(cond(KEvent(), true.b(), true.b(), "and".o()))
                .isTrue()
        assertThat(cond(KEvent(), true.b(), false.b(), "and".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to true)), "a".p(), true.b(), "and".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to true)), "a".p(), false.b(), "and".o()))
                .isFalse()
        
        assertThat(cond(KEvent(), false.b(), true.b(), "or".o())).isTrue()
        assertThat(cond(KEvent(), false.b(), false.b(), "or".o())).isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to false)), "a".p(), true.b(), "or".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to false)), "a".p(), false.b(), "or".o()))
                .isFalse()
    }
    
    @Test
    fun `evaluation of logical operators against incompatible types`() {
        assertThat(cond(KEvent(params = *arrayOf("a" to 1)), "a".p(), 1.i(), "and".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 1L)), "a".p(), 1L.l(), "and".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 1F)), "a".p(), 1F.f(), "and".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 1.0)), "a".p(), 1.0.d(), "and".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to "b")), "a".p(), "b".s(), "and".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to Date())), "a".p(), Date().t(), "and".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 1)), "a".p(), 1.i(), "or".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 1L)), "a".p(), 1L.l(), "or".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 1f)), "a".p(), 1F.f(), "or".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 1.0)), "a".p(), 1.0.d(), "or".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to "b")), "a".p(), "b".s(), "or".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to Date())), "a".p(), Date().t(), "or".o()))
                .isFalse()
    }
    
    @Test
    fun `evaluation of equality comparison operators`() {
        assertThat(cond(KEvent(params = *arrayOf("a" to true)), "a".p(), true.b(), "equal to".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to true)), "a".p(), false.b(), "equal to".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 4.i(), "equal to".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 5.i(), "equal to".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 6.i(), "equal to".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 4L.l(), "equal to".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 5L.l(), "equal to".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 6L.l(), "equal to".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5F)), "a".p(), 4F.f(), "equal to".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5F)), "a".p(), 5F.f(), "equal to".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5F)), "a".p(), 6F.f(), "equal to".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 4.0.d(), "equal to".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 5.0.d(), "equal to".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 6.0.d(), "equal to".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to "b")), "a".p(), "b".s(), "equal to".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to "b")), "a".p(), "c".s(), "equal to".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(4).t(), "equal to".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(5).t(), "equal to".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(6).t(), "equal to".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to true)), "a".p(), true.b(), "not equal to".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to true)), "a".p(), false.b(), "not equal to".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 4.i(), "not equal to".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 5.i(), "not equal to".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 6.i(), "not equal to".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 4L.l(), "not equal to".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 5L.l(), "not equal to".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 6L.l(), "not equal to".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5F)), "a".p(), 4F.f(), "not equal to".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5F)), "a".p(), 5F.f(), "not equal to".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5F)), "a".p(), 6F.f(), "not equal to".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 4.0.d(), "not equal to".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 5.0.d(), "not equal to".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 6.0.d(), "not equal to".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to "b")), "a".p(), "b".s(), "not equal to".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to "b")), "a".p(), "c".s(), "not equal to".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(4).t(), "not equal to".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(5).t(), "not equal to".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(6).t(), "not equal to".o()))
                .isTrue()
    }
    
    @Test
    fun `evaluation of comparison operators`() {
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 4.i(), "greater than".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 5.i(), "greater than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 6.i(), "greater than".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 4L.l(), "greater than".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 5L.l(), "greater than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 6L.l(), "greater than".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5F)), "a".p(), 4F.f(), "greater than".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5F)), "a".p(), 5F.f(), "greater than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5F)), "a".p(), 6F.f(), "greater than".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 4.0.d(), "greater than".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 5.0.d(), "greater than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 6.0.d(), "greater than".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(4).t(), "greater than".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(5).t(), "greater than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(6).t(), "greater than".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 4.i(), "greater than eq".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 5.i(), "greater than eq".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 6.i(), "greater than eq".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 4L.l(), "greater than eq".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 5L.l(), "greater than eq".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 6L.l(), "greater than eq".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5F)), "a".p(), 4F.f(), "greater than eq".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5F)), "a".p(), 5F.f(), "greater than eq".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5F)), "a".p(), 6F.f(), "greater than eq".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 4.0.d(), "greater than eq".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 5.0.d(), "greater than eq".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 6.0.d(), "greater than eq".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(4).t(), "greater than eq".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(5).t(), "greater than eq".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(6).t(), "greater than eq".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 4.i(), "less than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 5.i(), "less than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 6.i(), "less than".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 4L.l(), "less than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 5L.l(), "less than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 6L.l(), "less than".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5F)), "a".p(), 4F.f(), "less than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5F)), "a".p(), 5F.f(), "less than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5F)), "a".p(), 6F.f(), "less than".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 4.0.d(), "less than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 5.0.d(), "less than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 6.0.d(), "less than".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(4).t(), "less than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(5).t(), "less than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(6).t(), "less than".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 4.i(), "less than eq".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 5.i(), "less than eq".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5)), "a".p(), 6.i(), "less than eq".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 4L.l(), "less than eq".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 5L.l(), "less than eq".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5L)), "a".p(), 6L.l(), "less than eq".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5f)), "a".p(), 4F.f(), "less than eq".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5f)), "a".p(), 5F.f(), "less than eq".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5f)), "a".p(), 6F.f(), "less than eq".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 4.0.d(), "less than eq".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 5.0.d(), "less than eq".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to 5.0)), "a".p(), 6.0.d(), "less than eq".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(4).t(), "less than eq".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(5).t(), "less than eq".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to Date(5))), "a".p(), Date(6).t(), "less than eq".o()))
                .isTrue()
    }
    
    @Test
    fun `evaluation of comparison operators against incompatible types`() {
        assertThat(cond(KEvent(params = *arrayOf("a" to true)), "a".p(), true.b(), "greater than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to true)), "a".p(), true.b(), "greater than eq".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to true)), "a".p(), true.b(), "less than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to true)), "a".p(), true.b(), "less than eq".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to "b")), "a".p(), "b".s(), "greater than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to "b")), "a".p(), "b".s(), "greater than eq".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to "b")), "a".p(), "b".s(), "less than".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to "b")), "a".p(), "b".s(), "less than eq".o()))
                .isFalse()
    }
    
    @Test
    fun `evaluation of string comparison operators`() {
        assertThat(cond(KEvent(params = *arrayOf("a" to "b")), "a".p(), "b".s(), "equal to".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to "b")), "a".p(), "B".s(), "equal to".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to "b")), "a".p(), "b".s(), "not equal to".o()))
                .isFalse()
        assertThat(cond(KEvent(params = *arrayOf("a" to "b")), "a".p(), "B".s(), "not equal to".o()))
                .isTrue()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to "HeLlO wOrLd")), "a".p(), "O w".s(), "contains".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to "HeLlO wOrLd")), "a".p(), "o W".s(), "contains".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to "HeLlO wOrLd")), "a".p(), "O w".s(), "contains ic".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to "HeLlO wOrLd")), "a".p(), "o W".s(), "contains ic".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to "HeLlO wOrLd")), "a".p(), "oW".s(), "contains ic".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to "HeLlO wOrLd")), "a".p(), "HeLlO".s(), "starts with".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to "HeLlO wOrLd")), "a".p(), "Hello".s(), "starts with".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to "HeLlO wOrLd")), "a".p(), "HeLlO".s(), "starts with ic".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to "HeLlO wOrLd")), "a".p(), "hElLo".s(), "starts with ic".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to "HeLlO wOrLd")), "a".p(), "wOrLd".s(), "starts with ic".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to "HeLlO wOrLd")), "a".p(), "wOrLd".s(), "ends with".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to "HeLlO wOrLd")), "a".p(), "World".s(), "ends with".o()))
                .isFalse()
        
        assertThat(cond(KEvent(params = *arrayOf("a" to "HeLlO wOrLd")), "a".p(), "wOrLd".s(), "ends with ic".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to "HeLlO wOrLd")), "a".p(), "WoRlD".s(), "ends with ic".o()))
                .isTrue()
        assertThat(cond(KEvent(params = *arrayOf("a" to "HeLlO wOrLd")), "a".p(), "HeLlO".s(), "ends with ic".o()))
                .isFalse()
    }
    
    @Test
    fun `evaluation of complex expressions`() {
        assertThat(cond(KEvent(
                params = *arrayOf("a" to 10, "b" to 5, "c" to "c", "d" to true)),
                "c".p(), "c".s(), "equal to".o(), "a".p(), 15.i(), "less than".o(), "and".o(), "b".p(), 15.i(), "greater than eq".o(), "and".o(), "d".p(), true.b(), "equal to".o(), "or".o()))
                .isTrue()
    }
    
    @Test
    fun `evaluation disambiguates between strings and timestamps`() {
        assertThat(cond(KEvent(
                params = *arrayOf("a" to "value", "b" to DDNA.TIMESTAMP_FORMAT.parse("1970-01-01 00:00:00.000"))),
                "a".p(), "value".s(), "not equal to".o(), "b".p(), jsonObject("t" to "1971-01-01T00:00:00.000+0000"), "less than".o(), "or".o()))
                .isTrue()
    }
    
    @Test
    fun `evaluation fails on missing parameters`() {
        assertThat(cond(KEvent(
                params = *arrayOf("a" to 5)),
                "b".p(), 5.i(), "equal to".o()))
                .isFalse()
    }
    
    @Test
    fun `evaluation fails on mismatched parameter types`() {
        assertThat(cond(KEvent(
                params = *arrayOf("a" to "b")),
                "a".p(), 5.i(), "not equal to".o()))
                .isFalse()
    }
    
    private fun cond(event: KEvent, vararg values: Any) = EventTrigger(
            ddna,
            jsonObject(
                    "eventName" to event.name,
                    "condition" to jsonArray(*values))
                    .convert())
            .evaluate(event)
    
    private fun Boolean.b() = jsonObject("b" to this)
    private fun Date.t() = jsonObject("t" to this.tsIso())
    private fun Double.d() = jsonObject("d" to this)
    private fun Float.f() = jsonObject("f" to this)
    private fun Int.i() = jsonObject("i" to this)
    private fun Long.l() = jsonObject("l" to this)
    private fun String.p() = jsonObject("p" to this)
    private fun String.s() = jsonObject("s" to this)
    private fun String.o() = jsonObject("o" to this)
}
