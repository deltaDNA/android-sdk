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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import nl.jqno.equalsverifier.EqualsVerifier;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public final class ResponseTest {
    
    @Test
    public void ctor() {
        final int code = 1;
        final byte[] bytes = new byte[] {1};
        final Object body = new Object();
        final Response<Object> uut = new Response<Object>(code, bytes, body);
        
        assertThat(uut.code).isSameAs(code);
        assertThat(uut.bytes).isSameAs(bytes);
        assertThat(uut.body).isSameAs(body);
    }
    
    @Test
    public void isSuccessful() {
        assertThat(new Response<Void>(Integer.MIN_VALUE, null, null).isSuccessful()).isFalse();
        
        assertThat(new Response<Void>(199, null, null).isSuccessful()).isFalse();
        assertThat(new Response<Void>(200, null, null).isSuccessful()).isTrue();
        
        assertThat(new Response<Void>(299, null, null).isSuccessful()).isTrue();
        assertThat(new Response<Void>(300, null, null).isSuccessful()).isFalse();
        
        assertThat(new Response<Void>(Integer.MAX_VALUE, null, null).isSuccessful()).isFalse();
    }
    
    @Test
    public void equalsAndHashCode() {
        EqualsVerifier.forClass(Response.class).verify();
    }
    
    @Test
    public void create() throws Exception {
        final int code = 1;
        final String input = "lorem ipsum";
        final InputStream stream = new ByteArrayInputStream(input.getBytes());
        
        final Response<String> uut = Response.create(
                code,
                input.getBytes().length,
                stream,
                ResponseBodyConverter.STRING);
        
        assertThat(uut.code).isEqualTo(code);
        assertThat(uut.body).isEqualTo(input);
        assertThat(stream.available()).isEqualTo(0);
    }
    
    @Test
    public void createWithStreamingInput() throws Exception {
        final String[] input = new String[] {"lorem ", "ipsum"};
        final PipedInputStream stream = new PipedInputStream();
        final PipedOutputStream out = new PipedOutputStream(stream);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (final String token : input) {
                        Thread.sleep(100);
                        out.write(token.getBytes());
                    }
                    
                    out.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        final Response<String> uut = Response.create(
                1,
                -1,
                stream,
                ResponseBodyConverter.STRING);
        
        assertThat(uut.body).isEqualTo("lorem ipsum");
        try {
            stream.read();
            fail("stream has not been closed");
        } catch (IOException e) {
            // expected
        }
    }
}
