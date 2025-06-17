/*
 *    Copyright 2025 Mishmash IO UK Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mishmash.stacks.common;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public abstract class MemoizableAction<T> {

    private AtomicReference<CompletableFuture<Void>> futureRef =
            new AtomicReference<>(null);
    private Instant lastUpdated;

    protected abstract CompletableFuture<T> prepareAction();
    protected abstract void memoize(T item);
    protected abstract boolean hasMemoized();
    protected abstract T getMemoized();

    public T get() throws InterruptedException, ExecutionException {
        return getResult()
                .get();
    }

    public T uncheckedGet() {
        try {
            return get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void clear() {
        lastUpdated = Instant.MIN;
    }

    public CompletableFuture<T> getResult() {
        if (hasMemoized()) {
            return CompletableFuture.completedFuture(getMemoized());
        }

        return awaitCompletion()
            .thenApply(v -> getMemoized());
    }

    protected CompletableFuture<Void> awaitCompletion() {
        return futureRef
                .updateAndGet(f -> f == null || !hasMemoized()
                                        ? memoizeAction(prepareAction())
                                        : f);
    }

    private Void memoizeInternal(final T item, final Throwable error) {
        memoize(item);
        lastUpdated = Instant.now();

        return null;
    }

    private CompletableFuture<Void> memoizeAction(
            final CompletableFuture<T> action) {
        /*
         * Drop the original CompletableFuture<T> to
         * release its reference to T. Replace with a
         * null Void (returned by memoizeInternal()) instead.
         */
        return action.handle(this::memoizeInternal);
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }
}
