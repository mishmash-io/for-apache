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

package io.mishmash.stacks.compute.azure.osgi;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Component;

import io.mishmash.stacks.compute.azure.gen.openapi.imds.ApiException;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.api.DefaultApi;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.AttestedData;
import io.mishmash.stacks.compute.azure.utils.AzureConstants;
import io.mishmash.stacks.common.SoftRefMemoizableAction;

@Component(service={MemoizedIMDSAttestedData.class}, immediate=true)
public class MemoizedIMDSAttestedData
        extends SoftRefMemoizableAction<AttestedData> {

    private static final Logger LOG = Logger.getLogger(
            MemoizedIMDSAttestedData.class.getName());

    private static final Duration DEFAULT_MAX_AGE =
            Duration.of(10, ChronoUnit.MINUTES);

    public MemoizedIMDSAttestedData() {
        super(DEFAULT_MAX_AGE);
    }

    @Override
    protected CompletableFuture<AttestedData> prepareAction() {
        try {
            return new DefaultApi()
                    .attestedGetDocument(
                            AzureConstants.IMDS_VER_ATTESTED_DATA,
                            AzureConstants.IMDS_PARAM_META,
                            null)
                    .orTimeout(2, TimeUnit.SECONDS)
                    .whenComplete(this::actionPostRun);
        } catch (ApiException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    protected void actionPostRun(final AttestedData d, final Throwable t) {
        if (t != null) {
            LOG.log(Level.WARNING,
                    """
                    Failed to get Azure attestation, \
                    expect degraded functionality.""",
                    t);
        }
    }
}
