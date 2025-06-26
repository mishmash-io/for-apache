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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class DefaultOsgiWatcher implements Watcher {

    private static final Logger LOG =
            Logger.getLogger(DefaultOsgiWatcher.class.getName());

    private Watcher defaultWatcher;
    private String connectString;

    public DefaultOsgiWatcher(
            final String connectStr,
            final Watcher defWatcher) {
        defaultWatcher = defWatcher;
        connectString = connectStr;
    }

    @Override
    public void process(final WatchedEvent event) {
        if (defaultWatcher != null) {
            // pass to the wrapped watcher
            process(defaultWatcher, event);
        }

        // pass to all other currently configured watchers
        ClientConfigsTracker.watchersForQuorumConnectStr(connectString)
            .forEach(w -> process(w, event));
    }

    private void process(final Watcher watcher, final WatchedEvent event) {
        try {
            watcher.process(event);
        } catch (Exception e) {
            LOG.log(Level.WARNING, 
                    "Watcher "
                    + watcher.getClass().getName()
                    + " failed to process event, ignoring error",
                    e);
        }
    }
}
