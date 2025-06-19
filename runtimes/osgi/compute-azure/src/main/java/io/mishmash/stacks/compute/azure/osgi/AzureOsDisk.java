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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.mishmash.gen.openapi.azure.imds.client.model.Compute;
import io.mishmash.gen.openapi.azure.imds.client.model.DiffDiskSettings;
import io.mishmash.gen.openapi.azure.imds.client.model.ManagedDisk;
import io.mishmash.gen.openapi.azure.imds.client.model.OsDisk;
import io.mishmash.gen.openapi.azure.imds.client.model.StorageProfile;
import io.mishmash.stacks.compute.common.Storage;

@Component(service={Storage.class}, immediate=true)
public class AzureOsDisk extends AzureDiskBase {

    @Activate
    public AzureOsDisk(
            @Reference final MemoizedIMDSInstance imds,
            @Reference final AzureCompute compute) {
        super(imds, compute);
    }

    @Override
    protected String getDiskName(final Compute c) {
        return Optional.ofNullable(c)
                .map(Compute::getStorageProfile)
                .map(StorageProfile::getOsDisk)
                .map(OsDisk::getName)
                .orElse(null);
    }

    @Override
    protected String getDiskSku(final Compute c) {
        return Optional.ofNullable(c)
                .map(Compute::getStorageProfile)
                .map(StorageProfile::getOsDisk)
                .map(OsDisk::getManagedDisk)
                .map(ManagedDisk::getStorageAccountType)
                .orElse(null);
    }

    @Override
    protected String getDiskSize(final Compute c) {
        return Optional.ofNullable(c)
                .map(Compute::getStorageProfile)
                .map(StorageProfile::getOsDisk)
                .map(OsDisk::getDiskSizeGB)
                .orElse(null);
    }

    @Override
    protected boolean isDiskEphemeral(Compute c) {
        return Optional.ofNullable(c)
                .map(Compute::getStorageProfile)
                .map(StorageProfile::getOsDisk)
                .map(OsDisk::getDiffDiskSettings)
                .map(DiffDiskSettings::getOption)
                .map(o -> "Local".equals(o))
                .orElse(false);
    }

    @Override
    public Optional<URI> getURI() {
        return Optional.of(URI.create("file:///"));
    }
}
