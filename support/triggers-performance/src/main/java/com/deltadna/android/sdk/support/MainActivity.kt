/*
 * Copyright (c) 2018 deltaDNA Ltd. All rights reserved.
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

package com.deltadna.android.sdk.support

import android.os.Bundle
import android.os.Debug
import androidx.appcompat.app.AppCompatActivity;
import android.view.View
import com.deltadna.android.sdk.DDNA
import com.deltadna.android.sdk.Event
import com.deltadna.android.sdk.EventActionHandler
import com.deltadna.android.sdk.Params
import com.deltadna.android.sdk.listeners.EventListener
import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.squareup.okhttp.mockwebserver.MockResponse
import kotlinx.coroutines.experimental.async

/**
 * Requires for HTTPS to be disabled in the SDK.
 */
class MainActivity : AppCompatActivity() {
    
    private val aValues = arrayOf(
            jsonObject("p" to "c"),
            jsonObject("s" to "c"),
            jsonObject("o" to "equal to"),
            jsonObject("p" to "a"),
            jsonObject("i" to 15),
            jsonObject("o" to "less than"),
            jsonObject("o" to "and"),
            jsonObject("p" to "b"),
            jsonObject("i" to 15),
            jsonObject("o" to "greater than eq"),
            jsonObject("o" to "and"),
            jsonObject("p" to "d"),
            jsonObject("b" to true),
            jsonObject("o" to "equal to"),
            jsonObject("o" to "or"))
    private val aParams = arrayOf("a" to 10, "b" to 5, "c" to "c", "d" to true)
    
    private val server by lazy { (application as Application).server }
    private val listener = Listener()
    
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_main)
        
        enqueueSessionConfigurationResponse()
        async { server.takeRequest() }
    }
    
    public override fun onDestroy() {
        DDNA.instance().stopSdk()
        
        super.onDestroy()
    }
    
    fun onSessionConfiguration(view: View) {
        enqueueSessionConfigurationResponse()
        DDNA.instance().requestSessionConfiguration()
        DDNA.instance().register(listener)
        
        Debug.startMethodTracing("session_configuration_" + System.currentTimeMillis())
        async { server.takeRequest() }
    }
    
    fun onEvent(view: View) {
        Debug.startMethodTracing("event_" + System.currentTimeMillis())
        
        DDNA    .instance()
                .recordEvent(KEvent("event", *aParams))
                .add(EventActionHandler.GameParametersHandler {})
                .run()
        
        Debug.stopMethodTracing()
    }
    
    private fun enqueueSessionConfigurationResponse() {
        server.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody(jsonObject("parameters" to jsonObject(
                        "eventTriggers" to jsonArray(
                                jsonObject(
                                        "eventName" to "event",
                                        "action" to "gameParameters",
                                        "condition" to jsonArray(*aValues)))))
                        .toString()))
    }
    
    private class KEvent(name: String, vararg params: Pair<String, Any?>)
        : Event<KEvent>(name, Params().apply { params.forEach { (k, v) -> put(k , v) } })
    
    private class Listener : EventListener {
        override fun onSessionConfigured(cached: Boolean) {
            Debug.stopMethodTracing()
            DDNA.instance().unregister(this)
        }
        override fun onSessionConfigurationFailed(cause: Throwable?) {
            Debug.stopMethodTracing()
            DDNA.instance().unregister(this)
        }
    }
}
