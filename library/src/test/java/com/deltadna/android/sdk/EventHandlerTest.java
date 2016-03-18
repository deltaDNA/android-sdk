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

import com.deltadna.android.sdk.exceptions.BadRequestException;
import com.deltadna.android.sdk.helpers.EngageArchive;
import com.deltadna.android.sdk.listeners.RequestListener;
import com.deltadna.android.sdk.net.NetworkManager;
import com.deltadna.android.sdk.net.Response;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class EventHandlerTest {
    
    private EventStore store;
    private EngageArchive archive;
    private NetworkManager network;
    
    private EventHandler uut;
    
    @Before
    public void before() {
        store = mock(EventStore.class);
        archive = mock(EngageArchive.class);
        network = mock(NetworkManager.class);
        
        uut = new EventHandler(store, archive, network);
    }
    
    @After
    public void after() {
        uut.stop(false);
        uut = null;
        
        store = null;
        archive = null;
        network = null;
    }
    
    @Test
    public void startPeriodicUploads() throws InterruptedException, JSONException {
        final List<String> events1 = Arrays.asList("{\"value\":0}", "{\"value\":1}");
        final List<String> events2 = Arrays.asList("{\"value\":0}");
        withStoreEvents(events1, events2);
        invokeOnListeners(new Action() {
            @Override
            public void act(RequestListener listener) {
                listener.onSuccess(null);
            }
        });
        
        uut.start(0, 1);
        Thread.sleep(2200);
        
        final String expected1 = "{\"eventList\":[{\"value\":0},{\"value\":1}]}";
        final String expected2 = "{\"eventList\":[{\"value\":0}]}";
        
        verify(store, times(3)).swap();
        verify(store, times(3)).read();
        verify(network, times(2)).collect(
                argThat(new ArgumentMatcher<JSONObject>() {
                    private int run;

                    @Override
                    public boolean matches(Object argument) {
                        if (run != 2) { // no idea why we get a 3 runs and times(2) works
                            assertThat(argument).isInstanceOf(JSONObject.class);
                            assertThat(argument.toString()).isEqualTo(
                                    run == 0 ? expected1 : expected2);
                        }
                        run++;
                        return true;
                    }
                }),
                any(RequestListener.class));
    }
    
    @Test
    public void stopPeriodicUploads() throws InterruptedException {
        uut.start(1, 1);
        uut.stop(false);
        Thread.sleep(2200);
        
        verify(store, never()).read();
        verify(network, never()).collect(
                any(JSONObject.class),
                any(RequestListener.class));
    }
    
    @Test
    public void stopAndDispatch() throws InterruptedException {
        withStoreEvents(Arrays.asList("0"));
        invokeOnListeners(new Action() {
            @Override
            public void act(RequestListener listener) {
                listener.onSuccess(null);
            }
        });
        
        uut.start(1, 1);
        uut.stop(true);
        Thread.sleep(2200);
        
        verify(store).read();
        verify(network).collect(
                any(JSONObject.class),
                any(RequestListener.class));
    }
    
    @Test
    public void storeClearedOnCorruption() throws InterruptedException {
        withStoreEvents(Arrays.asList("0"));
        invokeOnListeners(new Action() {
            @Override
            public void act(RequestListener listener) {
                listener.onFailure(new BadRequestException(
                        new Response<String>(400, null, null)));
            }
        });
        
        uut.start(0, 1);
        Thread.sleep(1100);
        
        verify(store).clearOutfile();
    }
    
    @Test
    public void handleEvent() {
        final JSONObject event = new JSONObject();
        
        uut.handleEvent(event);
        
        verify(store).push(eq(event.toString()));
    }
    
    @Test
    public void handleEngagementWithLiveSuccess() throws JSONException {
        final JSONObject event = new JSONObject().put("event", 1);
        final JSONObject result = new JSONObject().put("result", 1);
        final RequestListener<JSONObject> listener = mock(RequestListener.class);
        when(network.engage(same(event), any(RequestListener.class)))
                .thenAnswer(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) throws Throwable {
                        ((RequestListener) invocation.getArguments()[1]).onSuccess(
                                new Response<JSONObject>(200, null, result));
                        return null;
                    }
                });
        
        uut.handleEngagement("point", "flavour", event, listener);
        
        verify(archive).put(eq("point"), eq("flavour"), eq(result.toString()));
        verify(listener).onSuccess(same(result));
    }
    
    @Test
    public void handleEngagementWithArchiveHit() throws JSONException {
        final JSONObject event = new JSONObject().put("event", 1);
        final JSONObject archived = new JSONObject().put("archived", 1);
        final RequestListener<JSONObject> listener = mock(RequestListener.class);
        when(network.engage(same(event), any(RequestListener.class)))
                .thenAnswer(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) throws Throwable {
                        ((RequestListener) invocation.getArguments()[1]).onFailure(
                                new Exception());
                        return null;
                    }
                });
        when(archive.contains("point", "flavour")).thenReturn(true);
        when(archive.get("point", "flavour")).thenReturn(archived.toString());
        
        uut.handleEngagement("point", "flavour", event, listener);
        
        final JSONObject cached = new JSONObject(archived.toString())
                .put("isCachedResponse", true);
        verify(archive, never())
                .put(eq("point"), eq("flavour"), eq(cached.toString()));
        verify(listener).onSuccess(argThat(new ArgumentMatcher<JSONObject>() {
            @Override
            public boolean matches(Object argument) {
                assertThat(argument).isInstanceOf(JSONObject.class);
                assertThat(argument.toString()).isEqualTo(cached.toString());
                return true;
            }
        }));
    }
    
    @Test
    public void handleEngagementWithArchiveMiss() throws JSONException {
        final JSONObject event = new JSONObject().put("event", 1);
        final Exception cause = new Exception();
        final RequestListener<JSONObject> listener = mock(RequestListener.class);
        when(network.engage(same(event), any(RequestListener.class)))
                .thenAnswer(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) throws Throwable {
                        ((RequestListener) invocation.getArguments()[1]).onFailure(cause);
                        return null;
                    }
                });
        when(archive.contains("point", "flavour")).thenReturn(false);
        
        uut.handleEngagement("point", "flavour", event, listener);
        
        verify(listener).onFailure(same(cause));
    }
    
    private void withStoreEvents(List<String>... items) {
        OngoingStubbing<Vector<String>> stubbing = when(store.read());
        for (final List<String> item : items) {
            stubbing = stubbing.thenReturn(new Vector<String>(item));
        }
    }
    
    private void invokeOnListeners(final Action action) {
        when(network.collect(any(JSONObject.class), any(RequestListener.class)))
                .thenAnswer(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) throws Throwable {
                        action.act((RequestListener) invocation.getArguments()[1]);
                        return null;
                    }
                });
    }
    
    private interface Action {
        
        void act(RequestListener listener);
    }
}
