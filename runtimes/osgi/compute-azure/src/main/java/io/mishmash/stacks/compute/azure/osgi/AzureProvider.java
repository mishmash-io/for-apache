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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import io.mishmash.gen.openapi.azure.imds.client.model.Compute;
import io.mishmash.gen.openapi.azure.imds.client.model.DataDisk;
import io.mishmash.gen.openapi.azure.imds.client.model.Instance;
import io.mishmash.gen.openapi.azure.imds.client.model.Ipv4Properties;
import io.mishmash.gen.openapi.azure.imds.client.model.Ipv6Properties;
import io.mishmash.gen.openapi.azure.imds.client.model.Network;
import io.mishmash.gen.openapi.azure.imds.client.model.NetworkInterface;
import io.mishmash.gen.openapi.azure.imds.client.model.StorageProfile;
import io.mishmash.stacks.common.SoftRefMemoizableAction;
import io.mishmash.stacks.compute.common.ComputeProvider;
import io.mishmash.stacks.compute.common.NetworkAccess;
import io.mishmash.stacks.compute.common.Storage;

@Component(service={ComputeProvider.class, AzureProvider.class}, immediate=true)
public class AzureProvider implements ComputeProvider {

    private static final Logger LOG = Logger.getLogger(
            AzureProvider.class.getName());

    public static final String AZURE_PUBLIC_CLOUD = "AzurePublicCloud";
    public static final String AZURE_CHINA_CLOUD = "AzureChinaCloud";
    public static final String AZURE_US_GOV_CLOUD = "AzureUSGovernmentCloud";

    /*
     * Don't refresh, it won't change without a restart.
     */
    private static final Duration MAX_AGE_AZ_ENV = null;
    /*
     * Refresh disks and networks once in a while, as these
     * might change.
     */
    private static final Duration MAX_AGE_RES =
            Duration.of(10, ChronoUnit.SECONDS);

    private MemoizedIMDSInstance instance;
    private MemoizedIMDSLoadBalancer lb;
    private AzureCompute compute;
    private BundleContext bundleCtx;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledPoll;
    private ConcurrentHashMap<String, ServiceRegistration<Storage>>
            storageRegistrations = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ServiceRegistration<NetworkAccess>>
            networkAccessRegistrations = new ConcurrentHashMap<>();

