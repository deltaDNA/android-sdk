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

import android.text.TextUtils;

import com.deltadna.android.sdk.helpers.Preconditions;

/**
 * Constructs a transaction {@link Event}.
 */
public class Transaction<T extends Transaction<T>> extends Event<T> {
    
    /**
     * Creates a new instance.
     *
     * @param name              the transaction's name
     * @param type              the transaction's type
     * @param productsReceived  the received products
     * @param productsSpent     the spent products
     *
     * @throws IllegalArgumentException if the {@code productsReceived} is null
     * @throws IllegalArgumentException if the {@code productsSpent} is null
     * 
     */
    public Transaction(
            String name,
            String type,
            Product productsReceived,
            Product productsSpent) {
        
        super("transaction");
        
        putParam("transactionName", name);
        putParam("transactionType", type);
        
        putParam("productsReceived", productsReceived);
        putParam("productsSpent", productsSpent);
    }
    
    @Override
    public T putParam(String key, Object value) {
        return super.putParam(key, value);
    }
    
    @Override
    public T putParam(String key, JsonParams value) {
        return super.putParam(key, value);
    }
    
    /**
     * Sets the transaction id.
     *
     * @param id the transaction id
     *
     * @return this {@link T} instance
     *
     * @throws IllegalArgumentException if the {@code id} is null or empty
     */
    public T setId(String id) {
        Preconditions.checkArg(
                !TextUtils.isEmpty(id),
                "id cannot be null or empty");
        
        return putParam("transactionID", id);
    }
    
    /**
     * Sets the product id.
     *
     * @param productId the product id
     *
     * @return this {@link T} instance
     *
     * @throws IllegalArgumentException if the {@code productId} is null or
     *                                  empty
     */
    public T setProductId(String productId) {
        Preconditions.checkArg(
                !TextUtils.isEmpty(productId),
                "productId cannot be null or empty");
        
        return putParam("productID", productId);
    }
    
    /**
     * Sets the transaction receipt.
     *
     * @param receipt the receipt
     *
     * @return this {@link T} instance
     *
     * @throws IllegalArgumentException if the {@code receipt} is null or empty
     */
    public T setReceipt(String receipt) {
        Preconditions.checkArg(
                !TextUtils.isEmpty(receipt),
                "receipt cannot be null or empty");
        
        return putParam("transactionReceipt", receipt);
    }
    
    /**
     * Sets the transaction server.
     *
     * @param server the server
     *
     * @return this {@link T} instance
     *
     * @throws IllegalArgumentException if the {@code receipt} is null or empty
     */
    public T setServer(String server) {
        Preconditions.checkArg(
                !TextUtils.isEmpty(server),
                "server cannot be null or empty");
        
        return putParam("transactionServer", server);
    }
    
    /**
     * Sets the transactor id.
     *
     * @param transactorId the transactor id
     *
     * @return this {@link T} instance
     *
     * @throws IllegalArgumentException if the {@code transactorId} is null or
     *                                  empty
     */
    public T setTransactorId(String transactorId) {
        Preconditions.checkArg(
                !TextUtils.isEmpty(transactorId),
                "transactorId cannot be null or empty");
        
        return putParam("transactorID", transactorId);
    }
}
