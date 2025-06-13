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

import java.util.Optional;

/**
 * A placement of a host within a {@link CloudProvider}.
 *
 * Placement instances are used to calculate the topology of a
 * running cluster which in turn allows to better distribute data
 * and tasks.
 *
 * Placements provide information like how far away are two hosts
 * from each other, if they may be affected by a single outage event,
 * or if they are under different regulations.
 *
 * The details contained in a {@link Placement} can be used to select
 * hosts with low network delays for optimal performance, or hosts that
 * are highly unlikely to fail at the same time (for highest availability).
 *
 * {@link CloudProvider}s may differ in how they organize their networks
 * and data centers, but typically physical proximity will be represented
 * by a {@link PlacementRegion} -> {@link PlacementZone} ->
 * {@link PlacementGroup} hierarchy.
 *
 * Optionally, providers can also complement these with
 * {@link PlacementDomain}s for better resolution. Placement domains
 * (such as update or fault domains) may or may not affect physical proximity
 * between hosts - each provider is expected to transparently handle that in
 * the correct way.
 */
public interface Placement {

    /**
     * Optionally, get the geography of the placement.
     *
     * @return a {@link PlacementGeography} or empty if unavailable
     */
    public Optional<PlacementGeography> getGeography();

    /**
     * Get the region of this placement.
     *
     * @return a {@link PlacementRegion} or empty if unavailable
     */
    public Optional<PlacementRegion> getRegion();

    /**
     * Get the zone within the {@link PlacementRegion}.
     *
     * @return a {@link PlacementZone} or empty if unavailable
     */
    public Optional<PlacementZone> getZone();

    /**
     * Get the placement group within the {@link PlacementZone}.
     *
     * Nodes within the same placement group are guaranteed
     * (by the provider) to have the lowest possible network
     * latency (by being physically closer to each other).
     *
     * @return a {@link PlacementGroup} or empty if unavailable
     */
    public Optional<PlacementGroup> getGroup();

    /**
     * Optionally, get a fault domain for this placement.
     *
     * A fault domain is a group of VMs that share common power
     * and/or network switch, etc. In other words they may fail
     * at the same time because of a single outage.
     *
     * @return a {@link PlacementDomain} or empty if unavailable
     */
    public Optional<PlacementDomain> getFaultDomain();

    /**
     * Optionally, get an update domain for this placement.
     *
     * An update domain is a group of VMs that can be restarted at the
     * same time. For example, during maintenance only one update domain
     * is restarted at a time.
     *
     * @return a {@link PlacementDomain} or empty if unavailable
     */
    public Optional<PlacementDomain> getUpdateDomain();

    /**
     * Get the cloud provider for this placement.
     *
     * @return a {@link CloudProvider} instance
     */
    public CloudProvider getProvider();

}
