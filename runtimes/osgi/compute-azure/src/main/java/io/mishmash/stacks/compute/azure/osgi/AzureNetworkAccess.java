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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.Instance;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.Ipv4Properties;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.Ipv6Properties;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.LoadBalancerAddressMapping;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.LoadBalancerLoadbalancer;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.Network;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.NetworkInterface;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.NetworkInterfaceIpv4;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.NetworkInterfaceIpv6;
import io.mishmash.stacks.compute.azure.gen.openapi.imds.client.model.SubnetProperties;
import io.mishmash.stacks.common.SoftRefMemoizableAction;
import io.mishmash.stacks.compute.common.NetworkAccess;

public class AzureNetworkAccess implements NetworkAccess {

    private static final Logger LOG = Logger.getLogger(
            AzureNetworkAccess.class.getName());

    /*
     * Refresh our params once in a while, as the network
     * settings might be changed by the user.
     */
    private static final Duration MAX_AGE_PREFIX =
            Duration.of(10, ChronoUnit.SECONDS);
    private static final Duration MAX_AGE_MAC =
            Duration.of(10, ChronoUnit.SECONDS);
    private static final Duration MAX_AGE_IMDS_EXTERNAL =
            Duration.of(10, ChronoUnit.SECONDS);
    private static final Duration MAX_AGE_LB_EXTERNAL =
            Duration.of(10, ChronoUnit.SECONDS);

    private MemoizedIMDSInstance instance;
    private MemoizedIMDSLoadBalancer lb;

    private String localAddr;

    private SoftRefMemoizableAction<Short> prefixMemo
        = new SoftRefMemoizableAction<Short>(MAX_AGE_PREFIX) {
            @Override
            protected CompletableFuture<Short> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getNetwork)
                        .thenApply(Network::getInterface)
                        .thenApply(AzureNetworkAccess.this::getPrefix);
            }
        };
    private SoftRefMemoizableAction<byte[]> macMemo
        = new SoftRefMemoizableAction<byte[]>(MAX_AGE_MAC) {
            @Override
            protected CompletableFuture<byte[]> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getNetwork)
                        .thenApply(Network::getInterface)
                        .thenApply(AzureNetworkAccess.this::getMAC)
                        .thenApply(m -> HexFormat.of().parseHex(m));
            }
        };
    private SoftRefMemoizableAction<InetAddress> imdsExternalAddrMemo
        = new SoftRefMemoizableAction<InetAddress>(MAX_AGE_IMDS_EXTERNAL) {
            @Override
            protected CompletableFuture<InetAddress> prepareAction() {
                return instance.getResult()
                        .thenApply(Instance::getNetwork)
                        .thenApply(Network::getInterface)
                        .thenApply(AzureNetworkAccess.this::getExternal);
            }
        };
    private SoftRefMemoizableAction<InetAddress> lbExternalAddrMemo
        = new SoftRefMemoizableAction<InetAddress>(MAX_AGE_LB_EXTERNAL) {
            @Override
            protected CompletableFuture<InetAddress> prepareAction() {
                return lb.getResult()
                        .thenApply(
                                LoadBalancerLoadbalancer::getPublicIpAddresses)
                        .thenApply(AzureNetworkAccess.this::getLBExternal);
            }
        };

    public AzureNetworkAccess(
            final String addr,
            final MemoizedIMDSInstance metadataService,
            final MemoizedIMDSLoadBalancer loadBalancerService) {
        this.localAddr = addr;
        this.instance = metadataService;
        this.lb = loadBalancerService;
    }

    protected short getPrefix(final List<NetworkInterface> ifaces) {
        if (findIpv6(ifaces) != null) {
            return 128;
        } else {
            Triple<String, Ipv4Properties, SubnetProperties> t =
                    findIpv4(ifaces);
            if (t != null) {
                return Short.valueOf(t.getRight().getPrefix());
            }

            return 32;
        }
    }

    protected InetAddress getLBExternal(
            final List<LoadBalancerAddressMapping> maps) {
        for (LoadBalancerAddressMapping m : maps) {
            if (localAddr.equals(m.getPrivateIpAddress())) {
                String publicAddr = m.getFrontendIpAddress();

                if (publicAddr != null) {
                    try {
                        return InetAddress.getByName(publicAddr);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING,
                                """
                                Failed to resolve Azure Loadbalancer \
                                frontend IP Address """
                                    + publicAddr
                                    + ". Address will be ignored.",
                                e);
                    }
                }

                break;
            }
        }

        return null;
    }

    protected InetAddress getExternal(final List<NetworkInterface> ifaces) {
        if (findIpv6(ifaces) != null) {
            return null;
        } else {
            Triple<String, Ipv4Properties, SubnetProperties> t =
                    findIpv4(ifaces);
            if (t != null) {
                String publicAddr = t.getMiddle().getPublicIpAddress();

                if (publicAddr != null && !publicAddr.isBlank()) {
                    try {
                        return InetAddress.getByName(publicAddr);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING,
                                "Failed to resolve Azure public IP Address "
                                    + publicAddr
                                    + ". Address will be ignored.",
                                e);
                    }
                }
            }

            return null;
        }
    }

    protected String getMAC(final List<NetworkInterface> ifaces) {
        Pair<String, Ipv6Properties> ipv6 = findIpv6(ifaces);

        if (ipv6 != null) {
            return ipv6.getLeft();
        } else {
            Triple<String, Ipv4Properties, SubnetProperties> t =
                    findIpv4(ifaces);
            if (t != null) {
                return t.getLeft();
            }

            return null;
        }
    }

    protected Triple<String, Ipv4Properties, SubnetProperties>
            findIpv4(final List<NetworkInterface> ifaces) {
        for (NetworkInterface iface : Optional
                                        .ofNullable(ifaces)
                                        .orElse(List.of())) {
            List<Ipv4Properties> list = Optional.ofNullable(iface.getIpv4())
                    .map(NetworkInterfaceIpv4::getIpAddress)
                    .orElse(List.of());

            for (int i = 0; i < list.size(); i++) {
                Ipv4Properties ipv4 = list.get(i);

                if (localAddr.equals(ipv4.getPrivateIpAddress())) {
                    return Triple.of(
                            iface.getMacAddress(),
                            ipv4,
                            iface.getIpv4().getSubnet().get(i));
                }
            }
        }

        return null;
    }

    protected Pair<String, Ipv6Properties>
            findIpv6(final List<NetworkInterface> ifaces) {
        for (NetworkInterface iface : Optional
                                        .ofNullable(ifaces)
                                        .orElse(List.of())) {
            for (Ipv6Properties ipv6 : Optional.ofNullable(iface.getIpv6())
                                        .map(NetworkInterfaceIpv6
                                                ::getIpAddress)
                                        .orElse(List.of())) {
                if (localAddr.equals(ipv6.getPrivateIpAddress())) {
                    return Pair.of(iface.getMacAddress(), ipv6);
                }
            }
        }

        return null;
    }

    @Override
    public InetAddress getAddress() {
        try {
            return InetAddress.getByName(localAddr);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public short getPrefixLength() {
        return prefixMemo.uncheckedGet();
    }

    @Override
    public Optional<byte[]> getHardwareAddress() {
        return Optional.ofNullable(macMemo.uncheckedGet());
    }

    @Override
    public Optional<InetAddress> getExternalAddress() {
        return Optional
                .ofNullable(imdsExternalAddrMemo.uncheckedGet())
                .or(() -> Optional
                        .ofNullable(lbExternalAddrMemo.uncheckedGet()));
    }
}
