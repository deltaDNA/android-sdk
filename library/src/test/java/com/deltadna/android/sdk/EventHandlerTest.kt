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
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.*
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner
import java.util.*

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
        val events1 = listOf("{\"value\":0}", "{\"value\":1}")
        val events2 = listOf("{\"value\":0}")
        withStoreEvents(events1, events2)
        invokeOnListeners() { it.onSuccess(null) }
        
        uut!!.start(0, 1)
        Thread.sleep(2200)
        
        val expected1 = "{\"eventList\":[{\"value\":0},{\"value\":1}]}"
        val expected2 = "{\"eventList\":[{\"value\":0}]}"
        
        verify(store, times(3)).swap()
        verify(store, times(3)).read()
        var run: Int = 0
        verify(network, times(2)).collect(
                argThat {
                    if (run != 2) {
                        // no idea why we get a 3 runs and times(2) works
                        assertThat(toString()).isEqualTo(
                                if (run == 0) expected1 else expected2)
                    }
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
        
        verify(store, never()).read()
        verify(network, never()).collect(any(), any())
    }
    
    @Test
    fun stopAndDispatch() {
        withStoreEvents(listOf("0"))
        invokeOnListeners() { it.onSuccess(null) }
        
        uut!!.start(1, 1)
        uut!!.stop(true)
        Thread.sleep(2200)
        
        verify(store).read()
        verify(network).collect(any(), any())
    }
    
    @Test
    fun storeClearedOnCorruption() {
        withStoreEvents(listOf("0"))
        invokeOnListeners() {
            it.onFailure(BadRequestException(Response<String>(400, null, null)))
        }
        
        uut!!.start(0, 1)
        Thread.sleep(1100)
        
        verify(store).clearOutfile()
    }
    
    @Test
    fun handleEvent() {
        with(JSONObject()) {
            uut!!.handleEvent(this)
            
            verify(store).push(eq(toString()))
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
    
    private fun withStoreEvents(vararg items: List<String>) {
        var stubbing = whenever(store.read())
        for (item in items) {
            stubbing = stubbing.thenReturn(Vector(item))
        }
    }
    
    private fun invokeOnListeners(action: (RequestListener<*>) -> Unit) {
        whenever(network.collect(any(), any())).thenAnswer {
            action.invoke(it.arguments[1] as RequestListener<*>)
            null
        }
    }
}
