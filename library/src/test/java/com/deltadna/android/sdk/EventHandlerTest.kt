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

import com.deltadna.android.sdk.helpers.EngageArchive
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
import org.mockito.runners.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class EventHandlerTest {
    
    private val store = mock<EventStore>()
    private val archive = mock<EngageArchive>()
    private val network = mock<NetworkManager>()
    
    private var uut: EventHandler? = null
    
    @Before
    fun before() {
        uut = EventHandler(store, archive, network)
    }
    
    @After
    fun after() {
        uut!!.stop(false)
        uut = null
        
        reset(store)
        reset(archive)
        reset(network)
    }
    
    @Test
    fun startPeriodicUploads() {
        withStoreEvents(
                listOf("{\"value\":0}", "{\"value\":1}"),
                listOf("{\"value\":0}"))
        withListeners() { onCompleted(Response(200, null, null, null)) }
        
        uut!!.start(0, 1)
        Thread.sleep(2200)
        
        verify(store, times(3)).items()
        var run: Int = 0
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
        uut!!.start(1, 1)
        uut!!.stop(false)
        Thread.sleep(2200)
        
        verify(store, never()).items()
        verify(network, never()).collect(any(), any())
    }
    
    @Test
    fun stopAndDispatch() {
        withStoreEvents(listOf("0")) {
            withListeners() { onCompleted(Response(200, null, null, null)) }
            
            uut!!.start(1, 1)
            uut!!.stop(true)
            Thread.sleep(2200)
            
            verify(store).items()
            verify(network).collect(
                    com.nhaarman.mockito_kotlin.any(),
                    com.nhaarman.mockito_kotlin.any())
        }
    }
    
    @Test
    fun handleEvent() {
        with(JSONObject()) {
            uut!!.handleEvent(this)
            
            verify(store).add(eq(toString()))
        }
    }
    
    @Test
    fun handleEngagementWithLiveSuccess() {
        val engagement = KEngagement("point", "flavour")
        val listener = mock<EngageListener<KEngagement>>()
        val result = JSONObject().put("result", 1)
        whenever(network.engage(any(), any())).thenAnswer {
            (it.arguments[1] as RequestListener<JSONObject>)
                    .onCompleted(Response(200, null, result, null))
            null
        }
        
        uut!!.handleEngagement(
                engagement,
                listener,
                "userId",
                "sessionId",
                0,
                "sdkVersion")
        
        verify(archive).put(
                eq(engagement.name),
                eq(engagement.flavour!!),
                eq(result.toString()))
        verify(listener).onCompleted( argThat {
            assertThat(this).isSameAs(engagement)
            assertThat(response).isEqualTo(Response(200, null, result, null))
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
        whenever(archive.contains(engagement.name, engagement.flavour!!))
                .thenReturn(true)
        whenever(archive.get(engagement.name, engagement.flavour))
                .thenReturn(archived.toString())
        
        uut!!.handleEngagement(
                engagement,
                listener,
                "userId",
                "sessionId",
                0,
                "sdkVersion")
        
        val cached = JsonObjectEquals(archived.toString())
                .put("isCachedResponse", true)
        verify(archive, never()).put(
                eq(engagement.name),
                eq(engagement.flavour),
                eq(cached.toString()))
        verify(listener).onCompleted(argThat {
            assertThat(this).isSameAs(engagement)
            assertThat(Response(-1, null, cached, null)).isEqualTo(response)
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
        whenever(archive.contains(engagement.name, engagement.flavour!!))
                .thenReturn(false)
        
        uut!!.handleEngagement(
                engagement,
                listener,
                "userId",
                "sessionId",
                0,
                "sdkVersion")
        
        verify(listener).onError(same(cause))
    }
    
    @Test
    fun itemsClearedOnSuccess() {
        withStoreEvents(listOf("0")) {
            withListeners { onCompleted(Response(200, null, null, null)) }
            
            uut!!.start(0, 1)
            Thread.sleep(500)
            
            verify(this[0]).close(eq(true))
        }
    }
    
    @Test
    fun itemsClearedOnCorruption() {
        withStoreEvents(listOf("0")) {
            withListeners() { onCompleted(Response(400, null, null, null)) }
            
            uut!!.start(0, 1)
            Thread.sleep(500)
            
            verify(this[0]).close(eq(true))
        }
    }
    
    @Test
    fun itemsNotClearedOnFailure() {
        withStoreEvents(listOf("0")) {
            withListeners { onError(Exception()) }
            
            uut!!.start(0, 1)
            Thread.sleep(500)
            
            verify(this[0]).close(eq(false))
        }
    }
    
    @Test
    fun pausesOnUnavailableItems() {
        withStoreEventsAndAvailability(
                listOf("0", "1", "2"),
                listOf(true, false, true)) {
            withListeners { onCompleted(Response(200, null, null, null)) }
            
            uut!!.start(0, 1)
            Thread.sleep(500)
            
            verify(this, times(2)).next()
            verify(this).close(eq(true))
        }
    }
    
    @Test
    fun skipsMissingItems() {
        withStoreEventsAndAvailability(listOf("0", null, "2")) {
            withListeners { onCompleted(Response(200, null, null, null)) }
            
            uut!!.start(0, 1)
            Thread.sleep(500)
            
            verify(network).collect(
                    argThat { toString().equals("{\"eventList\":[0,2]}") },
                    any())
            verify(this, times(3)).next()
            verify(this).close(eq(true))
        }
    }
    
    private fun withStoreEvents(
            vararg items: List<String>,
            block: List<CloseableIterator<EventStoreItem>>.() -> Unit = {}) {
        var stubbing = whenever(store.items())
        block.invoke(items.map {
            with(spy(StoredEventsIterator(it))) {
                stubbing = stubbing.thenReturn(this)
                this
            }
        })
    }
    
    private fun withStoreEventsAndAvailability(
            values: List<String?>,
            availabilities: List<Boolean> = listOf(),
            block: CloseableIterator<EventStoreItem>.() -> Unit = {}) {
        with(spy(StoredEventsIterator(values, availabilities))) {
            whenever(store.items()).thenReturn(this)
            block.invoke(this)
            this
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
        
        private val backing: List<EventStoreItemImpl>
        private var index = -1
        
        init {
            backing = (0..backingValues.size - 1).map {
                EventStoreItemImpl(
                        backingValues[it],
                        backingAvailabilities.getOrElse(it, { true }))
            }
        }
        
        override fun hasNext() = index < backing.size - 1
        override fun next() = backing[++index]
        override fun close(clear: Boolean) {}
        override fun remove() {}
    }
    
    open inner class EventStoreItemImpl(
            private val value: String?,
            private val availability: Boolean) : EventStoreItem {
        override fun available() = availability
        override fun get() = value
    }
    
    open class KEngagement : Engagement<KEngagement> {
        constructor(point: String, flavour: String?) : super(point, flavour)
    }

    /**
     * Ugly workaround to insert an equals() implementation for our purpose.
     */
    private class JsonObjectEquals(json: String) : JSONObject(json) {
        override fun equals(other: Any?) =
            if (other is JSONObject) other.toString().equals(toString())
            else super.equals(other)
    }
}
