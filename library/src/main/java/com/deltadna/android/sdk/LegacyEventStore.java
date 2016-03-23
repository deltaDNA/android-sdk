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

package com.deltadna.android.sdk;

import android.content.SharedPreferences;

import com.deltadna.android.sdk.helpers.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DeltaDNA event store, implements a two file buffer of events.
 */
@Deprecated
class LegacyEventStore {
    static private final String PF_KEY_IN_FILE = "DDSDK_EVENT_IN_FILE";
    static private final String PF_KEY_OUT_FILE = "DDSDK_EVENT_OUT_FILE";
    static private final String FILE_A = "A";
    static private final String FILE_B = "B";
    static private final int MAX_FILE_SIZE = 4 * 1024 * 1024;	// 4MB

    private final Preferences prefs;

    private String mInFilePath = null;
    private String mOutFilePath = null;

    private File mInFile = null;

    private boolean mInitialised = false;
    private boolean mDebug = false;

    protected ReentrantLock mLock = new ReentrantLock();

    /**
     * Initializes a new instance of the {@link EventStore} class.
     *
     * @param path Path to where we hold the events.
     * @param debug
     */
    public LegacyEventStore(String path, Preferences prefs, boolean debug) {
        this.prefs = prefs;
        mDebug = debug;

        try{
            initialiseFileStreams(path, false);
            mInitialised = true;
        }catch (Exception e){
            Log("Problem initialising Event Store: " + e.getMessage());
        }
    }
    /**
     * Pushes a new object onto the event store.
     *
     * @param obj The event to push
     *
     * @return TRUE on success, FALSE otherwise.
     */
    public boolean push(String obj){
        boolean result = false;

        mLock.lock();

        if(mInitialised && (mInFile.length() < MAX_FILE_SIZE)){
            try{
                byte[] record = obj.getBytes("UTF-8");
                byte[] length = Utils.toBytes(record.length);

                FileOutputStream strm = new FileOutputStream(mInFile, true);
                strm.write(length);
                strm.write(record);
                strm.flush();
                strm.close();
                strm = null;
                result = true;

            }catch (Exception e){
                Log("Problem pushing event to Event Store: " + e.getMessage());
            }
        }

        mLock.unlock();

        return result;
    }
    /**
     * Swap the in and out buffers.
     *
     * @return TRUE on success, FALSE otherwise.
     */
    public boolean swap(){
        boolean result = false;

        mLock.lock();

        File tempFile = new File(mOutFilePath);

        // only swap if out buffer is empty
        if(!tempFile.exists() || (tempFile.length() == 0)){
            File outFile = mInFile;
            mInFile = tempFile;
            try {
                mInFile.createNewFile();
                mInFilePath = mInFile.getAbsolutePath();
                mOutFilePath = outFile.getAbsolutePath();

                final SharedPreferences.Editor editor = prefs.getPrefs().edit();
                editor.putString(PF_KEY_IN_FILE, mInFile.getName());
                editor.putString(PF_KEY_OUT_FILE, outFile.getName());
                editor.apply();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            result = true;
        }

        mLock.unlock();

        return result;
    }
    /**
     * Read the contents of the out buffer as a list of String.  Can be
     * called multiple times.
     *
     * @return A list of events.
     */
    public Vector<String> read(){
        mLock.lock();

        Vector<String> results = new Vector<String>();
        try{
            File tempFile = new File(mOutFilePath);
            FileInputStream outStrm = new FileInputStream(tempFile);
            byte[] lengthField = new byte[4];
            while(outStrm.read(lengthField, 0, lengthField.length) > 0){
                int eventLength = Utils.toInt32(lengthField);
                byte[] recordField = new byte[eventLength];
                outStrm.read(recordField, 0, recordField.length);
                String record = new String(recordField, "UTF-8");
                results.add(record);
            }
            outStrm.close();

        }catch(Exception e){
            Log("Problem reading events from Event Store: " + e.getMessage());
        }

        mLock.unlock();

        return results;
    }
    /**
     * Clears the out buffer.
     */
    public void clear(){
        mLock.lock();

        if(mInFile != null){
            mInFile.delete();
        }

        File tempFile = new File(mOutFilePath);
        tempFile.delete();
        tempFile = null;

        mLock.unlock();
    }
    /**
     * Clears the out file and creates a new one.
     */
    public void clearOutfile(){
        mLock.lock();

        File tempFile = new File(mOutFilePath);
        tempFile.delete();
        tempFile = null;

        mLock.unlock();
    }
    /**
     * Initialises the file streams for the store.
     *
     * @param path Base path to use.
     * @param reset TRUE if existing files should be deleted.
     */
    private void initialiseFileStreams(String path, boolean reset){
        File dir = new File(path);

        if(!dir.exists()){
            dir.mkdirs();
        }

        String inFile = prefs.getPrefs().getString(PF_KEY_IN_FILE, FILE_A);
        String outFile = prefs.getPrefs().getString(PF_KEY_OUT_FILE, FILE_B);
        File tempFile = new File(inFile);
        inFile = tempFile.getName();		// support legacy pp that could have full pathx
        tempFile = new File(outFile);
        outFile = tempFile.getName();

        mInFile = new File(path, inFile);
        tempFile = new File(path, outFile);
        if(reset){
            mInFile.delete();
            tempFile.delete();
        }

        mInFilePath = tempFile.getAbsolutePath();
        mOutFilePath = tempFile.getAbsolutePath();

        if(mInFile.exists() && tempFile.exists() && !reset){
            Log("Loaded existing Event Store in @ " + mInFile.getAbsolutePath() + " out @ " + tempFile.getAbsolutePath());
        }else{
            Log("Creating new Event Store in @ " + path);
            try {
                mInFile.delete();
                mInFile.createNewFile();
                tempFile.delete();
                tempFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        final SharedPreferences.Editor editor = prefs.getPrefs().edit();
        editor.putString(PF_KEY_IN_FILE, mInFile.getName());
        editor.putString(PF_KEY_OUT_FILE, tempFile.getName());
        editor.apply();

        tempFile.delete();
        tempFile = null;
    }
    /**
     * Message logging call.
     *
     * @param message The message to log.
     */
    private void Log(String message){
        if (mDebug){
            android.util.Log.d(BuildConfig.LOG_TAG, "[DDSDK EventStore] " + message);
        }
    }
}
