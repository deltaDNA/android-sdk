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

import com.deltadna.android.sdk.helpers.Settings;
import com.deltadna.android.sdk.listeners.RequestListener;
import com.google.common.io.Files;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static com.google.common.truth.Truth.assertThat;

@RunWith(MockitoJUnitRunner.class)
public final class NetworkManagerTest {
    
    private static final String ENV_KEY = "env_key";
    private static final String COLLECT = "/collect";
    private static final String ENGAGE = "/engage";
    
    private NetworkManager uut;
    private MockWebServer server;
    
    @Before
    public void before() throws IOException {
        final Settings settings = mock(Settings.class);
        when(settings.httpRequestMaxRetries()).thenReturn(1);
        
        server = new MockWebServer();
        server.start();
        
        uut = new NetworkManager(
                ENV_KEY,
                server.url(COLLECT).toString(),
                server.url(ENGAGE).toString(),
                settings,
                null);
    }
    
    @After
    public void after() throws IOException {
        server.shutdown();
        uut = null;
    }
    
    @Test
    public void collect() throws JSONException, InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));
        
        uut.collect(new JSONObject().put("field", 1), null);
        
        final RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo(COLLECT + "/" + ENV_KEY);
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8()).isEqualTo("{\"field\":1}");
    }
    
    @Test
    public void collectBulk() throws JSONException, InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));
        
        uut.collect(new JSONObject().put("eventList", "[]"), null);
        
        final RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo(COLLECT + "/" + ENV_KEY + "/bulk");
    }
    
    @Test
    public void collectWithHash() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));
        
        uut = new NetworkManager(
                ENV_KEY,
                server.url(COLLECT).toString(),
                server.url(ENGAGE).toString(),
                mock(Settings.class),
                "hash");
        uut.collect(new JSONObject(), null);
        
        final RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith(COLLECT + "/" + ENV_KEY + "/hash");
    }
    
    @Test
    public void collectBulkWithHash() throws JSONException, InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));
        
        uut = new NetworkManager(
                ENV_KEY,
                server.url(COLLECT).toString(),
                server.url(ENGAGE).toString(),
                mock(Settings.class),
                "hash");
        uut.collect(new JSONObject().put("eventList", "[]"), null);
        
        final RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith(COLLECT + "/" + ENV_KEY + "/bulk" + "/hash");
    }
    
    @Test
    public void engage() throws JSONException, InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"result\":1}"));
        
        uut.engage(new JSONObject().put("field", 1), mock(RequestListener.class));
        
        final RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo(ENGAGE + "/" + ENV_KEY);
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8()).isEqualTo("{\"field\":1}");
    }
    
    @Test
    public void engageWithHash() throws JSONException, InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));
        
        uut = new NetworkManager(
                ENV_KEY,
                server.url(COLLECT).toString(),
                server.url(ENGAGE).toString(),
                mock(Settings.class),
                "hash");
        uut.engage(new JSONObject().put("field", 1), mock(RequestListener.class));
        
        final RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith(ENGAGE + "/" + ENV_KEY + "/hash");
    }
    
    @Test
    public void fetch() throws IOException, InterruptedException {
        final File dst = File.createTempFile("ddnasdk-test-", ".tmp");
        
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("response\nline\nline"));
        
        uut.fetch(server.url("/file").toString(), dst, mock(RequestListener.class));

        final RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/file");
        assertThat(request.getMethod()).isEqualTo("GET");
        
        assertThat(Files.readLines(dst, Charset.forName("UTF-8")))
                .isEqualTo(Arrays.asList("response", "line", "line"));
        
        assertThat(dst.delete()).isTrue();
    }
}
