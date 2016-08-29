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

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.app.ActivityManager.RunningAppProcessInfo as RAPI
import org.robolectric.RuntimeEnvironment as RE

@RunWith(RobolectricTestRunner::class)
class UtilsTest {
    
    @Test
    fun inForegroundWhenForegrounded() {
        withProcesses(rapi("other", false), rapi()) {
            assertThat(Utils.inForeground(this)).isTrue()
        }
    }
    
    @Test
    fun inForegroundWhenBackgrounded() {
        withProcesses(rapi("other"), rapi(fg = false)) {
            assertThat(Utils.inForeground(this)).isFalse()
        }
    }
    
    @Test
    fun inForegroundWhenOtherForegrounded() {
        withProcesses(rapi("other")) {
            assertThat(Utils.inForeground(this)).isFalse()
        }
    }
    
    @Test
    fun convert() {
        with(Bundle(2)) {
            putBoolean("1", true)
            putString("2", "string")
            
            assertThat(Utils.convert(this))
                    .isEqualTo("{\"1\":true,\"2\":\"string\"}")
        }
    }
    
    fun withProcesses(vararg list: RAPI, block: Context.() -> Unit) {
        val context = mock<Context>()
        val manager = mock<ActivityManager>()
        whenever(context.getSystemService(ACTIVITY_SERVICE)).thenReturn(manager)
        whenever(manager.runningAppProcesses).thenReturn(list.asList())
        whenever(context.packageName).thenReturn(RE.application.packageName)
        
        block.invoke(context)
    }
    
    private fun rapi(
            pkg: String = RE.application.packageName,
            fg: Boolean = true) = with(RAPI(pkg, 0, null)) {
        importance =
                if (fg) RAPI.IMPORTANCE_FOREGROUND
                else RAPI.IMPORTANCE_BACKGROUND
        this
    }
}
