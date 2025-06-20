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

import java.util.Optional;

import org.apache.commons.lang3.arch.Processor;

/**
 * Represents the resources available on a given compute host.
 */
public interface ComputeHost {
    /**
     * Get an id of this host.
     *
     * @return the host id
     */
    public String uniqueId();

    /**
     * Get RAM size.
     *
     * @return memory size, in MB, or empty if unknown
     */
    public Optional<Integer> getMemoryMB();

    /**
     * Get the number of available CPU cores.
     *
     * @return number of CPU cores or empty if unknown
     */
    public Optional<Integer> getNumCPUCores();

    /**
     * Get the CPU Architecture and type.
     *
     * @return a {@link Processor} instance describing the CPU or empty
     * when unknown
     */
    public Optional<Processor> getCPUArchitecture();

    /**
     * Check if this host is virtual or physical.
     *
     * @return {@link Boolean.TRUE} if virtual, empty if unknown
     */
    public Optional<Boolean> isVirtual();

    /**
     * Get the tier of this host.
     *
     * @return the tier name or empty if not set
     */
    public Optional<String> getTier();

    /**
     * Get an optional, human readable model of this host.
     *
     * Can be a SKU of a cloud instance offer or similar information.
     *
     * @return a model string or empty if unavailable
     */
    public Optional<String> getModel();

    /**
     * Get the {@link ComputeProvider}.
     *
     * @return the cloud provider
     */
    public ComputeProvider getProvider();
}
