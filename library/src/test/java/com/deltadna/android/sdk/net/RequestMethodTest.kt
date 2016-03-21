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

package com.deltadna.android.sdk.net

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.verify
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner
import java.net.HttpURLConnection

@RunWith(MockitoJUnitRunner::class)
class RequestMethodTest {
    
    private val connection = mock<HttpURLConnection>()
    
    @After
    fun after() {
        reset(connection)
    }
    
    @Test
    fun setGet() {
        RequestMethod.GET.set(connection)
        
        verify(connection).requestMethod = eq(RequestMethod.GET.name)
        verify(connection).doOutput = eq(false)
        verify(connection).doInput = eq(true)
    }
    
    @Test
    fun setPost() {
        RequestMethod.POST.set(connection)
        
        verify(connection).requestMethod = eq(RequestMethod.POST.name)
        verify(connection).doOutput = eq(true)
        verify(connection).doInput = eq(true)
    }
}
