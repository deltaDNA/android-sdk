/*
 * Copyright (c) 2017 deltaDNA Ltd. All rights reserved.
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

package com.deltadna.android.sdk.notifications

import android.content.Context
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PushMessageTest {
    
    private val app by lazy { RuntimeEnvironment.application }
    
    @After
    fun after() {
        scrubMetaData()
    }
    
    @Test
    fun from() {
        assertThat(msg(from = "sender").from).isEqualTo("sender")
    }
    
    @Test
    fun data() {
        val data = mapOf("a" to "1", "b" to "1")
        assertThat(msg(data = data).data).isEqualTo(data)
    }
    
    @Test
    fun id() {
        assertThat(msg().id).isEqualTo(-1)
        assertThat(msg(data = mapOf("_ddCampaign" to "1")).id).isEqualTo(1)
    }
    
    @Test()
    fun icon() {
        assertThat(msg().icon).isEqualTo(R.drawable.ddna_ic_stat_logo)
        
        injectMetaData(Bundle(1).apply {
            putString(MetaData.NOTIFICATION_ICON, "ddna_ic_stat_logo")
        })
        assertThat(msg().icon).isEqualTo(R.drawable.ddna_ic_stat_logo)
    }
    
    @Test()
    fun title() {
        assertThat(msg(data = mapOf(PushMessage.TITLE to "1")).title).isEqualTo("1")
        
        injectMetaData(Bundle(1).apply {
            putString(MetaData.NOTIFICATION_TITLE, "2")
        })
        assertThat(msg().title).isEqualTo("2")
        
        injectMetaData(Bundle(1).apply {
            putInt(MetaData.NOTIFICATION_TITLE, android.R.string.ok)
        })
        assertThat(msg().title).isEqualTo(app.getString(android.R.string.ok))
    }
    
    @Test
    fun message() {
        assertThat(msg().message).isEmpty()
        assertThat(msg(data = mapOf(PushMessage.MESSAGE to "1")).message).isEqualTo("1")
    }
    
    private fun msg(
            context: Context = app,
            from: String = "",
            data: Map<String, String> = emptyMap()) =
            PushMessage(context, from, data)
    
    private fun injectMetaData(values: Bundle) {
        MetaData.values = values
    }
    
    private fun scrubMetaData() {
        MetaData.values = null
    }
}
