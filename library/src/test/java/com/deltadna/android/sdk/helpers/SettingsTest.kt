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

package com.deltadna.android.sdk.helpers

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SettingsTest {
    
    private var uut = Settings()
    
    @Before
    fun before() {
        uut = Settings()
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun sessionTimeoutInvalid() {
        uut.sessionTimeout = -1
    }
    
    @Test
    fun sessionTimeout() {
        assertThat(uut.sessionTimeout).isEqualTo(5 * 60 * 1000)
        
        uut.sessionTimeout = 0
        assertThat(uut.sessionTimeout).isEqualTo(0)
        
        uut.sessionTimeout = 1000
        assertThat(uut.sessionTimeout).isEqualTo(1000)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun httpRequestMaxRetriesInvalid() {
        uut.httpRequestMaxRetries = -1
    }
    
    @Test
    fun httpRequestMaxRetries() {
        assertThat(uut.httpRequestMaxRetries).isEqualTo(0)
        
        uut.httpRequestMaxRetries = 5
        assertThat(uut.httpRequestMaxRetries).isEqualTo(5)
        
        uut.httpRequestMaxRetries = 0
        assertThat(uut.httpRequestMaxRetries).isEqualTo(0)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun httpRequestRetryDelayInvalid() {
        uut.httpRequestRetryDelay = -1
    }
    
    @Test
    fun httpRequestRetryDelay() {
        assertThat(uut.httpRequestRetryDelay).isEqualTo(2)
        
        uut.httpRequestRetryDelay = 5
        assertThat(uut.httpRequestRetryDelay).isEqualTo(5)
        
        uut.httpRequestRetryDelay = 0
        assertThat(uut.httpRequestRetryDelay).isEqualTo(0)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun httpRequestCollectTimeoutInvalid() {
        uut.httpRequestCollectTimeout = -1
    }
    
    @Test
    fun httpRequestCollectTimeout() {
        assertThat(uut.httpRequestCollectTimeout).isEqualTo(55)
        
        uut.httpRequestCollectTimeout = 5
        assertThat(uut.httpRequestCollectTimeout).isEqualTo(5)
        
        uut.httpRequestCollectTimeout = 0
        assertThat(uut.httpRequestCollectTimeout).isEqualTo(0)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun httpRequestEngageTimeoutInvalid() {
        uut.httpRequestEngageTimeout = -1
    }
    
    @Test
    fun httpRequestEngageTimeout() {
        assertThat(uut.httpRequestEngageTimeout).isEqualTo(5)
        
        uut.httpRequestEngageTimeout = 10
        assertThat(uut.httpRequestEngageTimeout).isEqualTo(10)
        
        uut.httpRequestEngageTimeout = 0
        assertThat(uut.httpRequestEngageTimeout).isEqualTo(0)
    }

    @Test
    fun imageMessageAutoNavigateLinkEnabled() {
        assertThat(uut.imageMessageAutoNavigateLinkEnabled).isEqualTo(true)

        uut.imageMessageAutoNavigateLinkEnabled = false

        assertThat(uut.imageMessageAutoNavigateLinkEnabled).isEqualTo(false)
    }
}
