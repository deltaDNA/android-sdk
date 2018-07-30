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

/**
 * General utils class.
 */
public class Utils{
	/**
	 * Constructs a big endian integer from the given big endian ordered
	 * byte data.
	 * 
	 * @param bytes The bytes to use.
	 * 
	 * @return The integer result.
	 */
	static public int toInt32(byte[] bytes){
		int result = 0;
		
		result = ((bytes[0] & 0xff) << 24) | ((bytes[1] & 0xff) << 16) | ((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff);
		
		return result;
	}
	/**
	 * Encodes the given big endian integer to a big endian ordered array of bytes.
	 * 
	 * @param value The integer value to encode.
	 * 
	 * @return The encoded bytes.
	 */
	static public byte[] toBytes(int value){
		byte[] result = new byte[4];
		
		result[3] = (byte)(value & 0xff);
		result[2] = (byte)((value >> 8) & 0xff);
		result[1] = (byte)((value >> 16) & 0xff);
		result[0] = (byte)((value >> 24) & 0xff);
		
		return result;
	}
}
