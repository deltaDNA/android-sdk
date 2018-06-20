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

import com.deltadna.android.sdk.DDNA
import com.squareup.okhttp.mockwebserver.MockWebServer
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class Application : android.app.Application() {
    
    val server by lazy { MockWebServer() }
    
    override fun onCreate() {
        super.onCreate()
        
        launch {
            server.start()
            
            val collect = server.url("/collect")
            val engage = server.url("/engage")
            
            launch(UI) {
                DDNA.initialise(DDNA.Configuration(
                        this@Application,
                        "07575004106474324897044893014183",
                        collect.toString(),
                        engage.toString())
                        .withSettings { it.setBackgroundEventUpload(false) })
                
                // to make sure the sdk is initialised by this point
                DDNA.instance().startSdk()
            }
        }
    }
}
