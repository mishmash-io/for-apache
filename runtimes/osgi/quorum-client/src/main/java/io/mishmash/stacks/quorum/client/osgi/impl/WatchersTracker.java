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

package io.mishmash.stacks.quorum.client.osgi.impl;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.zookeeper.Watcher;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

@Component
public class WatchersTracker {

    @Reference(cardinality=ReferenceCardinality.MULTIPLE)
    private Collection<ServiceReference<Watcher>> watchers = new ConcurrentLinkedQueue<>();

    public Watcher selectWatcher(final String quorumId) {
        return null;
    }
}
