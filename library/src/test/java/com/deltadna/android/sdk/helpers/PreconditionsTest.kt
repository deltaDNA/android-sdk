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

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PreconditionsTest {
    
    @get:Rule
    val thrown = ExpectedException.none()
    
    @Test
    fun checkArg() {
        Preconditions.checkArg(true, "msg")
    }
    
    @Test
    fun checkArgFailure() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage("msg")
        Preconditions.checkArg(false, "msg")
    }
    
    @Test
    fun checkString() {
        Preconditions.checkString("value", "msg")
    }
    
    @Test
    fun checkStringNull() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage("msg")
        Preconditions.checkString(null, "msg")
    }
    
    @Test
    fun checkStringEmpty() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage("msg")
        Preconditions.checkString("", "msg")
    }
}
