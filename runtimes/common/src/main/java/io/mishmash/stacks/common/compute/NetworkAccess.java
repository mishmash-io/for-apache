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

package io.mishmash.stacks.common.compute;

import java.net.InetAddress;
import java.util.Optional;

/**
 * Represents a network access mapping for a single IP
 * Address and possibly hosts on the same network or,
 * in case of a NAT rule for example - an external IP
 * address.
 */
public interface NetworkAccess {

    /**
     * Get the local IP address.
     *
     * @return An IP address that is assigned to the local node.
     */
    public InetAddress getAddress();

    /**
     * The network (or prefix) length in bits. Used to determine
     * net masks and such.
     *
     * If not known - will return the length of an individual address,
     * for example - {@code 32} for IPv4 addresses and {@code 128} for IPv6.
     *
     * @return the network or prefix length, in bits
     */
    public short getPrefixLength();

    /**
     * If a physical address (like a MAC address) is known the
     * returned {@link Optional} will contain its string representation.
     *
     * @return an {@link Optional} containing the physical-layer address
     * or an empty {@link Optional} if unknown.
     */
    public Optional<String> getPhysicalAddress();

    /**
     * If the local address is additionally mapped to an external
     * address that external address will be returned by this method.
     *
     * Useful in case of network address translation (NAT) rules where
     * this node might be seen by others under a different address.
     * 
     * @return An external address or an empty {@link Optional} if
     * not available.
     */
    public Optional<InetAddress> getExternalAddress();

}
