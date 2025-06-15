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
 * Represents a node placement group. Nodes within the same group are
 * guaranteed by the provider to be as close to each other as possible,
 * or in other words having the lowest possible network latency, etc.
 *
 * Depending on the cloud provider, this might be a single rack or a
 * server room.
 */
public interface PlacementGroup extends Comparator<PlacementGroup> {

    /**
     * Get the id of this group.
     *
     * @return a unique id
     */
    public String getId();

    /**
     * Compare the two placement groups for order. Returns a negative int,
     * zero, or positive integer as the first is the nearer to
     * this placement group, equidistant or the remoter of the two arguments.
     *
     * Use this method as a {@link Comparator} when sorting
     * {@link PlacementGroup}s based on how far they are from this group.
     *
     * {@inheritDoc}
     */
    @Override
    default int compare(PlacementGroup o1, PlacementGroup o2) {
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
