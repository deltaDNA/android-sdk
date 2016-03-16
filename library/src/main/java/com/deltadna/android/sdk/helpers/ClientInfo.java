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

import android.annotation.SuppressLint;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The ClientInfo class determines facts about the device the game is being played on.  The
 * results are formatted to be valid game parameter values.
 */
public class ClientInfo{
	private static String smPlatform = "ANDROID";

	/**
	 * The platform the game is being played on.
	 * 
	 * @return Always "ANDROID".
	 */
	static public String platform(){
		return smPlatform;
	}

	private static String smDeviceName = null;	
	/**
	 * The name of the device.
	 * 
	 * @return The device name.
	 */
	static public String deviceName(){
		if(smDeviceName == null){
			smDeviceName = android.os.Build.PRODUCT;
		}
		return smDeviceName;
	}

	private static String smDeviceModel = null;
	/**
	 * The device's model number.
	 * 
	 * @return The device model.
	 */
	static public String deviceModel(){
		if(smDeviceModel == null){
			smDeviceModel = android.os.Build.MODEL;
		}
		return smDeviceModel;
	}

	private static String smDeviceType = null;

	/**
	 * Android makes no distinction on device types only form factors and capabilities
	 * are important.
	 * 
	 * @return Always "UNKNOWN".
	 */
	static public String deviceType(){
		if(smDeviceType == null){
			smDeviceType = "UNKNOWN";
		}
		return smDeviceType;
	}

	private static String smOperatingSystem = null;
	/**
	 * The operating system the device is running.
	 * 
	 * @return Always "ANDROID".
	 */
	static public String operatingSystem(){
		if(smOperatingSystem == null){
			smOperatingSystem = "ANDROID";
		}
		return smOperatingSystem;
	}

	private static String smOperatingSystemVersion = null;
	/**
	 * The version of the operating system the device is running.
	 * 
	 * @return The device os version.
	 */
	static public String operatingSystemVersion(){
		if(smOperatingSystemVersion == null){
			smOperatingSystemVersion = android.os.Build.VERSION.RELEASE;
		}
		return smOperatingSystemVersion;
	}

	private static String smManufacturer = null;
	/**
	 * The manufacturer of the device the game is running on.
	 * 
	 * @return Device manufaturer string.
	 */
	static public String manufacturer(){
		if(smManufacturer == null){
			smManufacturer = android.os.Build.MANUFACTURER;
		}
		return smManufacturer;
	}

	private static String smTimezoneOffset = null;
	/**
	 * The timezone offset from UTC the device is set to.
	 * 
	 * @return The timezone offset.
	 */
	@SuppressLint("SimpleDateFormat")
	static public String timezoneOffset(){
		if(smTimezoneOffset == null){
		    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
		    String   timeZone = new SimpleDateFormat("Z").format(calendar.getTime());
		    smTimezoneOffset = timeZone.substring(0, 3) + ":"+ timeZone.substring(3, 5);
		}
		return smTimezoneOffset;
	}

	private static String smCountryCode = null;
	/**
	 * The country code the device is set to.
	 * 
	 * @return The country code.
	 */
	static public String countryCode(){
		if(smCountryCode == null){
			smCountryCode = Locale.getDefault().getCountry();
		}
		return smCountryCode;
	}

	private static String smLanguageCode = null;
	/**
	 * The language code the device is set to.
	 * 
	 * @return The language code.
	 */
	static public String languageCode(){
		if(smLanguageCode == null){
			smLanguageCode = Locale.getDefault().getLanguage();
		}
		return smLanguageCode;
	}

	private static String smLocale = null;
	/**
	 * The locale of the device in.
	 * 
	 * @return The locale.
	 */
	static public String locale(){
		if(smLocale == null){
			smLocale = Locale.getDefault().toString();
		}
		return smLocale;
	}
}

