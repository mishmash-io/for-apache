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

package io.mishmash.stacks.azure.osgi;

import java.lang.ref.SoftReference;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Component;

import io.mishmash.gen.openapi.azure.imds.ApiException;
import io.mishmash.gen.openapi.azure.imds.client.api.DefaultApi;
import io.mishmash.gen.openapi.azure.imds.client.model.Versions;
import io.mishmash.stacks.azure.utils.AzureConstants;
import io.mishmash.stacks.common.SoftRefMemoizableAction;

@Component(service={MemoizedIMDSVersions.class}, immediate=true)
public class MemoizedIMDSVersions extends SoftRefMemoizableAction<Versions> {

    private static final Logger LOG = Logger.getLogger(
            MemoizedIMDSVersions.class.getName());

    private static final Duration DEFAULT_MAX_AGE =
            Duration.of(1, ChronoUnit.DAYS);

    private SoftReference<String> latestVersion;

    public MemoizedIMDSVersions() {
        super(DEFAULT_MAX_AGE);
    }

    @Override
    protected CompletableFuture<Versions> prepareAction() {
        try {
            return new DefaultApi()
                .getVersions(AzureConstants.IMDS_PARAM_META)
                .orTimeout(2, TimeUnit.SECONDS)
                .whenComplete(this::postRunAction);
        } catch (ApiException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    protected void memoize(Versions item) {
        super.memoize(item);

        if (item != null) {
            latestVersion = new SoftReference<>(
                    item.getApiVersions().getLast());
        }
    }

    @Override
    public void clear() {
        super.clear();

        if (latestVersion != null) {
            latestVersion.clear();
        }

        latestVersion = null;
    }

    public String getLatestVersion()
            throws InterruptedException, ExecutionException {
        if (hasMemoized()) {
            return latestVersion.get();
        } else {
            return getResult()
                    .thenApply(v -> v.getApiVersions().getLast())
                    .get();
        }
    }

    protected void postRunAction(final Versions v, final Throwable t) {
        if (t != null) {
            LOG.log(Level.WARNING,
                    """
                    Failed to get supported Azure IMDS API versions""",
                    t);
        }
    }
}
