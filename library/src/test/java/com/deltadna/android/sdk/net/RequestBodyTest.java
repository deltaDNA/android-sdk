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

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class RequestBodyTest {
    
    @Test
    public void ctor() {
        final String type = "type";
        final byte[] content = new byte[] {0};
        final RequestBody uut = new RequestBody(type, content);

        assertThat(uut.type).isEqualTo(type);
        assertThat(uut.content).isEqualTo(content);
    }
    
    @Test
    public void fill() throws IOException {
        final RequestBody uut = new RequestBody("type", new byte[] {0});
        final HttpURLConnection conn = mock(HttpURLConnection.class);
        final OutputStream os = mock(OutputStream.class);
        when(conn.getOutputStream()).thenReturn(os);
        
        uut.fill(conn);
        
        verify(conn).setFixedLengthStreamingMode(eq(uut.content.length));
        verify(conn).setRequestProperty(eq("Content-Type"), eq(uut.type));
        verify(os).write(eq(uut.content));
    }
    
    @Test
    public void json() throws JSONException, UnsupportedEncodingException {
        final RequestBody uut =
                RequestBody.json(new JSONObject().put("field", 1));
        
        assertThat(uut.type).isEqualTo("application/json; charset=utf-8");
        assertThat(uut.content).isEqualTo("{\"field\":1}".getBytes("UTF-8"));
    }
}
