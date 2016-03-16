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

package com.deltadna.android.sdk.net;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.HttpURLConnection;
import java.net.ProtocolException;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public final class RequestMethodTest {
    
    private HttpURLConnection connection;
    
    @Before
    public void before() {
        connection = mock(HttpURLConnection.class);
    }
    
    @After
    public void after() {
        connection = null;
    }
    
    @Test
    public void setGet() throws ProtocolException {
        RequestMethod.GET.set(connection);
        
        verify(connection).setRequestMethod(eq(RequestMethod.GET.name()));
        verify(connection).setDoOutput(eq(false));
        verify(connection).setDoInput(eq(true));
    }
    
    @Test
    public void setPost() throws ProtocolException {
        RequestMethod.POST.set(connection);
        
        verify(connection).setRequestMethod(eq(RequestMethod.POST.name()));
        verify(connection).setDoOutput(eq(true));
        verify(connection).setDoInput(eq(true));
    }
}
