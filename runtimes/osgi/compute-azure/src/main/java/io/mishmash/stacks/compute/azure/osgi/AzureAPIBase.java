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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.models.AzureCloud;
import com.azure.identity.DefaultAzureCredentialBuilder;

import io.mishmash.gen.openapi.azure.imds.client.model.Compute;
import io.mishmash.gen.openapi.azure.imds.client.model.IdentityInfoResponse;
import io.mishmash.gen.openapi.azure.imds.client.model.Instance;
import io.mishmash.stacks.common.SoftRefMemoizableAction;

public abstract class AzureAPIBase {

    private static final Logger LOG = Logger.getLogger(
            AzureAPIBase.class.getName());

    /*
     * Refresh the tenant id once in a while to allow an identity
     * to be assigned post-factum, but don't refresh the rest -
     * regions, subscriptions, resource groups won't change without
     * a restart.
     */
    private static final Duration MAX_AGE_TENANT =
            Duration.of(30, ChronoUnit.SECONDS);
    private static final Duration MAX_AGE_PROFILE = null;
    private static final Duration MAX_AGE_REGION = null;
    private static final Duration MAX_AGE_RESOURCE_GROUP = null;

    private MemoizedIMDSInstance instance;
    private MemoizedIMDSIdentityInfo identity;

    private SoftRefMemoizableAction<String> tenantIdMemo
        = new SoftRefMemoizableAction<String>(MAX_AGE_TENANT) {
            @Override
            protected CompletableFuture<String> prepareAction() {
                return identity.getResult()
                        .thenApply(IdentityInfoResponse::getTenantId)
                        .exceptionally(t -> {
                            /*
                             * We can't get the tenant id, log and
                             * use a default of null instead
                             */
                            LOG.log(Level.WARNING,
                                    """
                                        Could not determine our Azure \
                                        tenant id - a Managed Identity might \
                                        not have been assigned to us - Azure \
                                        services can be inaccessible.""",
                                    t);

                            return (String) null;
                        });
            }
        };
    private SoftRefMemoizableAction<AzureProfile> azProfileMemo
        = new SoftRefMemoizableAction<AzureProfile>(MAX_AGE_PROFILE) {
            @Override
            protected CompletableFuture<AzureProfile> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(c -> new AzureProfile(
                                // returns a default null tenant id:
                                tenantIdMemo.uncheckedGet(),
                                c.getSubscriptionId(), 
                                getAzureCloudByName(c.getAzEnvironment())))
                        .whenComplete((p, e) -> {
                            if (e == null) {
                                return;
                            }

                            LOG.log(Level.WARNING,
                                    """
                                    Could not find our Azure \
                                    authentication profile and endpoints, \
                                    Azure service requests may not \
                                    authenticate.""",
                                    e);
                        });
            }
        };
    private SoftRefMemoizableAction<Region> regionMemo
        = new SoftRefMemoizableAction<Region>(MAX_AGE_REGION) {
            @Override
            protected CompletableFuture<Region> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(Compute::getLocation)
                        .thenApply(Region::fromName)
                        .whenComplete((r, e) -> {
                            if (e == null) {
                                return;
                            }

                            LOG.log(Level.WARNING,
                                    """
                                    Could not determine our Azure \
                                    Region. Regional \
                                    Azure service requests may fail.""",
                                    e);
                        });
            }
        };
    private SoftRefMemoizableAction<String> resourceGroupMemo
        = new SoftRefMemoizableAction<String>(MAX_AGE_RESOURCE_GROUP) {
            @Override
            protected CompletableFuture<String> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(Compute::getResourceGroupName)
                        .whenComplete((r, e) -> {
                            if (e == null) {
                                return;
                            }

                            LOG.log(Level.WARNING,
                                    """
                                    Could not determine our Azure \
                                    Resource Group. \
                                    Azure service requests may fail.""",
                                    e);
                        });
            }
        };

    public AzureAPIBase(
            final MemoizedIMDSInstance imdsInstance,
            final MemoizedIMDSIdentityInfo imdsIdentity) {
        instance = imdsInstance;
        identity = imdsIdentity;
    }

    protected AzureCloud getAzureCloudByName(String name) {
        if (AzureProvider.AZURE_CHINA_CLOUD.equals(name)) {
            return AzureCloud.AZURE_CHINA_CLOUD;
        } else if (AzureProvider.AZURE_US_GOV_CLOUD.equals(name)) {
            return AzureCloud.AZURE_US_GOVERNMENT_CLOUD;
        } else if (AzureProvider.AZURE_PUBLIC_CLOUD.equals(name)) {
            return AzureCloud.AZURE_PUBLIC_CLOUD;
        } else {
            return null;
        }
    }

    protected TokenCredential getDefaultCredential(
            final AzureProfile profile) {
        return new DefaultAzureCredentialBuilder()
                .authorityHost(
                        profile
                            .getEnvironment()
                            .getActiveDirectoryEndpoint())
                .build();
    }

    protected CompletableFuture<AzureProfile> awaitAzureProfile() {
        return azProfileMemo.getResult();
    }

    public AzureProfile getAzureProfile() {
        return azProfileMemo.uncheckedGet();
    }

    public Optional<String> getSubscriptionId() {
        return Optional.ofNullable(getAzureProfile())
                .map(AzureProfile::getSubscriptionId);
    }

    public Optional<String> getTenantId() {
        return Optional.ofNullable(getAzureProfile())
                .map(AzureProfile::getTenantId);
    }

    public Optional<AzureEnvironment> getAzureEnvironment() {
        return Optional.ofNullable(getAzureProfile())
                .map(AzureProfile::getEnvironment);
    }

    public Optional<String> getResourceGroupName() {
        return Optional.ofNullable(
                resourceGroupMemo.uncheckedGet());
    }

    public Optional<Region> getRegion() {
        return Optional.ofNullable(regionMemo.uncheckedGet());
    }
}
