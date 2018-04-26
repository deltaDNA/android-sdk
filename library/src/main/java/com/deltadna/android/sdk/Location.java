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

package com.deltadna.android.sdk;

import android.content.Context;
import android.os.Environment;

import java.io.File;

enum Location {
    
    INTERNAL {
        @Override
        File storage(Context context, String subdirectory) {
            return new File(
                    context.getFilesDir(),
                    DIRECTORY + subdirectory);
        }
        
        @Override
        File cache(Context context, String subdirectory) {
            return new File(
                    context.getCacheDir(),
                    DIRECTORY + subdirectory);
        }
    },
    EXTERNAL {
        @Override
        boolean available() {
            return Environment.getExternalStorageState()
                    .equals(Environment.MEDIA_MOUNTED);
        }
        
        @Override
        File storage(Context context, String subdirectory) {
            return new File(
                    context.getExternalFilesDir(null),
                    DIRECTORY + subdirectory);
        }
        
        @Override
        File cache(Context context, String subdirectory) {
            return new File(
                    context.getExternalCacheDir(),
                    DIRECTORY + subdirectory);
        }
    };
    
    boolean available() {
        return true;
    }
    
    abstract File storage(Context context, String subdirectory);
    abstract File cache(Context context, String subdirectory);
    
    private static final String DIRECTORY =
            "com.deltadna.android.sdk" + File.separator;
}
