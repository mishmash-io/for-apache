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
import java.util.Optional;

/**
 * Represents a geographic unit - like a continent or similar, that contains
 * {@link PlacementRegion}s.
 *
 * A geography may also be defined by a specific jurisdiction where certain
 * regulations apply - like the EU and its data protection laws.
 */
public interface PlacementGeography extends Comparator<PlacementGeography> {

    /**
     * Get the id of this geography.
     *
     * @return a unique id
     */
    public String getId();

    /**
     * Get an optional jurisdiction.
     *
     * @return a {@link PlacementJursidiction} or an empty optional if unknown.
     */
    public Optional<PlacementJurisdiction> getJurisdiction();

    /**
     * Compare the two geographies for order. Returns a negative int, zero,
     * or positive integer as the first geography is the nearer to this
     * geography, equidistant or the remoter of the two arguments.
     *
     * Use this method as a {@link Comparator} when sorting
     * {@link PlacementGeography}s based on how far they are from this
     * geography.
     *
     * {@inheritDoc}
     */
    @Override
    default int compare(PlacementGeography o1, PlacementGeography o2) {
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
