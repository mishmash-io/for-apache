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
 * A region where hosts can be deployed. A region typically contains
 * multiple {@link PlacementZone}s, that is - multiple data centers,
 * for example.
 */
public interface PlacementRegion extends Comparator<PlacementRegion> {

    /**
     * Get the id of this region.
     *
     * @return a unique id
     */
    public String getId();

    /**
     * Compare the two regions for order. Returns a negative int, zero,
     * or positive integer as the first region is the nearer to this region,
     * equidistant or the remoter of the two arguments.
     *
     * Use this method as a {@link Comparator} when sorting
     * {@link PlacementRegion}s based on how far they are from this region.
     *
     * {@inheritDoc}
     */
    @Override
    default int compare(PlacementRegion o1, PlacementRegion o2) {
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
