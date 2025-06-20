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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.Compute;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.Instance;
import io.mishmash.stacks.common.SoftRefMemoizableAction;
import io.mishmash.stacks.compute.common.ComputeProvider;
import io.mishmash.stacks.compute.common.Placement;
import io.mishmash.stacks.compute.common.PlacementDomain;
import io.mishmash.stacks.compute.common.PlacementGeography;
import io.mishmash.stacks.compute.common.PlacementGroup;
import io.mishmash.stacks.compute.common.PlacementRegion;
import io.mishmash.stacks.compute.common.PlacementZone;

@Component(service={Placement.class}, immediate=true)
public class AzurePlacement implements Placement {

    /*
     * Don't refresh settings, they won't change without the VM
     * being restarted.
     */
    private static final Duration MAX_AGE_REGION = null;
    private static final Duration MAX_AGE_ZONE = null;
    private static final Duration MAX_AGE_GROUP = null;
    private static final Duration MAX_AGE_UPDATE_DOMAIN = null;
    private static final Duration MAX_AGE_FAULT_DOMAIN = null;

    private MemoizedIMDSInstance instance;
    private AzureProvider azure;

    private SoftRefMemoizableAction<String> regionMemo
        = new SoftRefMemoizableAction<String>(MAX_AGE_REGION) {
            @Override
            protected CompletableFuture<String> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(Compute::getLocation);
            }
        };
    private SoftRefMemoizableAction<String> zoneMemo
        = new SoftRefMemoizableAction<String>(MAX_AGE_ZONE) {
            @Override
            protected CompletableFuture<String> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(Compute::getZone);
            }
        };
    private SoftRefMemoizableAction<String> groupMemo
        = new SoftRefMemoizableAction<String>(MAX_AGE_GROUP) {
            @Override
            protected CompletableFuture<String> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(Compute::getPlacementGroupId);
            }
        };
    private SoftRefMemoizableAction<String> updateDomainMemo
        = new SoftRefMemoizableAction<String>(MAX_AGE_UPDATE_DOMAIN) {
            @Override
            protected CompletableFuture<String> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(Compute::getPlatformUpdateDomain);
            }
        };
    private SoftRefMemoizableAction<String> faultDomainMemo
        = new SoftRefMemoizableAction<String>(MAX_AGE_FAULT_DOMAIN) {
            @Override
            protected CompletableFuture<String> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(Compute::getPlatformFaultDomain);
            }
        };

    @Activate
    public AzurePlacement(
            @Reference final MemoizedIMDSInstance imdsService,
            @Reference final AzureProvider azureProvider) {
        instance = imdsService;
        azure = azureProvider;
    }

    @Override
    public Optional<PlacementGeography> getGeography() {
        // TODO get some hard-coded values?
        return Optional.empty();
    }

    @Override
    public Optional<PlacementRegion> getRegion() {
        return Optional.ofNullable(regionMemo.uncheckedGet())
                .map(AzureRegion::new);
    }

    @Override
    public Optional<PlacementZone> getZone() {
        return Optional.ofNullable(zoneMemo.uncheckedGet())
                .map(AzureZone::new);
    }

    @Override
    public Optional<PlacementGroup> getGroup() {
        return Optional.ofNullable(groupMemo.uncheckedGet())
                .map(AzureGroup::new);
    }

    @Override
    public Optional<PlacementDomain> getFaultDomain() {
        return Optional.ofNullable(faultDomainMemo.uncheckedGet())
                .map(AzureDomain::new);
    }

    @Override
    public Optional<PlacementDomain> getUpdateDomain() {
        return Optional.ofNullable(updateDomainMemo.uncheckedGet())
                .map(AzureDomain::new);
    }

    @Override
    public ComputeProvider getProvider() {
        return azure;
    }
}
