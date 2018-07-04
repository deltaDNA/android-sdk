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

import com.deltadna.android.sdk.listeners.EngageListener
import com.deltadna.android.sdk.listeners.RequestListener
import com.deltadna.android.sdk.net.NetworkManager
import com.deltadna.android.sdk.net.Response
import com.deltadna.android.sdk.util.CloseableIterator
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.*
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EventHandlerTest {
    
    private lateinit var events: EventStore
    private lateinit var engagements: EngageStore
    private lateinit var network: NetworkManager
    
    private lateinit var uut: EventHandler
    
    @Before
    fun before() {
        events = mock()
        engagements = mock()
        network = mock()
        
        uut = EventHandler(events, engagements, network)
    }
    
    @After
    fun after() {
        uut.stop(false)
    }
    
    @Test
    fun startPeriodicUploads() {
        withStoreEvents(
                listOf("{\"value\":0}", "{\"value\":1}"),
                listOf("{\"value\":0}"))
        withListeners { onCompleted(Response(200, false, null, null, null)) }
        
        uut.start(0, 1)
        Thread.sleep(2200)
        
        verify(events, times(3)).items()
        var run = 0
        verify(network, times(2)).collect(
                argThat {
                    assertThat(toString()).isEqualTo(
                            when (run) {
                                0 -> "{\"eventList\":[{\"value\":0},{\"value\":1}]}"
                                1 -> "{\"eventList\":[{\"value\":0}]}"
                                else -> toString()
                            })
                    run++
                    true
                },
                any())
    }
    
    @Test
    fun stopPeriodicUploads() {
        uut.start(1, 1)
        uut.stop(false)
        Thread.sleep(2200)
        
        verify(events, never()).items()
        verify(network, never()).collect(any(), any())
    }
    
    @Test
    fun stopAndDispatch() {
        withStoreEvents(listOf("0")) {
            withListeners { onCompleted(Response(200, false, null, null, null)) }
            
            uut.start(1, 1)
            uut.stop(true)
            Thread.sleep(2200)
            
            verify(events).items()
            verify(network).collect(
                    any<JSONObject>(),
                    any<RequestListener<Void>>())
        }
    }
    
    @Test
    fun handleEvent() {
        with(JSONObject()) {
            uut.handleEvent(this)
            
            verify(events).add(eq(toString()))
        }
    }
    
    @Test
    fun handleEngagementWithLiveSuccess() {
        val engagement = KEngagement("point", "flavour")
        val listener = mock<EngageListener<KEngagement>>()
        val result = JSONObject().put("result", 1)
        whenever(network.engage(any(), any())).thenAnswer {
            (it.arguments[1] as RequestListener<JSONObject>)
                    .onCompleted(Response(200, false, null, result, null))
            null
        }
        
        uut.handleEngagement(
                engagement,
                listener,
                "userId",
                "sessionId",
                0,
                "sdkVersion",
                "platform")
        
        verify(engagements).put(same(engagement))
        verify(listener).onCompleted( argThat {
            assertThat(this).isSameAs(engagement)
            assertThat(this.statusCode).isEqualTo(200)
            assertThat(this.isCached).isFalse()
            assertThat(this.json.toString()).isEqualTo(result.toString())
            assertThat(this.error).isNull()
            true
        })
    }
    
    @Test
    fun handleEngagementWithArchiveHit() {
        val engagement = KEngagement("point", "flavour")
        val listener = mock<EngageListener<KEngagement>>()
        val archived = JSONObject().put("archived", 1)
        whenever(network.engage(any(), any())).thenAnswer {
            (it.arguments[1] as RequestListener<*>).onError(Exception())
            null
        }
        whenever(engagements.get(same(engagement))).then { archived }
        
        uut.handleEngagement(
                engagement,
                listener,
                "userId",
                "sessionId",
                0,
                "sdkVersion",
                "platform")
        
        val cached = JSONObject(archived.toString())
                .put("isCachedResponse", true)
        verify(engagements, never()).put(any())
        verify(listener).onCompleted(argThat {
            assertThat(this).isSameAs(engagement)
            assertThat(this.statusCode).isEqualTo(200)
            assertThat(this.isCached).isTrue()
            assertThat(this.json.toString()).isEqualTo(cached.toString())
            assertThat(this.error).isNull()
            true
        })
    }
    
    @Test
    fun handleEngagementWithArchiveMiss() {
        val engagement = KEngagement("point", "flavour")
        val listener = mock<EngageListener<KEngagement>>()
        val cause = Exception()
        whenever(network.engage(any(), any())).thenAnswer {
            (it.arguments[1] as RequestListener<*>).onError(cause)
            null
        }
        whenever(engagements.get(same(engagement))).then { null }
        
        uut.handleEngagement(
                engagement,
                listener,
                "userId",
                "sessionId",
                0,
                "sdkVersion",
                "platform")
        
        verify(listener).onError(same(cause))
    }
    
    @Test
    fun itemsClearedOnSuccess() {
        withStoreEvents(listOf("0")) {
            withListeners { onCompleted(Response(200, false, null, null, null)) }
            
            uut.start(0, 1)
            Thread.sleep(500)
            
            verify(this[0]).close(same(CloseableIterator.Mode.ALL))
        }
    }
    
    @Test
    fun itemsClearedOnCorruption() {
        withStoreEvents(listOf("0")) {
            withListeners { onCompleted(Response(400, false, null, null, null)) }
            
            uut.start(0, 1)
            Thread.sleep(500)
            
            verify(this[0]).close(same(CloseableIterator.Mode.ALL))
        }
    }
    
    @Test
    fun itemsNotClearedOnFailure() {
        withStoreEvents(listOf("0")) {
            withListeners { onError(Exception()) }
            
            uut.start(0, 1)
            Thread.sleep(500)
            
            verify(this[0]).close(same(CloseableIterator.Mode.NONE))
        }
    }
    
    @Test
    fun closesWhenNoItems() {
        withStoreEvents(listOf()) {
            uut.start(0, 1)
            Thread.sleep(500)
            
            verify(this[0]).close(same(CloseableIterator.Mode.NONE))
        }
    }
    
    @Test
    fun pausesOnUnavailableItems() {
        withStoreEventsAndAvailability(
                listOf("0", "1", "2"),
                listOf(true, false, true)) {
            withListeners { onCompleted(Response(200, false, null, null, null)) }
            
            uut.start(0, 1)
            Thread.sleep(500)
            
            verify(this, times(2)).next()
            verify(this).close(same(CloseableIterator.Mode.UP_TO_CURRENT))
        }
    }
    
    @Test
    fun skipsMissingItems() {
        withStoreEventsAndAvailability(listOf("0", null, "2")) {
            withListeners { onCompleted(Response(200, false, null, null, null)) }
            
            uut.start(0, 1)
            Thread.sleep(500)
            
            verify(network).collect(
                    argThat { toString() == "{\"eventList\":[0,2]}" },
                    any())
            verify(this, times(3)).next()
            verify(this).close(same(CloseableIterator.Mode.ALL))
        }
    }
    
    private fun withStoreEvents(
            vararg items: List<String>,
            block: List<CloseableIterator<EventStoreItem>>.() -> Unit = {}) {
        var stubbing = whenever(events.items())
        block.invoke(items.map {
            spy(StoredEventsIterator(it)).apply {
                stubbing = stubbing.thenReturn(this)
            }
        })
    }
    
    private fun withStoreEventsAndAvailability(
            values: List<String?>,
            availabilities: List<Boolean> = listOf(),
            block: CloseableIterator<EventStoreItem>.() -> Unit = {}) {
        spy(StoredEventsIterator(values, availabilities)).apply {
            whenever(events.items()).thenReturn(this)
            block.invoke(this)
        }
    }
    
    private fun withListeners(action: RequestListener<Any>.() -> Unit) {
        whenever(network.collect(any(), any())).thenAnswer {
            action.invoke(it.arguments[1] as RequestListener<Any>)
            null
        }
    }
    
    open inner class StoredEventsIterator(
            backingValues: List<String?>,
            backingAvailabilities: List<Boolean> = listOf()) :
            CloseableIterator<EventStoreItem> {
        
        private val backing: List<EventStoreItemImpl> = (0 until backingValues.size)
                .map { EventStoreItemImpl(
                        backingValues[it],
                        backingAvailabilities.getOrElse(it, { true }))
        }
        private var index = -1

        override fun hasNext() = index < backing.size - 1
        override fun next() = backing[++index]
        override fun close(mode: CloseableIterator.Mode) {}
        override fun remove() {}
    }
    
    open inner class EventStoreItemImpl(
            private val value: String?,
            private val availability: Boolean) : EventStoreItem {
        override fun available() = availability
        override fun get() = value
    }
}
