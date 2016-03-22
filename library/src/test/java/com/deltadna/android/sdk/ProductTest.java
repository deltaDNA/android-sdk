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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class ProductTest {
    
    private Product uut;
    
    @Before
    public void before() {
        uut = new Product();
    }
    
    @After
    public void after() {
        uut = null;
    }
    
    @Test
    public void empty() {
        assertThat(uut.toJson().toString())
                .isEqualTo(new JSONObject().toString());
    }
    
    @Test
    public void setRealCurrency() throws JSONException {
        uut.setRealCurrency("type", 1);
        
        final JSONObject result = uut.toJson();
        assertThat(result.getJSONObject("realCurrency").get("realCurrencyType"))
                .isEqualTo("type");
        assertThat(result.getJSONObject("realCurrency").get("realCurrencyAmount"))
                .isEqualTo(1);
    }
    
    @Test
    public void addVirtualCurrency() throws JSONException {
        uut.addVirtualCurrency("nameOne", "typeOne", 1);
        uut.addVirtualCurrency("nameTwo", "typeTwo", 2);
        
        final JSONArray result =
                uut.toJson().getJSONArray("virtualCurrencies");
        assertThat(result.getJSONObject(0).getJSONObject("virtualCurrency")
                .get("virtualCurrencyName")).isEqualTo("nameOne");
        assertThat(result.getJSONObject(0).getJSONObject("virtualCurrency")
                .get("virtualCurrencyType")).isEqualTo("typeOne");
        assertThat(result.getJSONObject(0).getJSONObject("virtualCurrency")
                .get("virtualCurrencyAmount")).isEqualTo(1);
        assertThat(result.getJSONObject(1).getJSONObject("virtualCurrency")
                .get("virtualCurrencyName")).isEqualTo("nameTwo");
        assertThat(result.getJSONObject(1).getJSONObject("virtualCurrency")
                .get("virtualCurrencyType")).isEqualTo("typeTwo");
        assertThat(result.getJSONObject(1).getJSONObject("virtualCurrency")
                .get("virtualCurrencyAmount")).isEqualTo(2);
    }
    
    @Test
    public void addItem() throws JSONException {
        uut.addItem("nameOne", "typeOne", 1);
        uut.addItem("nameTwo", "typeTwo", 2);
        
        final JSONArray result = uut.toJson().getJSONArray("items");
        assertThat(result.getJSONObject(0).getJSONObject("item")
                .get("itemName")).isEqualTo("nameOne");
        assertThat(result.getJSONObject(0).getJSONObject("item")
                .get("itemType")).isEqualTo("typeOne");
        assertThat(result.getJSONObject(0).getJSONObject("item")
                .get("itemAmount")).isEqualTo(1);
        assertThat(result.getJSONObject(1).getJSONObject("item")
                .get("itemName")).isEqualTo("nameTwo");
        assertThat(result.getJSONObject(1).getJSONObject("item")
                .get("itemType")).isEqualTo("typeTwo");
        assertThat(result.getJSONObject(1).getJSONObject("item")
                .get("itemAmount")).isEqualTo(2);
    }
}
