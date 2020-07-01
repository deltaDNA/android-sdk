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

package com.deltadna.android.sdk

import android.database.Cursor
import android.os.Environment
import com.deltadna.android.sdk.DatabaseHelper.ImageMessages.Column.*
import com.deltadna.android.sdk.Location.EXTERNAL
import com.deltadna.android.sdk.Location.INTERNAL
import com.deltadna.android.sdk.helpers.Settings
import com.deltadna.android.sdk.listeners.RequestListener
import com.deltadna.android.sdk.net.CancelableRequest
import com.deltadna.android.sdk.net.NetworkManager
import com.deltadna.android.sdk.net.Response
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.*
import kotlinx.coroutines.experimental.launch
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowEnvironment
import java.io.File
import java.util.*
import java.lang.System.currentTimeMillis as now

@Ignore
@RunWith(RobolectricTestRunner::class)
class ImageMessageStoreTest {
    
    private val application by lazy { RuntimeEnvironment.application }
    
    private lateinit var database: DatabaseHelper
    private lateinit var network: NetworkManager
    private lateinit var settings: Settings
    
    private lateinit var uut: ImageMessageStore
    
    @Before
    fun before() {
        database = mock()
        network = mock()
        settings = mock()
        
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
        
        uut = ImageMessageStore(application, database, network, settings)
    }
    
    @Test
    fun contains() {
        val c1 = mock<Cursor>().apply { whenever(count).then { 1 } }
        val c2 = mock<Cursor>().apply { whenever(count).then { 0 } }
        whenever(database.getImageMessage(eq("http://host.net/path/1.png")))
                .thenReturn(c1, c2)
        
        assertThat(uut.contains("http://host.net/path/1.png")).isTrue()
        assertThat(uut.contains("http://host.net/path/1.png")).isFalse()
    }
    
    @Test
    fun doesNotFetchFromNetworkWhenAskedNotTo() {
        whenever(database.getImageMessage("http://host.net/path/1.png")).then {
            mock<Cursor>().apply { whenever(moveToFirst()).then { false } }
        }
        
        uut.getOnlyIfCached("http://host.net/path/1.png")
        
        verifyZeroInteractions(network)
    }
    
