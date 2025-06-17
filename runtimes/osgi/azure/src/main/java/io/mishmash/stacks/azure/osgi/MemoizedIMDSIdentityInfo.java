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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Component;

import io.mishmash.gen.openapi.azure.imds.ApiException;
import io.mishmash.gen.openapi.azure.imds.client.api.GetMetadataInformationApi;
import io.mishmash.gen.openapi.azure.imds.client.model.IdentityInfoResponse;
import io.mishmash.stacks.azure.utils.AzureConstants;
import io.mishmash.stacks.common.SoftRefMemoizableAction;

@Component(service={MemoizedIMDSIdentityInfo.class}, immediate=true)
public class MemoizedIMDSIdentityInfo
        extends SoftRefMemoizableAction<IdentityInfoResponse> {

    private static final Logger LOG = Logger.getLogger(
            MemoizedIMDSIdentityInfo.class.getName());

    private static final Duration DEFAULT_MAX_AGE =
            Duration.of(1, ChronoUnit.MINUTES);

    public MemoizedIMDSIdentityInfo() {
        super(DEFAULT_MAX_AGE);
    }

    @Override
    protected CompletableFuture<IdentityInfoResponse> prepareAction() {
        try {
            return new GetMetadataInformationApi()
                    .identityGetInfo(
                            AzureConstants.IMDS_PARAM_META,
                            AzureConstants.IMDS_VER_IDENTITY_INFO)
                    .orTimeout(2, TimeUnit.SECONDS)
                    .whenComplete(this::postRunAction);
        } catch (ApiException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    protected void postRunAction(
            final IdentityInfoResponse r,
            final Throwable t) {
        if (t != null) {
            LOG.log(Level.WARNING,
                    """
                    Failed to get Azure identity details. \
                    Some functionalities might need an assigned \
                    identity.""",
                    t);
        }
    }
}
