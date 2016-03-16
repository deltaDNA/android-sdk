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

package com.deltadna.android.sdk;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class ParamsTest {
    
    private Params uut;
    private JSONObject json;
    
    @Before
    public void before() {
        json = mock(JSONObject.class);
        uut = new Params(json);
    }
    
    @After
    public void after() {
        uut = null;
        json = null;
    }
    
    @Test
    public void put() throws JSONException {
        uut.put("key", "value");
        uut.put("keyNull", null);
        
        verify(json).put(eq("key"), eq("value"));
        verify(json).put(eq("keyNull"), isNull());
    }
    
    @Test
    public void putNested() throws JSONException {
        final JSONObject nested = new JSONObject();
        uut.put("key", new Params(nested));
        
        verify(json).put(eq("key"), same(nested));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void putKeyCannotBeNull() {
        uut.put(null, "value");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void putKeyCannotBeEmpty() {
        uut.put("", "value");
    }
    
    @Test
    public void isEmpty() {
        when(json.length()).thenReturn(0, 1);
        
        assertThat(uut.isEmpty()).isTrue();
        assertThat(uut.isEmpty()).isFalse();
    }
}
