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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.deltadna.android.sdk.BuildConfig;

/**
 * The Engage archive holds previously requested engage responses. The responses
 * can be saved to disk.
 */
public class EngageArchive{
	static private final String FILENAME = "ENGAGEMENTS";

	private HashMap<String, String> mTable = new HashMap<String, String>();
	private Object mLock = new Object();
	private String mPath;

	/**
	 * Creates a new EnagageArchive, loading any previously saved Engagements from
	 * a file at {@code path}.
	 * 
	 * @param path The data file path.
	 */
	public EngageArchive(String path){
		mPath = path;
		load(mPath);
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
        return mTable.containsKey(createKey(decisionPoint, flavour));
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
        return mTable.get(createKey(decisionPoint, flavour));
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
        
        mTable.put(createKey(decisionPoint, flavour), value);
	}
    
	/**
	 * Loads an existing archive from disk.
	 * 
	 * @param path The path to load from.
	 */
	private void load(String path){
		synchronized(mLock){
			FileInputStream fs = null;

			try{
				File file = new File(path, FILENAME);

				Log.d(BuildConfig.LOG_TAG, "Loading Engage from " + file.getAbsolutePath());

				if(file.exists()){
					fs = new FileInputStream(file);

					String key = null;
					String value = null;
					int read = 0;
					byte[] length = new byte[4];
					int valueLength;
					byte[] valueField;
					while(fs.read(length, 0, length.length) > 0){
						valueLength = Utils.toInt32(length);
						valueField = new byte[valueLength];
						fs.read(valueField, 0, valueField.length);
						if(read % 2 == 0){
							key = new String(valueField, "UTF-8");
						}else{
							value = new String(valueField, "UTF-8");
							mTable.put(key, value);
						}
						read++;
					}
				}
			}catch (Exception e){
				Log.w(BuildConfig.LOG_TAG, "Unable to load Engagement archive: " + e.getMessage());

			}finally{
				if(fs != null){
					try {
						fs.close();
					} catch (IOException e) {}
				}
			}
		}
	}
	/**
	 * Save the archive to disk.
	 */
	public void save(){
		synchronized(mLock){
			FileOutputStream fs = null;

			try{
				File dir = new File(mPath);

				if(!dir.exists()){
					dir.mkdirs();
				}

				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				byte[] keyBytes = null;
				byte[] keyLenBytes = null;
				byte[] valueBytes = null;
				byte[] valueLenBytes = null;
				String key = null;
				String value = null;
				Set<String> keys = mTable.keySet();
				Iterator<String> iter = keys.iterator();
				while(iter.hasNext()){
					key = iter.next();
					value = mTable.get(key);
					
					keyBytes = key.getBytes();
					keyLenBytes = Utils.toBytes(keyBytes.length);
					valueBytes = value.getBytes();
					valueLenBytes = Utils.toBytes(valueBytes.length);

					bytes.write(keyLenBytes);
					bytes.write(keyBytes);
					bytes.write(valueLenBytes);
					bytes.write(valueBytes);
				}

				File file = new File(dir.getAbsolutePath(), FILENAME);
				fs = new FileOutputStream(file);

				byte[] byteArray = bytes.toByteArray();
				
				fs.write(byteArray, 0, byteArray.length);
				
			}catch (Exception e){
				Log.w(BuildConfig.LOG_TAG, "Unable to save Engagement archive: " + e.getMessage());
				
			}finally{
				if(fs != null){
					try {
						fs.close();
					} catch (IOException e) {}
				}
			}
		}
	}
	/**
	 * Clears the archive.
	 */
	public void clear(){
		synchronized(mLock){
			mTable.clear();
		}
	}
    
    private static String createKey(
            String decisionPoint,
            @Nullable String flavour) {
        
        return (TextUtils.isEmpty(flavour))
                ? decisionPoint
                : decisionPoint + '_' + flavour;
    }
}
