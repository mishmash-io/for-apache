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

package io.mishmash.stacks.compute.common;

import java.util.Comparator;

/**
 * Represents a placement domain where hosts share some common availability
 * characteristics.
 *
 * For example, nodes within the same {@code update domain}
 * will be upgraded/updated at the same time, while members of another update
 * domain will wait.
 *
 * Similarly, machines with the same fault domain typically share the same
 * network switch and/or power supply and will be all affected
 * by a single outage.
 */
public interface PlacementDomain extends Comparator<PlacementDomain> {

    /**
     * Get the id of this domain.
     *
     * @return a unique id
     */
    public String getId();

    /**
     * Compare the two domains for order. Returns a negative int,
     * zero, or positive integer as the first is the nearer to
     * this domain, equidistant or the remoter of the two arguments.
     *
     * Use this method as a {@link Comparator} when sorting
     * {@link PlacementDomain}s based on how far they are from this domain.
     *
     * {@inheritDoc}
     */
    @Override
    default int compare(PlacementDomain o1, PlacementDomain o2) {
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
