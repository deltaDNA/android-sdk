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

import android.content.Context
import android.os.Environment
import com.deltadna.android.sdk.helpers.Settings
import com.deltadna.android.sdk.util.CloseableIterator
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EventStoreTest {
    
    private var application: Context? = null
    private var settings: Settings? = null
    private var prefs: Preferences? = null
    
    private var uut: EventStore? = null
    
    @Before
    fun before() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
        
        application = RuntimeEnvironment.application
        settings = Settings()
        prefs = Preferences(application)
        
        uut = EventStore(application, settings, prefs)
    }
    
    @Test
    fun migratesLegacyStore() {
        val legacy = LegacyEventStore(
                File(   application!!.getExternalFilesDir(null),
                        "/ddsdk/events/").path,
                prefs,
                false,
                false)
        legacy.swap()
        legacy.swap()
        legacy.push("1")
        
        uut = EventStore(application, settings, prefs)
        pause()
        
        assertThat(legacy.read().size).isEqualTo(0)
        with(uut!!.items()) {
            assertThat(next().get()).isEqualTo("1")
            assertThat(hasNext()).isFalse()
        }
    }
    
    @Test
    fun itemsAddedAndRetrievable() {
        val items = listOf("1", "2", "3")
        with(uut!!) {
            items.forEach { add(it) }
            pause()
            
            with(items()) {
                items.forEach {
                    with(next()) {
                        assertThat(available()).isTrue()
                        assertThat(get()).isEqualTo(it)
                    }
                }
                
                assertThat(hasNext()).isFalse()
            }
        }
    }
    
    @Test
    fun itemAddedOnInternal() {
        settings!!.isUseInternalStorageForEvents = true
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_UNMOUNTED)
        
        with(uut!!) {
            add("1")
            pause()
            
            with(items()) {
                with(next()) {
                    assertThat(available()).isTrue()
                    assertThat(get()).isEqualTo("1")
                }
                
                assertThat(hasNext()).isFalse()
            }
        }
    }
    
    @Test
    fun itemAddedWhenExternalNotAvailable() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_UNMOUNTED)
        
        with(uut!!) {
            add("1")
            pause()
            
            with(items()) {
                with(next()) {
                    assertThat(available()).isTrue()
                    assertThat(get()).isEqualTo("1")
                }
                
                assertThat(hasNext()).isFalse()
            }
        }
    }
    
    @Test
    fun itemNotAvailableOnExternalUnmounted() {
        with(uut!!) {
            add("1")
            pause()
            ShadowEnvironment.setExternalStorageState(Environment.MEDIA_UNMOUNTED)
            
            with(items()) {
                assertThat(next().available()).isFalse()
                assertThat(hasNext()).isFalse()
            }
        }
    }
    
    @Test
    fun itemsRetrievedUpToLimit() {
        val items = listOf(512*1024, 512*1024, 512*1024, 1024*1024)
        with(uut!!) {
            items.forEach {
                with(CharArray(it)) {
                    fill('a')
                    add(String(this))
                }
                pause()
            }
            
            with(items()) {
                assertThat(hasNext()).isTrue()
                next()
                assertThat(hasNext()).isTrue()
                next()
                assertThat(hasNext()).isFalse()
                close(CloseableIterator.Mode.ALL)
            }
            with(items()) {
                assertThat(hasNext()).isTrue()
                next()
                assertThat(hasNext()).isFalse()
                close(CloseableIterator.Mode.ALL)
            }
            with(items()) {
                assertThat(hasNext()).isTrue()
                next()
                assertThat(hasNext()).isFalse()
                close(CloseableIterator.Mode.ALL)
            }
            with(items()) {
                assertThat(hasNext()).isFalse()
            }
        }
    }
    
    @Test
    fun oversizeItemNotAdded() {
        with(uut!!) {
            with(CharArray(1024*1024+1)) {
                fill('a')
                add(String(this))
            }
            pause()
            
            assertThat(items().hasNext()).isFalse()
        }
    }
    
    @Test
    fun itemNotAddedWhenFull() {
        with(uut!!) {
            (0..5).forEach {
                with(CharArray(1024*1024)) {
                    fill('a')
                    add(String(this))
                }
                pause()
            }
            
            (0..4).forEach {
                with(items()) {
                    assertThat(hasNext()).isTrue()
                    next()
                    assertThat(hasNext()).isFalse()
                    close(CloseableIterator.Mode.ALL)
                }
            }
            assertThat(items().hasNext()).isFalse()
        }
    }
    
    @Test
    fun itemsNotRemovedOnCloseWithoutClear() {
        val items = listOf("1", "2", "3")
        with(uut!!) {
            items.forEach { add(it) }
            pause()
            items().close(CloseableIterator.Mode.NONE)
            
            with(items()) {
                items.forEach {
                    with(next()) {
                        assertThat(available()).isTrue()
                        assertThat(get()).isEqualTo(it)
                    }
                }
                
                assertThat(hasNext()).isFalse()
            }
        }
    }
    
    @Test
    fun itemsRemovedOnCloseWithClearAll() {
        with(uut!!) {
            listOf("1", "2", "3").forEach { add(it) }
            pause()
            items().close(CloseableIterator.Mode.ALL)
            
            assertThat(items().hasNext()).isFalse()
        }
    }
    
    @Test
    fun itemsRemovedOnCloseWithClearUpToCurrent() {
        with(uut!!) {
            listOf("1", "2", "3").forEach { add(it) }
            pause()
            
            with(items()) {
                next()
                ShadowEnvironment.setExternalStorageState(Environment.MEDIA_UNMOUNTED)
                assertThat(next().available()).isFalse()
                close(CloseableIterator.Mode.UP_TO_CURRENT)
            }
            
            ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
            with(items()) {
                assertThat(next().get()).isEqualTo("2")
                assertThat(next().get()).isEqualTo("3")
            }
        }
    }
    
    @Test
    fun clear() {
        with(uut!!) {
            listOf("1", "2", "3").forEach { add(it) }
            clear()
            
            assertThat(items().hasNext()).isFalse()
        }
    }
    
    private fun pause() = Thread.sleep(1000)
}
