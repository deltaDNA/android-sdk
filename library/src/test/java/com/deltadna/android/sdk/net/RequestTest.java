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

import com.deltadna.android.sdk.exceptions.ResponseException;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public final class RequestTest {
    
    private MockWebServer server;
    
    @Before
    public void before() throws IOException {
        server = new MockWebServer();
        server.start();
    }
    
    @After
    public void after() throws IOException {
        server.shutdown();
        server = null;
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void malformedUrl() {
        new Request.Builder<Void>().url("fail");
    }
    
    @Test
    public void get() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));
        
        final Response<Void> response = new Request.Builder<Void>()
                .get()
                .url(server.url("/get").toString())
                .build()
                .call();
        
        final RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/get");
        assertThat(request.getBodySize()).isEqualTo(0);
        
        assertThat(response).isEqualTo(
                new Response<Void>(200, new byte[] {}, null));
    }
    
    @Test
    public void post() throws Exception {
        final String responseBody = "response";
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "text/plain"));
        
        final Response<String> response = new Request.Builder<String>()
                .post(new RequestBody("text/plain", "request".getBytes()))
                .header("Accept", "text/plain")
                .url(server.url("/post").toString())
                .build()
                .setConverter(ResponseBodyConverter.STRING)
                .call();
        
        final RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/post");
        assertThat(request.getBody().readUtf8()).isEqualTo("request");
        assertThat(request.getHeader("Content-Type")).isEqualTo("text/plain");
        assertThat(request.getHeader("Accept")).isEqualTo("text/plain");
        
        assertThat(response).isEqualTo(
                new Response<String>(200, responseBody.getBytes(), responseBody));
    }
    
    @Test
    public void httpError() {
        final String responseBody = "not found";
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody(responseBody));
        
        try {
            new Request.Builder<Void>()
                    .get()
                    .url(server.url("/fail").toString())
                    .build()
                    .call();
            
            fail("Exception not thrown");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(ResponseException.class);
            assertThat(((ResponseException) e).response).isEqualTo(
                    new Response<String>(404, responseBody.getBytes(), responseBody));
        }
    }
    
    @Test(expected = IOException.class)
    public void failure() throws Exception {
        server.shutdown();
        
        new Request.Builder<Void>()
                .get()
                .url(server.url("/fail").toString())
                .build()
                .call();
    }
}
