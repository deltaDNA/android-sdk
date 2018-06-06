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

import com.google.common.collect.Range
import com.google.common.truth.Truth.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.*

@RunWith(RobolectricTestRunner::class)
class PreferencesTest {
    
    private val application by lazy { RuntimeEnvironment.application }
    
    private lateinit var uut: Preferences
    
    @Before
    fun before() {
        uut = Preferences(application)
    }
    
    @After
    fun after() {
        uut.clear()
    }
    
    @Test
    fun firstSession() {
        assertThat(uut.firstSession).isIn(Range.closed(
                Date(System.currentTimeMillis() - 100),
                Date(System.currentTimeMillis() + 100)))
        
        with(Date(1)) {
            uut.firstSession = this
            assertThat(uut.firstSession).isEqualTo(this)
        }
    }
    
    @Test
    fun lastSession() {
        assertThat(uut.lastSession).isIn(Range.closed(
                Date(System.currentTimeMillis() - 100),
                Date(System.currentTimeMillis() + 100)))
        
        with(Date(1)) {
            uut.lastSession = this
            assertThat(uut.lastSession).isEqualTo(this)
        }
    }
    
    @Test
    fun forgetMe() {
        assertThat(uut.isForgetMe).isFalse()
        uut.isForgetMe = true
        assertThat(uut.isForgetMe).isTrue()
    }
    
    @Test
    fun forgotten() {
        assertThat(uut.isForgotten).isFalse()
        uut.isForgotten = true
        assertThat(uut.isForgotten).isTrue()
    }
    
    @Test
    fun clearRunAndSessionKeys() {
        uut.firstRun = 0
        uut.firstSession = Date(1)
        uut.lastSession = Date(2)
        
        uut.clearRunAndSessionKeys()
        
        assertThat(uut.firstRun).isEqualTo(1)
        assertThat(uut.firstSession).isNotEqualTo(Date(1))
        assertThat(uut.lastSession).isNotEqualTo(Date(2))
    }
    
    @Test
    fun clearForgetMeAndForgotten() {
        uut.userId = "userId"
        uut.isForgetMe = true
        uut.isForgotten = true
        
        uut.clearForgetMeAndForgotten()
        
        assertThat(uut.userId).isEqualTo("userId")
        assertThat(uut.isForgetMe).isFalse()
        assertThat(uut.isForgotten).isFalse()
    }
}