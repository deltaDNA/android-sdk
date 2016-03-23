package com.deltadna.android.sdk;

import android.support.annotation.Nullable;

interface EventStoreItem {
    
    boolean available();
    
    @Nullable
    String get();
}
