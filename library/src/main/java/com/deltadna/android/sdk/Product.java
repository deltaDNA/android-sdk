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

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Constructs a product, to be used in conjunction with a {@link Transaction}.
 */
public class Product<T extends Product<T>> implements JsonParams {
    
    final Params realCurrency = new Params();
    final JSONArray virtualCurrencies = new JSONArray();
    final JSONArray items = new JSONArray();
    
    /**
     * Creates a new instance.
     */
    public Product() {}
    
    @Override
    public JSONObject toJson() {
        final JSONObject contents = new JSONObject();
        
        try {
            if (!realCurrency.isEmpty()) {
                contents.put("realCurrency", realCurrency.json);
            }
            if (virtualCurrencies.length() > 0) {
                contents.put("virtualCurrencies", virtualCurrencies);
            }
            if (items.length() > 0) {
                contents.put("items", items);
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        
        return contents;
    }
    
    /**
     * Set a real currency for the product.
     *
     * @param type      type of the currency, using an ISO-4217 3 character
     *                  currency code
     * @param amount    amount of the currency, using the minor currency unit
     *
     * @return this {@link T} instance
     *
     * @throws IllegalArgumentException if the {@code type} is null or empty
     *
     * @see #convertCurrency(DDNA, String, float)
     */
    public T setRealCurrency(String type, int amount) {
        realCurrency
                .put("realCurrencyType", type)
                .put("realCurrencyAmount", amount);
        
        return (T) this;
    }
    
    /**
     * Adds a virtual currency to the product.
     *
     * @param name      name of the currency
     * @param type      type of the currency, using an ISO-4217 3 character
     *                  currency code
     * @param amount    amount of the currency, using the minor currency unit
     *
     * @return this {@link T} instance
     *
     * @throws IllegalArgumentException if the {@code name} is null or empty
     * @throws IllegalArgumentException if the {@code type} is null or empty
     */
    public T addVirtualCurrency(String name, String type, int amount) {
        virtualCurrencies.put(new Params().put(
                "virtualCurrency",
                new Params()
                        .put("virtualCurrencyName", name)
                        .put("virtualCurrencyType", type)
                        .put("virtualCurrencyAmount", amount)).json);
        
        return (T) this;
    }
    
    /**
     * Adds an item to the product.
     *
     * @param name      name of the item
     * @param type      type of the item
     * @param amount    amount of the item
     *
     * @return this {@link T} instance
     *
     * @throws IllegalArgumentException if the {@code name} is null or empty
     * @throws IllegalArgumentException if the {@code type} is null or empty
     */
    public T addItem(String name, String type, int amount) {
        items.put(new Params().put(
                "item",
                new Params()
                        .put("itemName", name)
                        .put("itemType", type)
                        .put("itemAmount", amount)).json);
        
        return (T) this;
    }
    
    /**
     * Converts a currency in a floating point format with a decimal point,
     * such as '1.23' EUR, into an integer representation which can be used
     * with {@link #setRealCurrency(String, int)}. This method will also work
     * for currencies which don't use a minor currency unit, for example such
     * as the Japanese Yen (JPY).
     *
     * @param ddna  the SDK instance
     * @param code  the ISO 4217 currency code
     * @param value the currency value to convert
     *
     * @return the converted integer value
     */
    public static int convertCurrency(DDNA ddna, String code, float value) {
        if (ddna.getIso4217().containsKey(code)) {
            return new Float(value * Math.pow(10, ddna.getIso4217().get(code)))
                    .intValue();
        } else {
            Log.w(BuildConfig.LOG_TAG, "Failed to find currency for: " + code);
            return 0;
        }
    }
}
