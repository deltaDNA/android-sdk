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

import com.nhaarman.mockito_kotlin.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.*

@RunWith(JUnit4::class)
class EventActionTest {
    
    private lateinit var store: ActionStore
    
    @Before
    fun before() {
        store = mock()
    }
    
    @Test
    fun `triggers are evaluated in order`() {
        val e = mock<Event<*>>()
        val t1 = mock<EventTrigger>()
        val t2 = mock<EventTrigger>()
        val t3 = mock<EventTrigger>()
        order(t2, t3, t1)
        
        EventAction(e, TreeSet<EventTrigger>().apply {
            add(t1)
            add(t2)
            add(t3)
        }, store).run()
        
        inOrder(t1, t2, t3) {
            verify(t2).evaluate(same(e))
            verify(t3).evaluate(same(e))
            verify(t1).evaluate(same(e))
        }
    }
    
    @Test
    fun `handlers are run in order of addition`() {
        val e = mock<Event<*>>()
        val t = mock<EventTrigger>()
        val h1 = mock<EventActionHandler<*>>()
        val h2 = mock<EventActionHandler<*>>()
        val h3 = mock<EventActionHandler<*>>()
        whenever(t.evaluate(e)).then { true }
        
        EventAction(e, TreeSet<EventTrigger>().apply { add(t) }, store)
                .add(h1)
                .add(h2)
                .add(h3)
                .run()
        
        inOrder(h1, h2, h3) {
            verify(h1).handle(same(t), same(store))
            verify(h2).handle(same(t), same(store))
            verify(h3).handle(same(t), same(store))
        }
    }
    
    @Test
    fun `handlers are run until one handles the action`() {
        val e = mock<Event<*>>()
        val t = mock<EventTrigger>()
        val h1 = mock<EventActionHandler<*>>()
        val h2 = mock<EventActionHandler<*>>()
        val h3 = mock<EventActionHandler<*>>()
        whenever(t.evaluate(e)).then { true }
        whenever(h1.handle(same(t), same(store))).then { false }
        whenever(h2.handle(same(t), same(store))).then { true }
        
        EventAction(e, TreeSet<EventTrigger>().apply { add(t) }, store)
                .add(h1)
                .add(h2)
                .add(h3)
                .run()
        
        verifyZeroInteractions(h3)
    }
    
    private fun order(vararg triggers: EventTrigger) {
        for (i in 0 until triggers.size) {
            for (j in 0 until triggers.size) {
                if (i == j) continue
                
                whenever(triggers[i].compareTo(triggers[j])).then {
                    if (i < j) -1 else 1
                }
            }
        }
    }
}
