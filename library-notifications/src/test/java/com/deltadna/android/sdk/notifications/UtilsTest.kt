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

package com.deltadna.android.sdk.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.same
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.app.ActivityManager.RunningAppProcessInfo as RAPI
import org.robolectric.RuntimeEnvironment as RE

@RunWith(RobolectricTestRunner::class)
class UtilsTest {
    
    @Test
    fun convertMap() {
        // Bundle doesn't implement equals() properly
        assertThat(Utils.convert(mapOf("a" to "1", "b" to "2")).toString())
                .isEqualTo(Bundle(2).apply {
                    putString("a", "1")
                    putString("b", "2")
                }.toString())
    }
    
    @Test
    fun convertBundle() {
        with(Bundle(2)) {
            putBoolean("1", true)
            putString("2", "string")
            
            assertThat(Utils.convert(this))
                    .isEqualTo("{\"1\":true,\"2\":\"string\"}")
        }
    }
    
    @Test
    fun wrapWithReceiverNotSet() {
        with(mock<Intent>()) {
            val result = Utils.wrapWithReceiver(mock(), this)
            
            assertThat(result).isSameAs(this)
            verifyZeroInteractions(this)
        }
    }
    
    @Test
    fun wrapWithReceiverSet() {
        val context = mock<Context>()
        val intent = mock<Intent>()
        
        DDNANotifications.setReceiver(EventReceiver::class.java)
        val result = Utils.wrapWithReceiver(context, intent)
        DDNANotifications.setReceiver(null)
        
        assertThat(result).isSameAs(intent)
        verify(intent).setClass(same(context), same(EventReceiver::class.java))
    }
}
