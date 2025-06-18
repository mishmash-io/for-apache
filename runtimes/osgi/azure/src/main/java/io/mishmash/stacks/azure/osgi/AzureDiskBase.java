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

import com.azure.resourcemanager.compute.models.ComputeSku;
import com.azure.resourcemanager.compute.models.Disk;

import io.mishmash.gen.openapi.azure.imds.client.model.Compute;
import io.mishmash.gen.openapi.azure.imds.client.model.Instance;
import io.mishmash.stacks.azure.utils.AzureConstants;
import io.mishmash.stacks.common.SoftRefMemoizableAction;
import io.mishmash.stacks.common.compute.Storage;

public abstract class AzureDiskBase implements Storage {

    /*
     * Refresh the tier (and the Disk, as the tier comes
     * from the Disk specs) once in a while. All others
     * hardly change, so, don't refresh them.
     */
    private static final Duration MAX_AGE_DISK =
            Duration.of(30, ChronoUnit.SECONDS);
    private static final Duration MAX_AGE_TIER =
            Duration.of(30, ChronoUnit.SECONDS);
    private static final Duration MAX_AGE_ID = null;
    private static final Duration MAX_AGE_MODEL = null;
    private static final Duration MAX_AGE_SIZE = null;
    private static final Duration MAX_AGE_EPHEMERAL = null;
    private static final Duration MAX_AGE_SKU = null;

    private MemoizedIMDSInstance instance;
    private AzureCompute compute;

    private SoftRefMemoizableAction<String> idMemo
        = new SoftRefMemoizableAction<String>(MAX_AGE_ID) {
            @Override
            protected CompletableFuture<String> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(c -> getDiskName(c));
            }
        };
    private SoftRefMemoizableAction<String> modelMemo
        = new SoftRefMemoizableAction<String>(MAX_AGE_MODEL) {
            @Override
            protected CompletableFuture<String> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(c -> getDiskSku(c));
            }
        };
    private SoftRefMemoizableAction<Long> sizeMemo
        = new SoftRefMemoizableAction<Long>(MAX_AGE_SIZE) {
            @Override
            protected CompletableFuture<Long> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(c -> getDiskSize(c))
                        .thenApply(Long::valueOf);
            }
        };
    private SoftRefMemoizableAction<Boolean> ephemeralMemo
        = new SoftRefMemoizableAction<Boolean>(MAX_AGE_EPHEMERAL) {
            @Override
            protected CompletableFuture<Boolean> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(c -> isDiskEphemeral(c));
            }
        };
    private SoftRefMemoizableAction<Disk> diskMemo
        = new SoftRefMemoizableAction<Disk>(MAX_AGE_DISK) {
            @Override
            protected CompletableFuture<Disk> prepareAction() {
                return idMemo.getResult()
                        .thenApply(compute::getDiskByName)
                        .thenApply(o -> o.orElse(null));
            }
        };
    private SoftRefMemoizableAction<ComputeSku> skuMemo
        = new SoftRefMemoizableAction<ComputeSku>(MAX_AGE_SKU) {
            @Override
            protected CompletableFuture<ComputeSku> prepareAction() {
                return modelMemo.getResult()
                        .thenApply(compute::getDiskSkuLocal)
                        .thenApply(o -> o.orElse(null));
            }
        };
    private SoftRefMemoizableAction<String> tierMemo
        = new SoftRefMemoizableAction<String>(MAX_AGE_TIER) {
            @Override
            protected CompletableFuture<String> prepareAction() {
                return diskMemo.getResult()
                        .thenApply(Disk::tags)
                        .thenApply(t -> t == null
                            ? null
                            : t.get(AzureConstants.TAG_TIER));
            }
        };

    public AzureDiskBase(
            final MemoizedIMDSInstance imdsInstance,
            final AzureCompute computeService) {
        instance = imdsInstance;
        compute = computeService;
    }

    protected abstract String getDiskName(Compute c);
    protected abstract String getDiskSku(Compute c);
    protected abstract String getDiskSize(Compute c);
    protected abstract boolean isDiskEphemeral(Compute c);

    @Override
    public Optional<String> getTier() {
        return Optional.ofNullable(tierMemo.uncheckedGet());
    }

    @Override
    public boolean isEphemeral() {
        return ephemeralMemo.uncheckedGet();
    }

    @Override
    public long getSizeGB() {
        return sizeMemo.uncheckedGet();
    }

    @Override
    public Optional<String> getModel() {
        return Optional.ofNullable(
                modelMemo.uncheckedGet());
    }

    @Override
    public String getId() {
        return idMemo.uncheckedGet();
    }

    protected Optional<Disk> getDisk() {
        return Optional.ofNullable(diskMemo.uncheckedGet());
    }

    protected Optional<ComputeSku> getSku() {
        return Optional.ofNullable(skuMemo.uncheckedGet());
    }
}
