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

package io.mishmash.stacks.simplecompute.osgi;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

import io.mishmash.stacks.common.compute.NetworkAccess;

@Component(
        service={NetworkAccess.class},
        immediate=true,
        configurationPolicy=ConfigurationPolicy.REQUIRE,
        configurationPid={ConfigNetworkAccess.CONFIG_NETWORK_PID})
public class ConfigNetworkAccess implements NetworkAccess {

    private static final Logger LOG = Logger.getLogger(
            ConfigNetworkAccess.class.getName());

    public static final String CONFIG_NETWORK_PID = "computeNetwork";

    public static final String CONFIG_NETWORK_PROP_INTERNAL_ADDR =
            "internalAddress";
    public static final String CONFIG_NETWORK_PROP_PREFIX_LEN =
            "prefixLength";
    public static final String CONFIG_NETWORK_PROP_HW_ADDR =
            "hardwareAddress";
    public static final String CONFIG_NETWORK_PROP_EXTERNAL_ADDR =
            "externalAddress";

    private String servicePid;
    private String internalAddr;
    private String prefixLen;
    private String hwAddr;
    private String externalAddr;

    @Override
    public InetAddress getAddress() {
        if (internalAddr == null) {
            String reason = CONFIG_NETWORK_PROP_INTERNAL_ADDR
                    + " is required, but not present in configuration "
                    + servicePid;

            LOG.log(Level.SEVERE, reason);

            throw new RuntimeException(reason);
        }

        try {
            return InetAddress.getByName(internalAddr);
        } catch (UnknownHostException e) {
            String reason = "Failed to parse "
                    + CONFIG_NETWORK_PROP_INTERNAL_ADDR
                    + " " + internalAddr
                    + " in configuration "
                    + servicePid
                    + ": "
                    + e.getMessage();
            LOG.log(Level.SEVERE, reason, e);

            throw new RuntimeException(e);
        }
    }

    @Override
    public short getPrefixLength() {
        if (prefixLen == null || prefixLen.isBlank()) {
            try {
                InetAddress localAddr = getAddress();

                // try to get it from the os:
                return NetworkInterface.getByInetAddress(localAddr)
                    .getInterfaceAddresses()
                        .stream()
                        .filter(ia -> ia.getAddress().equals(localAddr))
                        .map(ia -> ia.getNetworkPrefixLength())
                        .findAny()
                        // if not found - compute a default
                        .orElse((short)
                                (localAddr.getAddress().length * Byte.SIZE));
            } catch (SocketException e) {
                String reason = "Could not determine "
                        + CONFIG_NETWORK_PROP_PREFIX_LEN
                        + " in configuration "
                        + servicePid
                        + " from the OS: "
                        + e.getMessage();
                LOG.log(Level.SEVERE, reason, e);

                throw new RuntimeException(e);
            }
        }

        try {
            return Short.valueOf(prefixLen);
        } catch (NumberFormatException e) {
            String reason = "Failed to parse "
                    + CONFIG_NETWORK_PROP_PREFIX_LEN
                    + " in configuration "
                    + servicePid
                    + ": "
                    + e.getMessage();
            LOG.log(Level.SEVERE, reason, e);

            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<byte[]> getHardwareAddress() {
        if (hwAddr == null || hwAddr.isBlank()) {
            // try to get through the OS
            try {
                return Optional.of(
                        NetworkInterface
                            .getByInetAddress(getAddress())
                            .getHardwareAddress());
            } catch (SocketException e) {
                LOG.log(Level.WARNING,
                        "Failed to determine "
                        + CONFIG_NETWORK_PROP_HW_ADDR
                        + " in configuration "
                        + servicePid
                        + " from the OS: "
                        + e.getMessage()
                        + ". Treating as missing.");
                return Optional.empty();
            }
        }

        try {
            byte[] parsed = HexFormat.ofDelimiter(":").parseHex(hwAddr);
            return Optional.of(parsed);
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                    "Failed to parse "
                    + CONFIG_NETWORK_PROP_HW_ADDR
                    + " "
                    + hwAddr
                    + " in configuration "
                    + servicePid
                    + ": "
                    + e.getMessage()
                    + ". Treating as missing.");
            return Optional.empty();
        }
    }

    @Override
    public Optional<InetAddress> getExternalAddress() {
        return Optional.ofNullable(externalAddr)
                .map(a -> {
                    try {
                        return InetAddress.getByName(a);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING,
                                "Failed to parse "
                                + CONFIG_NETWORK_PROP_EXTERNAL_ADDR
                                + " in configuration "
                                + servicePid
                                + ": "
                                + e.getMessage()
                                + ". Treating as missing.");

                        return null;
                    }
                });
    }

    @Activate
    private void configure(final Map<String, Object> conf) {
        servicePid = (String) conf.get("service.pid");
        configureInternalAddr(conf);
        configurePrefixLen(conf);
        configureHwAddr(conf);
        configureExternalAddr(conf);
    }

    @Modified
    private void modify(final Map<String, Object> conf) {
        configureInternalAddr(conf);
        configurePrefixLen(conf);
        configureHwAddr(conf);
        configureExternalAddr(conf);
    }

    protected void configureInternalAddr(final Map<String, Object> conf) {
        internalAddr = (String) conf.get(CONFIG_NETWORK_PROP_INTERNAL_ADDR);
    }

    protected void configurePrefixLen(final Map<String, Object> conf) {
        prefixLen = (String) conf.get(CONFIG_NETWORK_PROP_PREFIX_LEN);
    }

    protected void configureHwAddr(final Map<String, Object> conf) {
        hwAddr = (String) conf.get(CONFIG_NETWORK_PROP_HW_ADDR);
    }

    protected void configureExternalAddr(final Map<String, Object> conf) {
        externalAddr = (String) conf.get(CONFIG_NETWORK_PROP_EXTERNAL_ADDR);
    }
}