    private SoftRefMemoizableAction<String> azureEnvMemo
        = new SoftRefMemoizableAction<String>(MAX_AGE_AZ_ENV) {
            @Override
            protected CompletableFuture<String> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getCompute)
                        .thenApply(Compute::getAzEnvironment);
            }
        };
    private SoftRefMemoizableAction<
            Pair<List<DataDisk>, List<NetworkInterface>>> disksAndNetworksMemo
        = new SoftRefMemoizableAction<
                Pair<List<DataDisk>, List<NetworkInterface>>>(MAX_AGE_RES) {
            @Override
            protected CompletableFuture<
                    Pair<List<DataDisk>, List<NetworkInterface>>>
                        prepareAction() {
                return instance.getResult()
                        .thenApply(i -> Pair.of(
                                Optional.ofNullable(i)
                                    .map(Instance::getCompute)
                                    .map(Compute::getStorageProfile)
                                    .map(StorageProfile::getDataDisks)
                                    .orElse(List.of()),
                                Optional.ofNullable(i)
                                    .map(Instance::getNetwork)
                                    .map(Network::getInterface)
                                    .orElse(List.of())));
            }
        };

    @Activate
    public AzureProvider(
            final BundleContext ctx,
            @Reference final MemoizedIMDSInstance imdsInstance,
            @Reference final MemoizedIMDSLoadBalancer loadBalancer,
            @Reference final AzureCompute azureCompute) {
        bundleCtx = ctx;
        instance = imdsInstance;
        lb = loadBalancer;
        compute = azureCompute;

        if (ctx == null) {
            // allow init for testing
            scheduler = null;
            scheduledPoll = null;
        } else {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduledPoll = scheduler.scheduleAtFixedRate(
                this::refreshFromIMDS, 10, 10, TimeUnit.SECONDS);
        }
    }

    @Deactivate
    protected void deactivate() {
        if (scheduler == null) {
            // do nothing, we were initialized by tests
        } else {
            // stop the scheduler
            scheduledPoll.cancel(true);
            scheduler.shutdownNow();
        }

        // deregister disks and networks...
        for (Map.Entry<String, ServiceRegistration<Storage>> diskReg
                : storageRegistrations.entrySet()) {
            deregisterDataDisk(diskReg.getKey(), diskReg.getValue());
        }

        for (Map.Entry<String, ServiceRegistration<NetworkAccess>> netReg
                : networkAccessRegistrations.entrySet()) {
            deregisterNetworkAccess(netReg.getKey(), netReg.getValue());
        }
    }

    @Override
    public String getName() {
        return azureEnvMemo.uncheckedGet();
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        if (o instanceof AzureProvider other) {
            return this.getName().equals(other.getName());
        } else {
            return false;
        }
    }

    protected void refreshFromIMDS() {
        try {
            Pair<List<DataDisk>, List<NetworkInterface>> resources
                    = disksAndNetworksMemo.get();
            Set<String> newLuns = resources.getLeft().stream()
                    .map(DataDisk::getLun)
                    .collect(Collectors.toSet());
            HashSet<String> newPrivateIPs = new HashSet<>();
            // add IPv4 private addresses
            newPrivateIPs.addAll(resources.getRight().stream()
                    .flatMap(iface -> iface.getIpv4().getIpAddress().stream())
                    .map(Ipv4Properties::getPrivateIpAddress)
                    .collect(Collectors.toSet()));
            // add IPv6 private addresses
            newPrivateIPs.addAll(resources.getRight().stream()
                    .flatMap(iface -> iface.getIpv6().getIpAddress().stream())
                    .map(Ipv6Properties::getPrivateIpAddress)
                    .collect(Collectors.toSet()));

            // register new, or change existing data disks
            for (String newLun : newLuns) {
                storageRegistrations.compute(newLun,
                        (lun, reg) -> {
                            if (reg == null) {
                                // register new disk
                                return registerDataDisk(lun);
                            } else {
                                // check and if needed - update the reg
                                checkAndUpdateDataDisk(lun, reg);
                                return reg;
                            }
                        });
            }

            // deregister disks that were removed
            for (String registeredLun : storageRegistrations.keySet()) {
                if (!newLuns.contains(registeredLun)) {
                    ServiceRegistration<Storage> reg =
                            storageRegistrations.remove(registeredLun);
                    deregisterDataDisk(registeredLun, reg);
                }
            }

            // register new, or change existing network access details
            for (String newPrivateIP : newPrivateIPs) {
                networkAccessRegistrations.compute(
                        newPrivateIP,
                        (ip, reg) -> {
                            if (reg == null) {
                                // register new net access
                                return registerNetworkAccess(ip);
                            } else {
                                checkAndUpdateNetworkAccess(ip);
                                return reg;
                            }
                        });
            }

            // deregister networks that have been removed
            for (String registeredPrivateIP
                    : networkAccessRegistrations.keySet()) {
                if (!newPrivateIPs.contains(registeredPrivateIP)) {
                    ServiceRegistration<NetworkAccess> reg =
                            networkAccessRegistrations
                                .remove(registeredPrivateIP);
                    deregisterNetworkAccess(registeredPrivateIP, reg);
                }
            }
        } catch (InterruptedException e) {
            // we're being deactivated, just end
        } catch (ExecutionException e) {
            LOG.log(Level.SEVERE,
                    """
                    Failed to get Azure Disks and networks, \
                    functionality might be severely limited. \
                    Will retry.""",
                    e);
        }
    }

    protected ServiceRegistration<NetworkAccess> registerNetworkAccess(
            final String privateAddress) {
        try {
        AzureNetworkAccess net = new AzureNetworkAccess(
                privateAddress, instance, lb);
        ServiceRegistration<NetworkAccess> reg = bundleCtx
                .registerService(
                        NetworkAccess.class,
                        net,
                        // TODO: add params?
                        new Hashtable<>());
        LOG.log(Level.INFO,
                "Registered Azure Network service for private "
                        + " IP Address "
                        + privateAddress);

        return reg;
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "Failed to register Azure Network servcie for "
                            + " private IP address "
                            + privateAddress
                            + ", will retry.",
                    e);

            throw new RuntimeException(e);
        }
    }

    protected void checkAndUpdateNetworkAccess(
            final String privateAddress) {
        // Nothing to do here
    }

    protected void deregisterNetworkAccess(
            final String privateAddress,
            final ServiceRegistration<NetworkAccess> serviceReg) {
        try {
            serviceReg.unregister();
            LOG.log(Level.INFO,
                    "Deregistered Azure Network service for private IP "
                        + " address "
                        + privateAddress);
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "Failed to deregister Azure Network service for "
                        + " private IP address "
                        + privateAddress,
                    e);
        }
    }

    protected ServiceRegistration<Storage> registerDataDisk(
            final String lun) {
        try {
            AzureDataDisk d = new AzureDataDisk(lun, instance, compute);
            ServiceRegistration<Storage> reg = bundleCtx
                    .registerService(
                            Storage.class,
                            d,
                            // TODO: add params from DataDisk and SKU?
                            new Hashtable<>());
            LOG.log(Level.INFO,
                    "Registered Azure Data Disk service for LUN "
                            + lun);

            return reg;
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "Failed to register Azure Data Disk servcie for LUN "
                            + lun
                            + ", will retry.",
                    e);

            throw new RuntimeException(e);
        }
    }

    protected void checkAndUpdateDataDisk(
            final String lun,
            final ServiceRegistration<Storage> currentDisk) {
        // TODO: if parameters of the disk have changed, do a
        // currentDisk.setProperties(newProps)
    }

    protected void deregisterDataDisk(
            final String lun,
            final ServiceRegistration<Storage> serviceReg) {
        try {
            serviceReg.unregister();
            LOG.log(Level.INFO,
                    "Deregistered Azure Data Disk service for LUN "
                        + lun);
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "Failed to deregister Azure Data Disk service for LUN "
                        + lun,
                    e);
        }
    }
}