    @Test
    fun fetchesToInternalFromNetworkWhenNotCachedAndUnmounted() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_UNMOUNTED)
        whenever(database.getImageMessage(eq("http://host.net/path/1.png"))).then {
            mock<Cursor>().apply {
                whenever(moveToFirst()).then { false }
            }
        }
        
        launch { uut.get("http://host.net/path/1.png") }
        
        Robolectric.getForegroundThreadScheduler().runOneTask()
        verify(network, timeout(500)).fetch(
                eq("http://host.net/path/1.png"),
                argThat { this == File(
                        INTERNAL.cache(application, "image_messages"),
                        "1.png") },
                any())
    }
    
    @Test
    fun fetchesToInternalFromNetworkWhenNotCachedAndSet() {
        whenever(settings.isUseInternalStorageForImageMessages).then { true }
        whenever(database.getImageMessage(eq("http://host.net/path/1.png"))).then {
            mock<Cursor>().apply {
                whenever(moveToFirst()).then { false }
            }
        }
        
        launch { uut.get("http://host.net/path/1.png") }
        
        Robolectric.getForegroundThreadScheduler().runOneTask()
        verify(network, timeout(500)).fetch(
                eq("http://host.net/path/1.png"),
                argThat { this == File(
                        INTERNAL.cache(application, "image_messages"),
                        "1.png") },
                any())
    }
    
    @Test
    fun fetchesToExternalFromNetworkWhenNotCachedAndMounted() {
        whenever(database.getImageMessage(eq("http://host.net/path/1.png"))).then {
            mock<Cursor>().apply {
                whenever(moveToFirst()).then { false }
            }
        }
        
        launch { uut.get("http://host.net/path/1.png") }
        
        Robolectric.getForegroundThreadScheduler().runOneTask()
        verify(network, timeout(500)).fetch(
                eq("http://host.net/path/1.png"),
                argThat { this == File(
                        EXTERNAL.cache(application, "image_messages"),
                        "1.png") },
                any())
    }
    
    @Test
    fun insertsEntryOnFetchSuccess() {
        whenever(database.getImageMessage(eq("http://host.net/path/1.png"))).then {
            mock<Cursor>().apply {
                whenever(moveToFirst()).then { false }
            }
        }
        doAnswer {
            (it.arguments[2] as RequestListener<File>).onCompleted(
                    mock<Response<File>>().apply {
                        whenever(isSuccessful).then { true }
                    })
            mock<CancelableRequest>()
        }.whenever(network).fetch(
                eq("http://host.net/path/1.png"),
                argThat { this == File(
                        EXTERNAL.cache(application, "image_messages"),
                        "1.png") },
                any())
        
        launch { uut.get("http://host.net/path/1.png") }
        
        verify(database, timeout(500)).insertImageMessage(
                eq("http://host.net/path/1.png"),
                eq(EXTERNAL),
                eq("1.png"),
                any(),
                argThat { before(Date()) && after(Date(now() - 500)) })
    }
    
    @Test
    fun loadsFromStorage() {
        File(EXTERNAL.cache(application, "image_messages"), "1.png").createNewFile()
        
        whenever(database.getImageMessage(eq("http://host.net/path/1.png"))).then {
            mock<Cursor>().apply {
                whenever(moveToFirst()).then { true }
                whenever(getColumnIndex(eq(LOCATION.toString()))).then { 0 }
                whenever(getColumnIndex(eq(NAME.toString()))).then { 1 }
                whenever(getString(eq(0))).then { EXTERNAL.name }
                whenever(getString(eq(1))).then { "1.png" }
            }
        }
        
        launch { uut.get("http://host.net/path/1.png") }
        Thread.sleep(500)
        
        verifyZeroInteractions(network)
    }
    
    @Test
    fun loadingMissingFromStorageFetchesFromNetwork() {
        whenever(database.getImageMessage(eq("http://host.net/path/1.png"))).then {
            mock<Cursor>().apply {
                whenever(moveToFirst()).then { false }
                whenever(getColumnIndex(eq(LOCATION.toString()))).then { 0 }
                whenever(getColumnIndex(eq(NAME.toString()))).then { 1 }
                whenever(getString(eq(0))).then { EXTERNAL.name }
                whenever(getString(eq(1))).then { "1.png" }
            }
        }
        
        launch { uut.get("http://host.net/path/1.png") }
        
        verify(network, timeout(500)).fetch(
                eq("http://host.net/path/1.png"),
                argThat { this == File(
                        EXTERNAL.cache(application, "image_messages"),
                        "1.png") },
                any())
    }
    
    @Test
    fun prefetchNothing() {
        with(mock<ImageMessageStore.Callback<Void>>()) {
            uut.prefetch(this)

            runTasks()
            verify(this).onCompleted(isNull())
        }
    }
    
    @Test
    fun prefetch() {
        val callback = mock<ImageMessageStore.Callback<Void>>()
        // 1 and 3 will need to be downloaded, 2 is already cached
        val items = arrayOf("1.png", "2.png", "3.png")
        items.forEach { name ->
            val file = File(EXTERNAL.cache(application, "image_messages"), name)
            file.createNewFile()
            
            if (name == "2.png") {
                whenever(database.getImageMessage(eq("http://host.net/path/$name")))
                        .then { mock<Cursor>().apply {
                            whenever(moveToFirst()).then { true }
                            whenever(getColumnIndex(eq(LOCATION.toString()))).then { 0 }
                            whenever(getColumnIndex(eq(NAME.toString()))).then { 1 }
                            whenever(getString(eq(0))).then { EXTERNAL.name }
                            whenever(getString(eq(1))).then { name }
                        }}
            } else {
                whenever(database.getImageMessage(eq("http://host.net/path/$name")))
                        .then { mock<Cursor>().apply {
                            whenever(moveToFirst()).then { false }
                        }}
                
                doAnswer {
                    (it.arguments[2] as RequestListener<File>).onCompleted(
                            Response<File>(200, false, null, file, null))
                    mock<CancelableRequest>()
                }.whenever(network).fetch(eq("http://host.net/path/$name"), any(), any())
            }
        }
        
        uut.prefetch(
                callback,
                *items.map { "http://host.net/path/$it" }.toTypedArray())

        waitAndRunTasks()
        verify(callback).onCompleted(isNull())
        verify(network, never()).fetch(
                eq("http://host.net/path/2.png"),
                any(),
                any())
    }
    
    @Test @Ignore
    fun prefetchFailsOnAnySingleFailure() {
        val callback = mock<ImageMessageStore.Callback<Void>>()
        // 2 will fail to download
        val items = arrayOf("1.png", "2.png", "3.png")
        items.forEach { name ->
            val file = File(EXTERNAL.cache(application, "image_messages"), name)
            file.createNewFile()
            
            whenever(database.getImageMessage(eq("http://host.net/path/$name")))
                    .then { mock<Cursor>().apply {
                        whenever(moveToFirst()).then { false }
                    }}
            
            doAnswer {
                if (name == "2.png") {
                    (it.arguments[2] as RequestListener<File>).onCompleted(
                            Response<File>(500, false, null, null, "error"))
                } else {
                    (it.arguments[2] as RequestListener<File>).onCompleted(
                            Response<File>(200, false, null, file, null))
                }
                mock<CancelableRequest>()
            }.whenever(network).fetch(eq("http://host.net/path/$name"), any(), any())
        }
        
        uut.prefetch(
                callback,
                *items.map { "http://host.net/path/$it" }.toTypedArray())
        
        waitAndRunTasks()
        verify(callback, timeout(500)).onFailed(any())
    }
    
    @Test
    fun cleanUp() {
        val items = arrayOf("1.png", "2.png", "3.png")
        items.forEach { if (it == "2.png") {
            File(EXTERNAL.cache(application, "image_messages"), it).createNewFile()
        }}
        whenever(database.imageMessages).then { mock<Cursor>().apply {
            whenever(moveToNext()).thenReturn(true, true, true, false)
            whenever(getColumnIndex(eq(ID.toString()))).then { 0 }
            whenever(getColumnIndex(eq(LOCATION.toString()))).then { 1 }
            whenever(getColumnIndex(eq(NAME.toString()))).then { 2 }
            whenever(getLong(eq(0))).thenReturn(0, 2)
            whenever(getString(eq(1))).then { EXTERNAL.name }
            whenever(getString(eq(2))).thenReturn(items[0], *items.drop(1).toTypedArray())
        }}
        
        uut.cleanUp()
        
        verify(database, timeout(500)).removeImageMessage(eq(0L))
        verify(database, timeout(500)).removeImageMessage(eq(2L))
    }
    
    @Test
    fun clear() {
        val items = arrayOf("1.png", "2.png", "3.png", "4.png")
        items.take(2).forEach {
            File(INTERNAL.cache(application, "image_messages"), it).createNewFile()
        }
        items.takeLast(2).forEach {
            File(EXTERNAL.cache(application, "image_messages"), it).createNewFile()
        }
        
        uut.clear()
        
        verify(database, timeout(500)).removeImageMessageRows()
        assertThat(INTERNAL.cache(application, "image_messages").listFiles()).isEmpty()
        assertThat(EXTERNAL.cache(application, "image_messages").listFiles()).isEmpty()
    }
}
