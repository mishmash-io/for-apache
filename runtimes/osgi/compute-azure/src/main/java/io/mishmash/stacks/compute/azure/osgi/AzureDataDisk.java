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

import java.net.URI;
import java.util.Optional;

import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.Compute;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.DataDisk;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.ManagedDisk;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.StorageProfile;

public class AzureDataDisk extends AzureDiskBase {

    private String lun;

    public AzureDataDisk(
            final String diskLUN,
            final MemoizedIMDSInstance imds,
            final AzureCompute compute) {
        super(imds, compute);

        this.lun = diskLUN;
    }

    protected Optional<DataDisk> getDisk(final Compute c) {
        return Optional.ofNullable(c)
                .map(Compute::getStorageProfile)
                .map(StorageProfile::getDataDisks)
                .flatMap(l -> l.stream()
                                .filter(d -> lun.equals(d.getLun()))
                                .findAny());
    }

    @Override
    protected String getDiskName(final Compute c) {
        return getDisk(c)
                .map(DataDisk::getName)
                .orElse(null);
    }

    @Override
    protected String getDiskSku(final Compute c) {
        return getDisk(c)
                .map(DataDisk::getManagedDisk)
                .map(ManagedDisk::getStorageAccountType)
                .orElse(null);
    }

    @Override
    protected String getDiskSize(final Compute c) {
        return getDisk(c)
                .map(DataDisk::getDiskSizeGB)
                .orElse(null);
    }

    @Override
    protected boolean isDiskEphemeral(Compute c) {
        return false;
    }

    @Override
    public boolean isEphemeral() {
        return false;
    }

    public String getLUN() {
        return lun;
    }

    @Override
    public Optional<URI> getURI() {
        // TODO Auto-generated method stub
        return null;
    }
}
