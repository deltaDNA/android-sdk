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

import com.deltadna.android.sdk.net.Response
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject

@RunWith(JUnit4::class)
class EngagementTest {
    
    @Test
    fun flavour() {
        assertThat(KEngagement("point").flavour).isNull()
        assertThat(KEngagement("point", null).flavour).isNull()
        assertThat(KEngagement("point", "flavour").flavour).isEqualTo("flavour")
    }
    
    @Test
    fun response() {
        with(KEngagement("point")) {
            assertThat(statusCode).isEqualTo(0)
            assertThat(isSuccessful).isFalse()
            assertThat(json).isNull()
            assertThat(error).isNull()
            
            setResponse(Response(200, false, null, JSONObject(), null))
            assertThat(statusCode).isEqualTo(200)
            assertThat(isCached).isFalse()
            assertThat(isSuccessful).isTrue()
            assertThat(json).isNotNull()
            assertThat(error).isNull()
            
            setResponse(Response(200, true, null, JSONObject(), null))
            assertThat(statusCode).isEqualTo(200)
            assertThat(isCached).isTrue()
            assertThat(isSuccessful).isTrue()
            assertThat(json).isNotNull()
            assertThat(error).isNull()
            
            setResponse(Response(300, false, null, null, "error"))
            assertThat(statusCode).isEqualTo(300)
            assertThat(isCached).isFalse()
            assertThat(isSuccessful).isFalse()
            assertThat(json).isNull()
            assertThat(error).isNotNull()
            
            // cached response
            setResponse(Response(300, false, null, JSONObject(), "error"))
            assertThat(statusCode).isEqualTo(300)
            assertThat(isCached).isFalse()
            assertThat(isSuccessful).isTrue()
            assertThat(json).isNotNull()
            assertThat(error).isNotNull()
        }
    }
    
    @Test
    fun getDecisionPoint() {
        assertThat(KEngagement("point").decisionPoint).isEqualTo("point")
    }
    
    private class KEngagement : Engagement<KEngagement> {
        constructor(point: String) : super(point)
        constructor(point: String, flavour: String?) : super(point, flavour)
    }
}
