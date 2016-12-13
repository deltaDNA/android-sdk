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

import android.os.Build
import com.deltadna.android.sdk.test.assertThrown
import com.google.common.truth.Truth.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml",
        sdk = intArrayOf(Build.VERSION_CODES.M))
class ProductTest {
    
    private val ddna = DDNA(
            RuntimeEnvironment.application,
            "environmentKey",
            "collectUrl",
            "engageUrl",
            null,
            null,
            null,
            null,
            null)
    
    private var uut: Product<*>? = null
    
    @Before
    fun before() {
        uut = KProduct()
    }
    
    @After
    fun after() {
        uut = null
    }
    
    @Test
    fun empty() {
        assertThat(uut!!.toJson().toString()).isEqualTo(JSONObject().toString())
    }
    
    @Test
    @Throws(JSONException::class)
    fun setRealCurrency() {
        uut!!.setRealCurrency("type", 1)
        
        val result = uut!!.toJson()
        assertThat(result.getJSONObject("realCurrency").get("realCurrencyType"))
                .isEqualTo("type")
        assertThat(result.getJSONObject("realCurrency").get("realCurrencyAmount"))
                .isEqualTo(1)
    }
    
    @Test
    @Throws(JSONException::class)
    fun addVirtualCurrency() {
        uut!!.addVirtualCurrency("nameOne", "typeOne", 1)
        uut!!.addVirtualCurrency("nameTwo", "typeTwo", 2)
        
        val result = uut!!.toJson().getJSONArray("virtualCurrencies")
        assertThat(result.getJSONObject(0).getJSONObject("virtualCurrency")
                .get("virtualCurrencyName"))
                .isEqualTo("nameOne")
        assertThat(result.getJSONObject(0).getJSONObject("virtualCurrency")
                .get("virtualCurrencyType"))
                .isEqualTo("typeOne")
        assertThat(result.getJSONObject(0).getJSONObject("virtualCurrency")
                .get("virtualCurrencyAmount"))
                .isEqualTo(1)
        assertThat(result.getJSONObject(1).getJSONObject("virtualCurrency")
                .get("virtualCurrencyName"))
                .isEqualTo("nameTwo")
        assertThat(result.getJSONObject(1).getJSONObject("virtualCurrency")
                .get("virtualCurrencyType"))
                .isEqualTo("typeTwo")
        assertThat(result.getJSONObject(1).getJSONObject("virtualCurrency")
                .get("virtualCurrencyAmount"))
                .isEqualTo(2)
    }
    
    @Test
    @Throws(JSONException::class)
    fun addItem() {
        uut!!.addItem("nameOne", "typeOne", 1)
        uut!!.addItem("nameTwo", "typeTwo", 2)
        
        val result = uut!!.toJson().getJSONArray("items")
        assertThat(result.getJSONObject(0).getJSONObject("item").get("itemName"))
                .isEqualTo("nameOne")
        assertThat(result.getJSONObject(0).getJSONObject("item").get("itemType"))
                .isEqualTo("typeOne")
        assertThat(result.getJSONObject(0).getJSONObject("item").get("itemAmount"))
                .isEqualTo(1)
        assertThat(result.getJSONObject(1).getJSONObject("item").get("itemName"))
                .isEqualTo("nameTwo")
        assertThat(result.getJSONObject(1).getJSONObject("item").get("itemType"))
                .isEqualTo("typeTwo")
        assertThat(result.getJSONObject(1).getJSONObject("item").get("itemAmount"))
                .isEqualTo(2)
    }
    
    @Test
    fun convertCurrency() {
        assertThat(Product.convertCurrency(ddna, "EUR", 1.23f))
                .isEqualTo(123)
        assertThat(Product.convertCurrency(ddna, "JPY", 123f))
                .isEqualTo(123)
        assertThat(Product.convertCurrency(ddna, "KWD", 1.234f))
                .isEqualTo(1234)
    }
    
    @Test
    fun convertCurrencyFloors() {
        assertThat(Product.convertCurrency(ddna, "EUR", 1.235f))
                .isEqualTo(123)
    }
    
    @Test
    fun convertCurrencyZero() {
        assertThat(Product.convertCurrency(ddna, "EUR", 0f))
                .isEqualTo(0)
    }
    
    @Test
    fun convertCurrencyInvalidCode() {
        assertThat(Product.convertCurrency(ddna, "ZZZ", 1.23f))
                .isEqualTo(0)
    }
    
    @Test
    fun convertCurrencyThrowsOnInvalidInputs() {
        assertThrown<IllegalArgumentException> {
            Product.convertCurrency(null, "EUR", 1.23f)
        }
        assertThrown<IllegalArgumentException> {
            Product.convertCurrency(ddna, null, 1.23f)
        }
        assertThrown<IllegalArgumentException> {
            Product.convertCurrency(ddna, "", 1.23f)
        }
    }
    
    private class KProduct : Product<KProduct>()
}
