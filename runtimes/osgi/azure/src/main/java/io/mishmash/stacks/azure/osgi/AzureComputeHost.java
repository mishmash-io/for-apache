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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.arch.Processor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.azure.resourcemanager.compute.models.ComputeSku;
import com.azure.resourcemanager.compute.models.ResourceSkuCapabilities;

import io.mishmash.gen.openapi.azure.imds.client.model.Compute;
import io.mishmash.gen.openapi.azure.imds.client.model.Instance;
import io.mishmash.gen.openapi.azure.imds.client.model.TagsProperties;
import io.mishmash.stacks.azure.utils.AzureConstants;
import io.mishmash.stacks.common.SoftRefMemoizableAction;
import io.mishmash.stacks.common.compute.ComputeHost;
import io.mishmash.stacks.common.compute.ComputeProvider;

@Component(service={ComputeHost.class}, immediate=true)
public class AzureComputeHost implements ComputeHost {

    protected static final String VM_SKU_CAPABILITY_CPUS = "vCPUs";
    protected static final String VM_SKU_CAPABILITY_MEM_SIZE = "MemoryGB";
    protected static final String VM_SKU_CAPABILITY_CPU_ARCH = "CpuArchitectureType";

    protected static final String VM_ARCH_X86 = "x64";
    protected static final String VM_ARCH_ARM = "Arm64";

    /*
     * Assign a max age to requests fetching the VM tags, as they
     * may change dynamically. The rest of the params - the id of the
     * VM, its SKU, the Azure environment will hardly change, so,
     * don't refresh them.
     */
    private static final Duration MAX_AGE_TAGS =
            Duration.of(30, ChronoUnit.SECONDS);
    private static final Duration MAX_AGE_VMID = null;
    private static final Duration MAX_AGE_MODEL = null;
    private static final Duration MAX_AGE_SKU = null;

    private MemoizedIMDSInstance imds;
    private AzureCompute compute;
    private AzureProvider azure;

    private SoftRefMemoizableAction<String> vmIdMemo
        = new SoftRefMemoizableAction<String>(MAX_AGE_VMID) {
            @Override
            protected CompletableFuture<String> prepareAction() {
                return imds.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(Compute::getVmId);
            }
        };
    private SoftRefMemoizableAction<String> modelMemo
        = new SoftRefMemoizableAction<String>(MAX_AGE_MODEL) {
            @Override
            protected CompletableFuture<String> prepareAction() {
                return imds.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(Compute::getVmSize);
            }
        };
    private SoftRefMemoizableAction<List<TagsProperties>> tagsMemo
        = new SoftRefMemoizableAction<List<TagsProperties>>(MAX_AGE_TAGS) {
            @Override
            protected CompletableFuture<List<TagsProperties>> prepareAction() {
                return imds.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(Compute::getTagsList)
                        .thenApply(l -> l == null ? List.of() : l);
            }
        };
    private SoftRefMemoizableAction<ComputeSku> skuMemo
        = new SoftRefMemoizableAction<ComputeSku>(MAX_AGE_SKU) {
            @Override
            protected CompletableFuture<ComputeSku> prepareAction() {
                return modelMemo.getResult()
                        .thenApply(compute::getVMSkuLocal)
                        .thenApply(o -> o.orElse(null));
            }
        };

    @Activate
    public AzureComputeHost(
            @Reference final MemoizedIMDSInstance metadataService,
            @Reference final AzureCompute computeService,
            @Reference final AzureProvider azureProvider) {
        this.imds = metadataService;
        this.compute = computeService;
        this.azure = azureProvider;
    }

    @Override
    public String uniqueId() {
        return vmIdMemo.uncheckedGet();
    }

    @Override
    public Optional<Integer> getMemoryMB() {
        return Optional.ofNullable(skuMemo.uncheckedGet())
                .map(ComputeSku::capabilities)
                .flatMap(l -> l.stream()
                    .filter(c -> c.name().equals(VM_SKU_CAPABILITY_MEM_SIZE))
                    .findAny())
                .map(ResourceSkuCapabilities::value)
                .map(Double::valueOf)
                .map(d -> (int)(d * 1024));
    }

    @Override
    public Optional<Integer> getNumCPUCores() {
        return Optional.ofNullable(skuMemo.uncheckedGet())
                .map(ComputeSku::capabilities)
                .flatMap(l -> l.stream()
                    .filter(c -> c.name().equals(VM_SKU_CAPABILITY_CPUS))
                    .findAny())
                .map(ResourceSkuCapabilities::value)
                .map(Integer::valueOf);
    }

    public Optional<Processor> getCPUArchitecture() {
        return Optional.ofNullable(skuMemo.uncheckedGet())
                .map(ComputeSku::capabilities)
                .flatMap(l -> l.stream()
                    .filter(c -> c.name().equals(VM_SKU_CAPABILITY_CPU_ARCH))
                    .findAny())
                .map(ResourceSkuCapabilities::value)
                .map(s -> {
                    if (s.equals(VM_ARCH_X86)) {
                        return new Processor(
                                Processor.Arch.BIT_64,
                                Processor.Type.X86);
                    } else if (s.equals(VM_ARCH_ARM)) {
                        return new Processor(
                                Processor.Arch.BIT_64,
                                Processor.Type.AARCH_64);
                    } else {
                        return new Processor(
                                Processor.Arch.UNKNOWN,
                                Processor.Type.UNKNOWN);
                    }
                });
    }

    @Override
    public Optional<Boolean> isVirtual() {
        return Optional.of(Boolean.TRUE);
    }

    @Override
    public Optional<String> getTier() {
        return Optional.ofNullable(tagsMemo.uncheckedGet())
            .flatMap(l -> l.stream()
                    .filter(p -> AzureConstants.TAG_TIER.equals(p.getName()))
                    .findAny())
            .map(TagsProperties::getValue);
    }

    @Override
    public Optional<String> getModel() {
        return Optional.ofNullable(modelMemo.uncheckedGet());
    }

    @Override
    public ComputeProvider getProvider() {
        return azure;
    }
}
