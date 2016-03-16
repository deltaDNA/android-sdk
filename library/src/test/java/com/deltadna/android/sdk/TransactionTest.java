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

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class TransactionTest {
    
    @Test
    public void ctor() throws JSONException {
        final Transaction uut = new Transaction(
                "name",
                "type",
                new Product(),
                new Product());
        
        assertThat(uut.name).isEqualTo("transaction");
        
        assertThat(uut.params.json.get("transactionName")).isEqualTo("name");
        assertThat(uut.params.json.get("transactionType")).isEqualTo("type");
        
        assertThat(uut.params.json.getJSONObject("productsReceived").toString())
                .isEqualTo(new Product().toJson().toString());
        assertThat(uut.params.json.getJSONObject("productsSpent").toString())
                .isEqualTo(new Product().toJson().toString());
    }
    
    @Test
    public void setId() throws JSONException {
        assertThat(transaction().setId("value").params.json.get("transactionID"))
                .isEqualTo("value");
    }
    
    @Test
    public void setProductId() throws JSONException {
        assertThat(transaction().setProductId("value").params.json.get("productID"))
                .isEqualTo("value");
    }
    
    @Test
    public void setReceipt() throws JSONException {
        assertThat(transaction().setReceipt("value").params.json.get("transactionReceipt"))
                .isEqualTo("value");
    }
    
    @Test
    public void setServer() throws JSONException {
        assertThat(transaction().setServer("value").params.json.get("transactionServer"))
                .isEqualTo("value");
    }
    
    @Test
    public void setTransactorId() throws JSONException {
        assertThat(transaction().setTransactorId("value").params.json.get("transactorID"))
                .isEqualTo("value");
    }
    
    private static Transaction transaction() {
        return new Transaction("name", "type", new Product(), new Product());
    }
}
