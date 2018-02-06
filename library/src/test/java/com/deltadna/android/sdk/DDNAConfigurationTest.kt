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

import com.deltadna.android.sdk.test.read
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DDNAConfigurationTest {
    
    @Test
    fun missingUrlSchemasGetFixed() {
        createUut("collectUrl", "engageUrl").apply {
            assertThat(read<String>("collectUrl")).isEqualTo("https://collectUrl")
            assertThat(read<String>("engageUrl")).isEqualTo("https://engageUrl")
        }
    }
    
    @Test
    fun httpUrlsGetChangedToHttps() {
        createUut("http://collectUrl", "http://engageUrl").apply {
            assertThat(read<String>("collectUrl")).isEqualTo("https://collectUrl")
            assertThat(read<String>("engageUrl")).isEqualTo("https://engageUrl")
        }
    }
    
    @Test
    fun httpsUrlsStayUntouched() {
        createUut("https://collectUrl", "https://engageUrl").apply {
            assertThat(read<String>("collectUrl")).isEqualTo("https://collectUrl")
            assertThat(read<String>("engageUrl")).isEqualTo("https://engageUrl")
        }
    }
    
    private fun createUut(collectUrl: String, engageUrl: String) =
            DDNA.Configuration(
                    mock(),
                    "environmentKey",
                    collectUrl,
                    engageUrl)
}
