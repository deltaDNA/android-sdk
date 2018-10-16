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

import android.app.Activity
import android.content.Intent
import com.deltadna.android.sdk.listeners.ImageMessageResultListener
import com.github.salomonbrys.kotson.jsonObject
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageMessageActivityTest {
    
    @Test
    fun `handle result invokes correct callback method`() {
        with(mock<ImageMessageResultListener>()) {
            ImageMessageActivity.handleResult(
                    Activity.RESULT_CANCELED,
                    Intent(),
                    this)
            
            verify(this).onCancelled()
        }
        
        with(mock<ImageMessageResultListener>()) {
            val params = "${jsonObject("a" to 1)}"
            ImageMessageActivity.handleResult(
                    Activity.RESULT_OK,
                    Intent().putExtra("action", ImageMessage.Action(jsonObject(
                                    "type" to "type",
                                    "value" to "action")
                                    .convert()))
                            .putExtra("params", params),
                    this)
            
            verify(this).onAction(eq("action"), argThat { "$this" == params })
        }
        
        with(mock<ImageMessageResultListener>()) {
            val params = "${jsonObject("a" to 1)}"
            ImageMessageActivity.handleResult(
                    Activity.RESULT_OK,
                    Intent().putExtra("action", ImageMessage.LinkAction(jsonObject(
                                    "type" to "type",
                                    "value" to "link")
                                    .convert()))
                            .putExtra("params", params),
                    this)
            
            verify(this).onLink(eq("link"), argThat { "$this" == params })
        }
        
        with(mock<ImageMessageResultListener>()) {
            val params = "${jsonObject("a" to 1)}"
            ImageMessageActivity.handleResult(
                    Activity.RESULT_OK,
                    Intent().putExtra("action", ImageMessage.StoreAction(jsonObject(
                                    "type" to "type",
                                    "value" to jsonObject("ANDROID" to "store"))
                                    .convert(), null))
                            .putExtra("params", params),
                    this)
            
            verify(this).onStore(eq("store"), argThat { "$this" == params })
        }
    }
}
