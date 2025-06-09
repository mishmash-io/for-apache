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

package io.mishmash.stacks.common.cloud;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Represents a single storage unit - a disk, or a bucket that
 * is attached to a host.
 *
 * Storage can be tiered, where, for example, fast disks are used for
 * quick data access applications and slower disks deep storage for
 * archives. It can also be ephemeral and therefore lost on reboot.
 */
public interface Storage {
    /**
     * Get the storage tier, if available.
     *
     * @return the tier name or empty if unavailable
     */
    public Optional<String> getTier();

    /**
     * Check if this storage is ephemeral.
     *
     * @return true if ephemeral and will be lost on reboot
     */
    public boolean isEphemeral();

    /**
     * Get a size indication.
     *
     * @return the size in GB.
     */
    public long getSizeGB();

    /**
     * Get an optional, human readable model of this storage.
     *
     * Can be a SKU of a cloud disk offer or similar information.
     *
     * @return a model string or empty if unavailable
     */
    public Optional<String> getModel();

    /**
     * Get a filesystem path that can be used to access this storage.
     *
     * @return the {@link Path} to use when accessing this storage
     */
    public Path getPath();
}
