package com.deltadna.android.sdk.util;

import java.util.Iterator;

public interface CloseableIterator<T> extends Iterator<T> {
    
    void close(boolean clear);
}
