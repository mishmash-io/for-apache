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

import java.util.Comparator;

/**
 * A {@link PlacementZone} is an individual location within a single
 * {@link PlacementRegion} where hosts can be deployed by a provider.
 *
 * Zones may contain one or more data centers, but will be typically
 * separated from other zones by a significant distance to avoid
 * disasters affecting both at the same time.
 */
public interface PlacementZone extends Comparator<PlacementZone> {

    /**
     * Get the id of this zone.
     *
     * @return a unique id
     */
    public String getId();

    /**
     * Compare the two zones for order. Returns a negative int, zero,
     * or positive integer as the first zone is the nearer to this zone,
     * equidistant or the remoter of the two arguments.
     *
     * Use this method as a {@link Comparator} when sorting
     * {@link PlacementZone}s based on how far they are from this zone.
     *
     * {@inheritDoc}
     */
    @Override
    default int compare(PlacementZone o1, PlacementZone o2) {
        if (this.equals(o1)) {
            if (this.equals(o2)) {
                return 0;
            } else {
                return -1;
            }
        } else if (this.equals(o2)) {
            return 1;
        } else {
            return 0;
        }
    }

}
