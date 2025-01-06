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

package io.mishmash.stacks.oidc.protocol;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class RESTRenewer<T> {

    private static final Logger LOG =
            Logger.getLogger(RESTRenewer.class.getName());

    private RESTClient rest;
    private AtomicReference<CompletableFuture<T>> itemRef =
            new AtomicReference<>();

    public RESTRenewer(final RESTClient client) {
        rest = client;
    }

    protected abstract CompletableFuture<T> requestNew(T current);
    protected abstract boolean needsRefresh(T current);

    protected RESTClient restClient() {
        return rest;
    }

    protected T get() throws InterruptedException, ExecutionException {
        return itemRef.updateAndGet(this::getOrRefresh).get();
    }

    protected CompletableFuture<T> getOrRefresh(
            final CompletableFuture<T> current) {
        try {
            CompletableFuture<T> next = null;

            if (current == null
                    || current.isCancelled()
                    || current.isCompletedExceptionally()) {
                next = requestNew(null);
            } else if (current.isDone() && needsRefresh(current.get())) {
                next = requestNew(current.get());
            } else {
                return current;
            }

            return next.whenComplete((jwt, t) -> {
                if (t != null) {
                    LOG.log(Level.WARNING,
                            "Could not refresh OIDC item",
                            t);
                }
            });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
