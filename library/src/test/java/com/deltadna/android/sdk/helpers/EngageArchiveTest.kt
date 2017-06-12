/*
 * Copyright (c) 2017 deltaDNA Ltd. All rights reserved.
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

package com.deltadna.android.sdk.helpers

import android.os.Environment
import com.google.common.truth.Truth.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class EngageArchiveTest {
    
    private val path = RuntimeEnvironment.application.getExternalFilesDir(null)
    private val legacyPath = Environment.getExternalStorageDirectory()
    
    @Before
    fun before() {
        path.mkdirs()
        legacyPath.mkdirs()
    }
    
    @After
    fun after() {
        path.deleteRecursively()
        legacyPath.deleteRecursively()
    }
    
    @Test
    fun migratesFiles() {
        val f1 = File.createTempFile("file1", "", legacyPath)
        val f2 = File.createTempFile("file2", "", legacyPath)
        val f3 = File.createTempFile("file3", "", legacyPath)
        
        EngageArchive(path, legacyPath)
        
        pause()
        
        assertThat(File(path, f1.name).exists()).isTrue()
        assertThat(File(path, f2.name).exists()).isTrue()
        assertThat(File(path, f3.name).exists()).isTrue()
        assertThat(f1.exists()).isFalse()
        assertThat(f2.exists()).isFalse()
        assertThat(f3.exists()).isFalse()
    }
    
    @Test
    fun migrationPathNull() {
        val f = File.createTempFile("file1", "", legacyPath)
        
        EngageArchive(path, null)
        pause()
        
        assertThat(f.exists()).isTrue()
    }
    
    @Test
    fun saveAndLoad() {
        with(EngageArchive(path, legacyPath)) {
            put("point", "flavour", "value")
            save()
        }
        
        pause()
        
        with(EngageArchive(path, legacyPath)) {
            pause()
            
            assertThat(contains("point", "flavour")).isTrue()
            assertThat(get("point", "flavour")).isEqualTo("value")
        }
    }
    
    private fun pause() = Thread.sleep(1000)
}
