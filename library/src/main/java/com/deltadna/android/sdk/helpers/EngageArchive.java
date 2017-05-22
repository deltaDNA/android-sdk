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

package com.deltadna.android.sdk.helpers;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.deltadna.android.sdk.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The engage archive holds previously requested engage responses. The responses
 * can be saved to disk.
 */
public class EngageArchive {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + EngageArchive.class.getSimpleName();
	private static final String FILENAME = "ENGAGEMENTS";
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, String> table = Collections.synchronizedMap(
            new HashMap<String, String>());
    
	private final File path;
    
	/**
	 * Creates a new instance, loading any previously saved engagements from
	 * a file at {@code path}.
	 * 
	 * @param path          the path for the engage archive
     * @param legacyPath    the legacy path from which to migrate engage files
	 */
	public EngageArchive(File path, File legacyPath) {
		this.path = path;
        
        executor.submit(new MigrateLegacyArchive(legacyPath, path));
        executor.submit(new Load());
	}
    
	/**
	 * Returns true if the archive contains a previous response for the
	 * decision point with a flavour.
	 * 
     * @param decisionPoint the decision point
     * @param flavour       the flavour, may be {@code null}
	 * 
	 * @return TRUE if store contains a response, FALSE otherwise.
	 */
    public boolean contains(String decisionPoint, @Nullable String flavour) {
        return table.containsKey(createKey(decisionPoint, flavour));
	}
    
	/**
     * Gets a decision point with a flavour.
	 * 
     * @param decisionPoint the decision point
     * @param flavour       the flavour, may be {@code null}
	 * 
	 * @return The data on success, null otherwise.
	 */
    public String get(String decisionPoint, @Nullable String flavour) {
        return table.get(createKey(decisionPoint, flavour));
	}
    
	/**
     * Puts a decision point with a flavour in the archive.
     * 
     * @param decisionPoint the decision point
     * @param flavour       the flavour, may be {@code null}
     * @param value         the data
	 */
    public void put(
            String decisionPoint,
            @Nullable String flavour,
            String value) {
        
        table.put(createKey(decisionPoint, flavour), value);
	}
    
	/**
	 * Save the archive to disk.
	 */
	public void save() {
        executor.submit(new Save());
    }
    
	/**
	 * Clears the archive.
	 */
	public void clear() {
        executor.submit(new Clear());
    }
    
    private static String createKey(
            String decisionPoint,
            @Nullable String flavour) {
        
        return (TextUtils.isEmpty(flavour))
                ? decisionPoint
                : decisionPoint + '_' + flavour;
    }
    
    private final class MigrateLegacyArchive implements Runnable {
        
        private final File from;
        private final File to;
        
        MigrateLegacyArchive(File from, File to) {
            this.from = from;
            this.to = to;
        }
        
        @Override
        public void run() {
            if (!from.exists()) {
                return;
            }
            
            if (!to.exists() && !to.mkdirs()) {
                Log.w(TAG, "Failed to create " + to + " for migration");
                return;
            }
            
            final File[] files = from.listFiles();
            Log.d(TAG, String.format(
                    Locale.US,
                    "Moving %d files from %s to %s",
                    files.length,
                    from,
                    to));
            
            int moved = 0;
            for (final File file : files) {
                try {
                    Utils.move(file, to);
                    moved++;
                } catch (IOException e) {
                    Log.w(TAG, "Failed moving " + file, e);
                }
            }
            
            if (moved == files.length) {
                if (!from.delete()) {
                    Log.w(TAG, "Failed deleting " + from);
                }
            }
            
            Log.d(TAG, String.format(
                    Locale.US,
                    "Successfully moved %d files from %s to %s",
                    moved,
                    from,
                    to));
        }
    }
    
    private final class Load implements Runnable {
        
        @Override
        public void run() {
            FileInputStream fs = null;
            
            try {
                File file = new File(path, FILENAME);
                Log.d(TAG, "Loading engagement archive from " + file);
                
                if (file.exists()) {
                    fs = new FileInputStream(file);
                    
                    String key = null;
                    String value;
                    int read = 0;
                    byte[] length = new byte[4];
                    int valueLength;
                    byte[] valueField;
                    while (fs.read(length, 0, length.length) > 0) {
                        valueLength = Utils.toInt32(length);
                        valueField = new byte[valueLength];
                        fs.read(valueField, 0, valueField.length);
                        if (read % 2 == 0) {
                            key = new String(valueField, "UTF-8");
                        } else {
                            value = new String(valueField, "UTF-8");
                            table.put(key, value);
                        }
                        read++;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to load engagement archive", e);
            } finally {
                if (fs != null) {
                    try {
                        fs.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Failed to close engagement archive", e);
                    }
                }
            }
        }
    }
    
    private final class Save implements Runnable {
        
        @Override
        public void run() {
            FileOutputStream fs = null;
            
            try {
                if (!path.exists()) {
                    path.mkdirs();
                }
                
                final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] keyBytes;
                byte[] keyLenBytes;
                byte[] valueBytes;
                byte[] valueLenBytes;
                String key;
                String value;
                final Set<String> keys = table.keySet();
                final Iterator<String> iter = keys.iterator();
                while (iter.hasNext()) {
                    key = iter.next();
                    value = table.get(key);
                    
                    keyBytes = key.getBytes();
                    keyLenBytes = Utils.toBytes(keyBytes.length);
                    valueBytes = value.getBytes();
                    valueLenBytes = Utils.toBytes(valueBytes.length);
                    
                    bytes.write(keyLenBytes);
                    bytes.write(keyBytes);
                    bytes.write(valueLenBytes);
                    bytes.write(valueBytes);
                }
                
                fs = new FileOutputStream(new File(path, FILENAME));
                byte[] byteArray = bytes.toByteArray();
                fs.write(byteArray, 0, byteArray.length);
            } catch (Exception e) {
                Log.w(TAG, "Failed to save engagement archive", e);
            } finally {
                if (fs != null) {
                    try {
                        fs.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Failed to close engagement archive", e);
                    }
                }
            }
        }
    }
    
    private final class Clear implements Runnable {
        
        @Override
        public void run() {
            table.clear();
        }
    }
}
