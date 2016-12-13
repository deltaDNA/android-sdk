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

import android.os.Build
import com.deltadna.android.sdk.helpers.Settings
import com.deltadna.android.sdk.listeners.SessionListener
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml",
        sdk = intArrayOf(Build.VERSION_CODES.M))
class DDNATest {
    
    private var uut = createUut()
    
    @Before
    fun before() {
        uut = createUut()
    }
    
    @Test
    fun sessionChanging() {
        val s1 = uut.newSession().sessionId
        assertThat(s1).isNotEmpty()
        
        val s2 = uut.newSession().sessionId
        assertThat(s2).isNotEmpty()
        assertThat(s2).isNotEqualTo(s1)
    }
    
    @Test
    fun sessionNotifications() {
        with(mock<SessionListener>()) {
            uut.register(this)
            uut.newSession()
            uut.newSession()
            
            verify(this, times(2)).onSessionUpdated()
            
            uut.unregister(this)
            uut.newSession()
            
            verifyNoMoreInteractions(this)
        }
    }
    
    private fun createUut() = DDNA(
            RuntimeEnvironment.application,
            "environmentKey",
            "collectUrl",
            "engageUrl",
            Settings(),
            null,
            null,
            null,
            null)
}
