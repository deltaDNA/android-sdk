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

import com.deltadna.android.sdk.helpers.ClientInfo
import com.deltadna.android.sdk.net.Response
import com.github.salomonbrys.kotson.jsonObject
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.mock
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

@RunWith(RobolectricTestRunner::class)
class ImageMessageTest {
    
    private lateinit var ddna: DDNA
    
    @Before
    fun before() {
        ddna = mock()
        
        DDNA.initialise(DDNA.Configuration(
                RuntimeEnvironment.application, "envKey", "collUrl", "engUrl"))
        DDNA.instance().inject(ddna)
    }
    
    @After
    fun after() {
        DDNA.instance().scrub()
    }
    
    @Test
    fun createWithImage() {
        with(KEngagement("point")) {
            setResponse(Response(200, false, null, IMAGE, null))
            
            assertThat(ImageMessage.create(this)).isNotNull()
        }
    }
    
    @Test
    fun createWithFailure() {
        with(KEngagement("point")) {
            setResponse(Response(300, false, null, null, "error"))
            
            assertThat(ImageMessage.create(this)).isNull()
        }
    }
    
    @Test
    fun createWithoutImage() {
        with(KEngagement("point")) {
            setResponse(Response(200, false, null, JSONObject(), null))
            
            assertThat(ImageMessage.create(this)).isNull()
        }
    }
    
    @Test
    fun `is serialisable`() {
        ObjectOutputStream(ByteArrayOutputStream())
                .writeObject(ImageMessage(IMAGE))
    }
    
    @Test
    fun `correct action is created based on type`() {
        assertThat(ImageMessage.BaseAction.create(jsonObject(
                "type" to "dismiss")
                .convert(), null)).isInstanceOf(ImageMessage.DismissAction::class.java)
        assertThat(ImageMessage.BaseAction.create(jsonObject(
                "type" to "action",
                "value" to "action")
                .convert(), null)).isInstanceOf(ImageMessage.Action::class.java)
        assertThat(ImageMessage.BaseAction.create(jsonObject(
                "type" to "link",
                "value" to "link")
                .convert(), null)).isInstanceOf(ImageMessage.LinkAction::class.java)
        assertThat(ImageMessage.BaseAction.create(jsonObject(
                "type" to "store",
                "value" to jsonObject("ANDROID" to "android"))
                .convert(), null)).isInstanceOf(ImageMessage.StoreAction::class.java)
    }
    
    @Test
    fun `action is created with type`() {
        assertThat(ImageMessage.BaseAction.create(jsonObject(
                "type" to "action").convert(), null)!!.type)
                .isEqualTo("action")
    }
    
    @Test
    fun `action and link actions return value`() {
        assertThat(ImageMessage.Action(jsonObject(
                "type" to "action", "value" to "a").convert()).value)
                .isEqualTo("a")
        assertThat(ImageMessage.LinkAction(jsonObject(
                "type" to "link", "value" to "b").convert()).value)
                .isEqualTo("b")
    }
    
    @Test
    fun `store actions return value depending on platform`() {
        with(null) {
            assertThat(ImageMessage.StoreAction(jsonObject(
                    "type" to "store",
                    "value" to jsonObject(
                            "ANDROID" to "a",
                            "AMAZON" to "b",
                            "IOS" to "c"))
                    .convert(), this).value).isEqualTo("a")
        }
        
        with(ClientInfo.PLATFORM_ANDROID) {
            assertThat(ImageMessage.StoreAction(jsonObject(
                    "type" to "store",
                    "value" to jsonObject(
                            "ANDROID" to "a",
                            "AMAZON" to "b",
                            "IOS" to "c"))
                    .convert(), this).value).isEqualTo("a")
        }
        
        with(ClientInfo.PLATFORM_AMAZON) {
            assertThat(ImageMessage.StoreAction(jsonObject(
                    "type" to "store",
                    "value" to jsonObject(
                            "ANDROID" to "a",
                            "AMAZON" to "b",
                            "IOS" to "c"))
                    .convert(), this).value).isEqualTo("b")
        }
    }
    
    @Test
    fun `actions are serialisable`() {
        ObjectOutputStream(ByteArrayOutputStream())
                .writeObject(ImageMessage.DismissAction(jsonObject(
                        "type" to "dismiss")
                        .convert()))
        ObjectOutputStream(ByteArrayOutputStream())
                .writeObject(ImageMessage.Action(jsonObject(
                        "type" to "action",
                        "value" to "action")
                        .convert()))
        ObjectOutputStream(ByteArrayOutputStream())
                .writeObject(ImageMessage.LinkAction(jsonObject(
                        "type" to "link",
                        "value" to "link")
                        .convert()))
        ObjectOutputStream(ByteArrayOutputStream())
                .writeObject(ImageMessage.StoreAction(jsonObject(
                        "type" to "store",
                        "value" to jsonObject("ANDROID" to "android"))
                        .convert(), null))
    }
    
    private class KEngagement(point: String) : Engagement<KEngagement>(point)
    
    private companion object {
        
        val IMAGE = JSONObject(mapOf(
                "transactionID" to "1898738848054116400",
                "image" to mapOf(
                        "width" to 512,
                        "height" to 256,
                        "format" to "png",
                        "spritemap" to mapOf(
                                "background" to mapOf(
                                        "x" to 2,
                                        "y" to 74,
                                        "width" to 320,
                                        "height" to 180),
                                "buttons" to listOf(
                                        mapOf(  "x" to 2,
                                                "y" to 38,
                                                "width" to 160,
                                                "height" to 34),
                                        mapOf(  "x" to 2,
                                                "y" to 2,
                                                "width" to 160,
                                                "height" to 34))),
                        "layout" to mapOf(
                                "landscape" to mapOf(
                                        "background" to mapOf(
                                                "contain" to mapOf(
                                                        "halign" to "center",
                                                        "valign" to "center",
                                                        "left" to "10%",
                                                        "right" to "10%",
                                                        "top" to "0px",
                                                        "bottom" to "0%"),
                                                "action" to mapOf(
                                                        "type" to "dismiss")),
                                        "buttons" to listOf(
                                                mapOf(  "x" to 160,
                                                        "y" to 145,
                                                        "action" to mapOf(
                                                                "type" to "action",
                                                                "value" to "POWERUP")),
                                                mapOf(  "x" to 0,
                                                        "y" to 145,
                                                        "action" to mapOf(
                                                                "type" to "dismiss"))))),
                        "shim" to mapOf(
                                "mask" to "dimmed",
                                "action" to mapOf(
                                        "type" to "dismiss")),
                        "url" to "http://download.deltadna.net/engagements/f20f3c4fc19d4d49bc365b472770c065.png"),
                "eventParams" to mapOf(
                        "platform" to "WEB",
                        "responseTransactionID" to 2344672516611522560,
                        "responseDecisionpointName" to "Fourteen",
                        "responseEngagementID" to 15247,
                        "responseEngagementName" to "15247",
                        "responseEngagementType" to "TARGETING",
                        "responseVariantName" to "20389",
                        "responseMessageSequence" to 1),
                "parameters" to mapOf(
                        "powerUpName" to "MoHawk")))
    }
}
