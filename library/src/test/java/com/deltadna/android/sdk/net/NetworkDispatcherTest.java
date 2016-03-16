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

import android.os.Handler;

import com.deltadna.android.sdk.exceptions.ResponseException;
import com.deltadna.android.sdk.listeners.RequestListener;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class NetworkDispatcherTest {
    
    private static final int MAX_RETRIES = 1;
    
    private NetworkDispatcher uut;
    private MockWebServer server;
    
    @Before
    public void before() throws IOException {
        final Handler handler = mock(Handler.class);
        when(handler.post(any(Runnable.class))).then(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        });
        
        uut = new NetworkDispatcher(handler);
        
        server = new MockWebServer();
        server.start();
    }
    
    @After
    public void after() throws IOException {
        server.shutdown();
        uut = null;
    }
    
    @Test
    public void successfulRequest() throws InterruptedException {
        final String responseBody = "response";
        final RequestListener<Response<String>> listener =
                mock(RequestListener.class);
        
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody));
        
        uut.enqueue(
                new Request.Builder<String>()
                        .get()
                        .url(server.url("/success").toString())
                        .build(),
                ResponseBodyConverter.STRING,
                listener);
        
        server.takeRequest();
        Thread.sleep(100);
        
        assertThat(server.getRequestCount()).isEqualTo(1);
        verify(listener).onSuccess(eq(new Response<String>(
                200, responseBody.getBytes(), responseBody)));
    }
    
    @Test
    public void failureRetriesRequest()
            throws InterruptedException {
        
        final String responseBody = "not found";
        final RequestListener<Response<Void>> listener =
                mock(RequestListener.class);
        
        for (int i = 0; i < 1 + MAX_RETRIES; i++) {
            server.enqueue(new MockResponse()
                    .setResponseCode(404)
                    .setBody(responseBody));
        }
        
        uut.enqueue(
                new Request.Builder<Void>()
                        .get()
                        .url(server.url("/failure").toString())
                        .maxRetries(MAX_RETRIES)
                        .build(),
                listener);
        
        for (int i = 0; i < 1 + MAX_RETRIES; i++) {
            server.takeRequest();
        }
        Thread.sleep(100);
        
        assertThat(server.getRequestCount()).isEqualTo(MAX_RETRIES + 1);
        verify(listener).onFailure(argThat(new ArgumentMatcher<Throwable>() {
            @Override
            public boolean matches(Object argument) {
                assertThat(argument).isInstanceOf(ResponseException.class);
                assertThat(((ResponseException) argument).response)
                        .isEqualTo(new Response<String>(
                                404,
                                responseBody.getBytes(),
                                responseBody));
                return true;
            }
        }));
    }
    
    @Test
    public void retriesRespectDelay() throws InterruptedException {
        final RequestListener<Response<Void>> listener =
                mock(RequestListener.class);
        
        server.enqueue(new MockResponse().setResponseCode(404));
        server.enqueue(new MockResponse().setResponseCode(200));
        
        uut.enqueue(
                new Request.Builder<Void>()
                        .get()
                        .url(server.url("/delay").toString())
                        .maxRetries(1)
                        .retryDelay(1000)
                        .build(),
                listener);
        
        server.takeRequest();
        final long first = System.currentTimeMillis();
        server.takeRequest();
        final long second = System.currentTimeMillis();
        Thread.sleep(100);
        
        assertThat(second - first).isGreaterThan(900L);
        assertThat(second - first).isAtMost(1100L);
        assertThat(server.getRequestCount()).isEqualTo(2);
        verify(listener).onSuccess(any(Response.class));
    }
    
    // FIXME test failing only on jenkins
    @Ignore
    @Test
    public void requestCancellation() throws InterruptedException {
        final RequestListener<Response<Void>> listener =
                mock(RequestListener.class);
        
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBodyDelay(200, TimeUnit.MILLISECONDS));
        uut.enqueue(
                new Request.Builder<Void>()
                        .get()
                        .url(server.url("/cancel").toString())
                        .build(),
                listener)
                .cancel();
        server.takeRequest(100, TimeUnit.MILLISECONDS);
        Thread.sleep(500);
        
        assertThat(server.getRequestCount()).isEqualTo(1);
        verifyZeroInteractions(listener);
    }
}
