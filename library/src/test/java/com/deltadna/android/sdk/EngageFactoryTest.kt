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

import com.deltadna.android.sdk.listeners.EngageListener
import com.nhaarman.mockito_kotlin.*
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EngageFactoryTest {
    
    private var analytics = mock<DDNA>()
    private var uut = EngageFactory(analytics)
    
    @Before
    fun before() {
        analytics = mock<DDNA>()
        uut = EngageFactory(analytics)
    }
    
    @Test
    fun requestGameParameters() {
        uut.requestGameParameters("decisionPoint", mock())
        verify(analytics).requestEngagement(
                argThat<Engagement<*>> {
                    getDecisionPoint() == "decisionPoint" &&
                    params.isEmpty },
                any())
        
        uut.requestGameParameters("decisionPoint", Params().put("a", 1), mock())
        verify(analytics).requestEngagement(
                argThat<Engagement<*>> {
                    getDecisionPoint() == "decisionPoint" &&
                    params.json.toString() == "{\"a\":1}"
                },
                any())
    }
    
    @Test
    fun requestImageMessage() {
        uut.requestImageMessage("decisionPoint", mock())
        verify(analytics).requestEngagement(
                argThat<Engagement<*>> {
                    getDecisionPoint() == "decisionPoint" &&
                    params.isEmpty },
                any())
        
        uut.requestImageMessage("decisionPoint", Params().put("a", 1), mock())
        verify(analytics).requestEngagement(
                argThat<Engagement<*>> {
                    getDecisionPoint() == "decisionPoint" &&
                    params.json.toString() == "{\"a\":1}"
                },
                any())
    }
    
    @Test
    fun callbacks() {
        var callback = mock<EngageFactory.Callback<JSONObject>>()
        doAnswer {
            (it.arguments[1] as EngageListener<Engagement<*>>).onError(mock())
            analytics
        }.whenever(analytics).requestEngagement(any<Engagement<*>>(), any())
        
        uut.requestGameParameters("decisionPoint", callback)
        
        verify(callback).onCompleted(argThat { length() == 0 })
        
        callback = mock()
        doAnswer {
            (it.arguments[1] as EngageListener<Engagement<*>>).onCompleted(
                    mock<Engagement<*>>().apply {
                        whenever(this.getJson()).then { JSONObject("{}") }})
            analytics
        }.whenever(analytics).requestEngagement(any<Engagement<*>>(), any())
        
        uut.requestGameParameters("decisionPoint", callback)
        
        verify(callback).onCompleted(argThat { length() == 0 })
        
        callback = mock()
        doAnswer {
            (it.arguments[1] as EngageListener<Engagement<*>>).onCompleted(
                    mock<Engagement<*>>().apply {
                        whenever(this.getJson()).then { JSONObject("{\"parameters\":{\"a\":1}}") }})
            analytics
        }.whenever(analytics).requestEngagement(any<Engagement<*>>(), any())
        
        uut.requestGameParameters("decisionPoint", callback)
        
        verify(callback).onCompleted(argThat { toString() == "{\"a\":1}" })
    }
}
