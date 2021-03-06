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

package com.deltadna.android.sdk

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.json.JSONObject
import org.robolectric.shadows.ShadowLooper
import java.util.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.staticProperties
import kotlin.reflect.jvm.javaField
import kotlin.test.fail

inline fun <reified T> Any.read(field: String) =
        javaClass.getDeclaredField(field).let {
            it.isAccessible = true
            it.get(this) as T
        }

fun JsonObject.convert() = JSONObject(toString())
fun JSONObject.convert() = JsonParser().parse(toString())!!

fun Date.tsIso(): String = DDNA.TIMESTAMP_FORMAT_ISO.format(this)

inline fun <reified T: Throwable> assertThrown(block: () -> Unit) {
    try {
        block()
        fail("Expected exception ${T::class} not thrown")
    } catch (ignored: Throwable) {}
}

fun waitAndRunTasks(millis: Long = 500, iterations: Int = 1) {
    for (i in 0 until iterations) {
        Thread.sleep(millis)
        runTasks()
    }
}

fun runTasks(iterations: Int = 1) {
    for (i in 0 until iterations) {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }
}

fun DDNA.inject(with: DDNA?): DDNA = inject(with, "instance")
fun DDNA.scrub(): DDNA = inject(null)

private inline fun <reified T: Any> T.inject(what: Any?, where: String): T {
    T::class.memberProperties
            .find { it.name == where }
            ?.javaField
            ?.apply {
                isAccessible = true
                set(this@inject, what)
            }
            ?: T::class
                    .staticProperties
                    .find { it.name == where }
                    ?.javaField
                    ?.apply {
                        isAccessible = true
                        set(this@inject, what)
                    }
    return this
}

class KEvent(name: String = "name", vararg params: Pair<String, Any?>)
    : Event<KEvent>(name, Params().apply { params.forEach { (k, v) -> put(k, v) } })

class KEngagement(point: String, flavour: String?)
    : Engagement<KEngagement>(point, flavour)
