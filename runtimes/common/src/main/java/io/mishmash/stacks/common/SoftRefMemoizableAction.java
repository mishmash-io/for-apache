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

import java.lang.ref.SoftReference;
import java.time.Duration;
import java.time.Instant;

public abstract class SoftRefMemoizableAction<T>
        extends MemoizableAction<T> {

    private SoftReference<T> ref;
    private Duration maxAge;

    public SoftRefMemoizableAction() {
        // no-arg constructor
    }

    public SoftRefMemoizableAction(final Duration newMaxAge) {
        maxAge = newMaxAge;
    }

    @Override
    protected void memoize(T item) {
        ref = new SoftReference<>(item);
    }

    @Override
    protected boolean hasMemoized() {
        return ref != null && !ref.refersTo(null)
                && (hasMaxAge() ? checkAge() : true);
    }

    @Override
    protected T getMemoized() {
        return ref.get();
    }

    @Override
    public void clear() {
        super.clear();

        if (ref != null) {
            ref.clear();
        }

        ref = null;
    }

    protected boolean checkAge() {
        return getLastUpdated()
                .isAfter(Instant.now().minus(maxAge));
    }

    public boolean hasMaxAge() {
        return maxAge != null;
    }

    public void setMaxAge(final Duration newMaxAge) {
        maxAge = newMaxAge;
    }

    public Duration getMaxAge() {
        return maxAge;
    }
}
