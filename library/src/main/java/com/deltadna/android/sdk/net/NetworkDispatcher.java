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

package com.deltadna.android.sdk.net;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.deltadna.android.sdk.BuildConfig;
import com.deltadna.android.sdk.listeners.RequestListener;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handles enqueuing of network requests on an {@link ExecutorService}
 * and calling back on the listeners (if supplied) either with the
 * success result or the failure cause.
 */
final class NetworkDispatcher {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + NetworkDispatcher.class.getSimpleName();
    private static final int MAX_REQUESTS = 10;
    
    private final Map<Request, Cancelable> requests =
            new ConcurrentHashMap<>(MAX_REQUESTS);
    
    private final Handler handler;
    private final ScheduledExecutorService executor;
    
    NetworkDispatcher(Handler handler) {
        this.handler = handler;
        this.executor = new NetworkExecutor(MAX_REQUESTS);
    }
    
    CancelableRequest enqueue(
            Request<Void> request,
            @Nullable RequestListener<Void> listener) {
        
        return enqueue(request, ResponseBodyConverter.NULL, listener);
    }
    
    <T> CancelableRequest enqueue(
            final Request<T> request,
            @Nullable ResponseBodyConverter<T> converter,
            @Nullable RequestListener<T> listener) {
        
        Log.d(TAG, "Enqueuing " + request);
        
        final Future<Response<T>> future = executor.submit(request
                .setConverter(converter)
                .setRequestListener(listener));
        
        final Cancelable cancelable = new Cancelable(future);
        requests.put(request, cancelable);
        
        return cancelable;
    }
    
    private final class NetworkExecutor extends ScheduledThreadPoolExecutor {

        NetworkExecutor(int maxRequests) {
            super(  0,
                    new ThreadFactory() {
                        private final ThreadFactory inner =
                                Executors.defaultThreadFactory();
                        
                        @Override
                        public Thread newThread(@NonNull Runnable r) {
                            final Thread thread = inner.newThread(r);
                            thread.setName(NetworkDispatcher.class.getSimpleName()
                                    + "-" + thread.getName());
                            return thread;
                        }
                    });
            
            setMaximumPoolSize(maxRequests);
        }
        
        @Override
        protected <V> RunnableScheduledFuture<V> decorateTask(
                Callable<V> callable,
                RunnableScheduledFuture<V> task) {
            
            if (callable instanceof Request) {
                final Request<V> request = (Request<V>) callable;
                
                return new RequestFuture<>(
                        super.decorateTask(callable, task),
                        request,
                        request.listener);
            } else {
                throw new IllegalArgumentException(String.format(
                        Locale.US,
                        "Only %s tasks allowed",
                        Request.class.getSimpleName()));
            }
        }
        
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            
            if (t == null && r instanceof RequestFuture) {
                final RequestFuture<Response> future = (RequestFuture) r;
                
                try {
                    final Response response = future.get();
                    
                    Log.d(TAG, String.format(
                            Locale.US,
                            "Successfully performed %s with %s",
                            future.request,
                            response));
                    
                    if (future.listener != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                future.listener.onCompleted(response);
                            }
                        });
                    }
                    
                    requests.remove(future.request);
                } catch (InterruptedException e) {
                    // TODO is this appropriate?
                    Thread.currentThread().interrupt();
                } catch (final ExecutionException e) {
                    Log.w(TAG, "Failed performing " + future.request, e);
                    
                    if (future.request.shouldRetry()) {
                        Log.w(TAG, "Retrying " + future.request);
                        
                        final Future newFuture = schedule(
                                future.request,
                                future.request.retryDelay,
                                TimeUnit.MILLISECONDS);
                        requests.get(future.request).setTask(newFuture);
                    } else if (future.listener != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                future.listener.onError(e.getCause());
                            }
                        });

                        requests.remove(future.request);
                    }
                } catch (CancellationException e) {
                    // TODO should the listener be notified of the cancellation?
                    Log.d(TAG, "Cancelled " + future.request);
                    
                    requests.remove(future.request);
                }
            } else if (t != null) {
                Log.e(TAG, "Failed executing task", t);
            }
        }
    }
    
    /**
     * Wrapper around a {@link RunnableScheduledFuture} which encapsulates
     * some fields which we'll need for later use.
     */
    private final class RequestFuture<V> implements RunnableScheduledFuture<V> {

        private final RunnableScheduledFuture<V> delegate;
        private final Request<V> request;
        @Nullable
        private final RequestListener<V> listener;

        private RequestFuture(
                RunnableScheduledFuture<V> delegate,
                Request<V> request,
                @Nullable RequestListener<V> listener) {

            this.delegate = delegate;
            this.request = request;
            this.listener = listener;
        }

        @Override
        public boolean isPeriodic() {
            return delegate.isPeriodic();
        }

        @Override
        public void run() {
            delegate.run();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return delegate.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return delegate.get();
        }

        @Override
        public V get(long timeout, @NonNull TimeUnit unit) throws
                InterruptedException,
                ExecutionException,
                TimeoutException {
            
            return delegate.get(timeout, unit);
        }

        @Override
        public long getDelay(@NonNull TimeUnit unit) {
            return delegate.getDelay(unit);
        }

        @Override
        public int compareTo(@NonNull Delayed another) {
            return delegate.compareTo(another);
        }
    }
    
    /**
     * Wrapper around a {@link CancelableRequest} which allows us to
     * change the {@link Future} task to be cancelled, as a new instance
     * is created upon each re-submit.
     */
    private static final class Cancelable implements CancelableRequest {
        
        private Future task;
        
        Cancelable(Future task) {
            this.task = task;
        }
        
        @Override
        public synchronized void cancel() {
            task.cancel(false);
        }
        
        synchronized void setTask(Future task) {
            this.task = task;
        }
    }
}
