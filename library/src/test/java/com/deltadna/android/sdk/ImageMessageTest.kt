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

import com.deltadna.android.sdk.net.Response
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ImageMessageTest {
    
    @Test
    fun createWithImage() {
        with(KEngagement("point")) {
            setResponse(Response(200, null, IMAGE, null))
            
            assertThat(ImageMessage.create(this)).isNotNull()
        }
    }
    
    @Test
    fun createWithFailure() {
        with(KEngagement("point")) {
            setResponse(Response(300, null, null, "error"))
            
            assertThat(ImageMessage.create(this)).isNull()
        }
    }
    
    @Test
    fun createWithoutImage() {
        with(KEngagement("point")) {
            setResponse(Response(200, null, JSONObject(), null))
            
            assertThat(ImageMessage.create(this)).isNull()
        }
    }
    
    private class KEngagement(point: String) : Engagement<KEngagement>(point)
    
    private companion object {
        
        val IMAGE = JSONObject("{\r\n    \"transactionID\": \"1898738848054116400\",\r\n    \"image\": {\r\n        \"width\": 512,\r\n        \"height\": 256,\r\n        \"format\": \"png\",\r\n        \"spritemap\": {\r\n            \"background\": {\r\n                \"x\": 2,\r\n                \"y\": 74,\r\n                \"width\": 320,\r\n                \"height\": 180\r\n            },\r\n            \"buttons\": [\r\n                {\r\n                    \"x\": 2,\r\n                    \"y\": 38,\r\n                    \"width\": 160,\r\n                    \"height\": 34\r\n                },\r\n                {\r\n                    \"x\": 2,\r\n                    \"y\": 2,\r\n                    \"width\": 160,\r\n                    \"height\": 34\r\n                }\r\n            ]\r\n        },\r\n        \"layout\": {\r\n            \"landscape\": {\r\n                \"background\": {\r\n                    \"contain\": {\r\n                        \"halign\": \"center\",\r\n                        \"valign\": \"center\",\r\n                        \"left\": \"10%\",\r\n                        \"right\": \"10%\",\r\n                        \"top\": \"0px\",\r\n                        \"bottom\": \"0%\"\r\n                    },\r\n                    \"action\": {\r\n                        \"type\": \"dismiss\"\r\n                    }\r\n                },\r\n                \"buttons\": [\r\n                    {\r\n                        \"x\": 160,\r\n                        \"y\": 145,\r\n                        \"action\": {\r\n                            \"type\": \"action\",\r\n                            \"value\": \"POWERUP\"\r\n                        }\r\n                    },\r\n                    {\r\n                        \"x\": 0,\r\n                        \"y\": 145,\r\n                        \"action\": {\r\n                            \"type\": \"dismiss\"\r\n                        }\r\n                    }\r\n                ]\r\n            }\r\n        },\r\n        \"shim\": {\r\n            \"mask\": \"dimmed\",\r\n            \"action\": {\r\n                \"type\": \"dismiss\"\r\n            }\r\n        },\r\n        \"url\": \"http://download.deltadna.net/engagements/f20f3c4fc19d4d49bc365b472770c065.png\"\r\n    },\r\n    \"parameters\": {\r\n        \"powerUpName\": \"MoHawk\"\r\n    }\r\n}")
    }
}
