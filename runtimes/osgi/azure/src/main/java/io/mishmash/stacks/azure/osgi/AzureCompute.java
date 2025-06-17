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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.Region;
import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.compute.models.ComputeResourceType;
import com.azure.resourcemanager.compute.models.ComputeSku;
import com.azure.resourcemanager.compute.models.Disk;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachineScaleSet;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;

import io.mishmash.stacks.common.SoftRefMemoizableAction;

@Component(service={AzureCompute.class}, immediate=true)
public class AzureCompute extends AzureAPIBase {

    private static final Logger LOG = Logger.getLogger(
            AzureCompute.class.getName());

    /*
     * Refresh our ComputeManager instance once in a while,
     * as the credentials (and profile) that it uses might
     * have changed. (The tenant id in particular, see
     * parent class).
     */
    private static final Duration MAX_AGE_COMPUTE =
            Duration.of(30, ChronoUnit.SECONDS);

    private SoftRefMemoizableAction<ComputeManager> computeMemo
        = new SoftRefMemoizableAction<ComputeManager>(MAX_AGE_COMPUTE) {
            @Override
            protected CompletableFuture<ComputeManager> prepareAction() {
                return awaitAzureProfile()
                        .thenApply(p ->
                                ComputeManager.authenticate(
                                        getDefaultCredential(p),
                                        p))
                        .whenComplete((m, e) -> {
                            if (e == null) {
                                return;
                            }

                            LOG.log(Level.WARNING,
                                    """
                                    Azure Compute authentication failed. \
                                    Host, disk, netowrk and other Compute \
                                    resource details will be missing.""",
                                    e);
                        });
            }
        };

    @Activate
    public AzureCompute(
            @Reference final MemoizedIMDSInstance instance,
            @Reference final MemoizedIMDSIdentityInfo identity) {
        super(instance, identity);
    }

    public ComputeManager getComputeManager() {
        return computeMemo.uncheckedGet();
    }

    public Optional<Disk> getDiskById(final String resourceId) {
        return Optional.ofNullable(getComputeManager())
                .map(ComputeManager::disks)
                .map(d -> d.getById(resourceId));
    }

    public Optional<Disk> getDiskByName(final String diskName) {
        return getResourceGroupName()
                .flatMap(rg -> getDiskByName(rg, diskName));
    }

    public Optional<Disk> getDiskByName(
            final String resourceGroupName,
            final String diskName) {
        return Optional.ofNullable(getComputeManager())
                .map(ComputeManager::disks)
                .map(d -> d.getByResourceGroup(
                        resourceGroupName, diskName));
    }

    public Optional<VirtualMachine> getVMById(final String resourceId) {
        return Optional.ofNullable(getComputeManager())
                .map(ComputeManager::virtualMachines)
                .map(d -> d.getById(resourceId));
    }

    public Optional<VirtualMachine> getVMByName(final String vmName) {
        return getResourceGroupName()
                .flatMap(rg -> getVMByName(rg, vmName));
    }

    public Optional<VirtualMachine> getVMByName(
            final String resourceGroupName,
            final String vmName) {
        return Optional.ofNullable(getComputeManager())
                .map(ComputeManager::virtualMachines)
                .map(d -> d.getByResourceGroup(
                        resourceGroupName, vmName));
    }

    public Optional<VirtualMachineScaleSet> 
            getVMScaleSetById(final String resourceId) {
        return Optional.ofNullable(getComputeManager())
                .map(ComputeManager::virtualMachineScaleSets)
                .map(d -> d.getById(resourceId));
    }

    public Optional<VirtualMachineScaleSet>
            getVMScaleSetByName(final String vmScaleSetName) {
        return getResourceGroupName()
                .flatMap(rg -> getVMScaleSetByName(rg, vmScaleSetName));
    }

    public Optional<VirtualMachineScaleSet>
            getVMScaleSetByName(
                    final String resourceGroupName,
                    final String vmScaleSetName) {
        return Optional.ofNullable(getComputeManager())
                .map(ComputeManager::virtualMachineScaleSets)
                .map(d -> d.getByResourceGroup(
                        resourceGroupName, vmScaleSetName));
    }

    public Optional<ResourceGroup> getResourceGroup() {
        return Optional.ofNullable(getComputeManager())
                .map(ComputeManager::resourceManager)
                .map(ResourceManager::resourceGroups)
                .flatMap(g -> getResourceGroupName()
                                .map(n -> g.getByName(n)));
    }

    protected Optional<PagedIterable<ComputeSku>> getSkus(
            final Region region,
            final ComputeResourceType resourceType) {
        return Optional.ofNullable(getComputeManager())
            .map(ComputeManager::computeSkus)
            .map(s -> s.listByRegionAndResourceType(region, resourceType));
    }

    protected Optional<ComputeSku> findSkuByName(
            final PagedIterable<ComputeSku> skus,
            final String skuName) {
        for (ComputeSku sku : skus) {
            if (sku.name().getValue().equals(skuName)) {
                return Optional.of(sku);
            }
        }

        return Optional.empty();
    }

    public Optional<ComputeSku> getDiskSkuLocal(final String sku) {
        return getRegion()
                .flatMap(r -> getSkus(r, ComputeResourceType.DISKS))
                .flatMap(i -> findSkuByName(i, sku));
    }

    public Optional<ComputeSku> getVMSkuLocal(final String sku) {
        return getRegion()
                .flatMap(r -> getSkus(r, ComputeResourceType.VIRTUALMACHINES))
                .flatMap(i -> findSkuByName(i, sku));
    }

    public Optional<ComputeSku> getDiskSku(
            final String region,
            final String sku) {
        return getRegion()
                .flatMap(r -> getSkus(
                        Region.fromName(region),
                        ComputeResourceType.DISKS))
                .flatMap(i -> findSkuByName(i, sku));
    }

    public Optional<ComputeSku> getVMSku(
            final String region,
            final String sku) {
        return getRegion()
                .flatMap(r -> getSkus(
                        Region.fromName(region),
                        ComputeResourceType.VIRTUALMACHINES))
                .flatMap(i -> findSkuByName(i, sku));
    }
}
