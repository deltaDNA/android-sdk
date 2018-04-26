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

import android.os.Environment
import com.google.common.truth.Truth.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LocationTest {
    
    private val application by lazy { RuntimeEnvironment.application }
    
    @Test
    fun internal() {
        with(Location.INTERNAL) {
            assertThat(available()).isTrue()
            
            assertThat(storage(application, "dir")).isEqualTo(
                    File(   application.filesDir,
                            "com.deltadna.android.sdk" + File.separator + "dir"))
            assertThat(cache(application, "dir")).isEqualTo(
                    File(   application.cacheDir,
                            "com.deltadna.android.sdk" + File.separator + "dir"))
        }
    }
    
    @Test
    fun external() {
        with(Location.EXTERNAL) {
            ShadowEnvironment.setExternalStorageState(Environment.MEDIA_UNMOUNTED)
            assertThat(available()).isFalse()
            
            ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
            assertThat(available()).isTrue()
            
            assertThat(storage(application, "dir")).isEqualTo(
                    File(   application.getExternalFilesDir(null),
                            "com.deltadna.android.sdk" + File.separator + "dir"))
            assertThat(cache(application, "dir")).isEqualTo(
                    File(   application.externalCacheDir,
                            "com.deltadna.android.sdk" + File.separator + "dir"))
        }
    }
}
