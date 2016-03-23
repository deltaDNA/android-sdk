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

import com.deltadna.android.sdk.exceptions.BadRequestException
import com.deltadna.android.sdk.helpers.EngageArchive
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
        withListeners() { onSuccess(null) }
        
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
            withListeners() { onSuccess(null) }
            
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
        val event = JSONObject().put("event", 1)
        val result = JSONObject().put("result", 1)
        val listener = mock<RequestListener<JSONObject>>()
        whenever(network.engage(same(event), any())).thenAnswer {
            (it.arguments[1] as RequestListener<Response<JSONObject>>)
                    .onSuccess(Response(200, null, result))
            null
        }
        
        uut!!.handleEngagement("point", "flavour", event, listener)
        
        verify(archive).put(eq("point"), eq("flavour"), eq(result.toString()))
        verify(listener).onSuccess(same(result))
    }
    
    @Test
    fun handleEngagementWithArchiveHit() {
        val event = JSONObject().put("event", 1)
        val archived = JSONObject().put("archived", 1)
        val listener = mock<RequestListener<JSONObject>>()
        whenever(network.engage(same(event), any())).thenAnswer {
            (it.arguments[1] as RequestListener<*>).onFailure(Exception())
            null
        }
        whenever(archive.contains("point", "flavour")).thenReturn(true)
        whenever(archive.get("point", "flavour")).thenReturn(archived.toString())
        
        uut!!.handleEngagement("point", "flavour", event, listener)
        
        val cached = JSONObject(archived.toString()).put("isCachedResponse", true)
        verify(archive, never()).put(
                eq("point"), eq("flavour"), eq(cached.toString()))
        verify(listener).onSuccess(argThat {
            assertThat(this).isInstanceOf(JSONObject::class.java)
            assertThat(this.toString()).isEqualTo(cached.toString())
            true
        })
    }
    
    @Test
    fun handleEngagementWithArchiveMiss() {
        val event = JSONObject().put("event", 1)
        val cause = Exception()
        val listener = mock<RequestListener<JSONObject>>()
        whenever(network.engage(same(event), any())).thenAnswer {
            (it.arguments[1] as RequestListener<*>).onFailure(cause)
            null
        }
        whenever(archive.contains("point", "flavour")).thenReturn(false)
        
        uut!!.handleEngagement("point", "flavour", event, listener)
        
        verify(listener).onFailure(same(cause))
    }
    
    @Test
    fun itemsClearedOnSuccess() {
        withStoreEvents(listOf("0")) {
            withListeners { onSuccess(null) }
            
            uut!!.start(0, 1)
            Thread.sleep(500)
            
            verify(this[0]).close(eq(true))
        }
    }
    
    @Test
    fun itemsClearedOnCorruption() {
        withStoreEvents(listOf("0")) {
            withListeners() {
                onFailure(BadRequestException(Response<String>(400, null, null)))
            }
            
            uut!!.start(0, 1)
            Thread.sleep(500)
            
            verify(this[0]).close(eq(true))
        }
    }
    
    @Test
    fun itemsNotClearedOnFailure() {
        withStoreEvents(listOf("0")) {
            withListeners { onFailure(Exception()) }
            
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
            withListeners { onSuccess(null) }
            
            uut!!.start(0, 1)
            Thread.sleep(500)
            
            verify(this, times(2)).next()
            verify(this).close(eq(true))
        }
    }
    
    @Test
    fun skipsMissingItems() {
        withStoreEventsAndAvailability(listOf("0", null, "2")) {
            withListeners { onSuccess(null) }
            
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
    
    private fun withListeners(action: RequestListener<*>.() -> Unit) {
        whenever(network.collect(any(), any())).thenAnswer {
            action.invoke(it.arguments[1] as RequestListener<*>)
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
}
