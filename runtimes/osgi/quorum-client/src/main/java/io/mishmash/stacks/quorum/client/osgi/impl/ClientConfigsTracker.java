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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.client.HostProvider;
import org.apache.zookeeper.client.ZKClientConfig;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

@Component(immediate=true)
public class ClientConfigsTracker {

    private static Collection<Map.Entry<ZKClientConfig, Map<String, Object>>>
            configs = new ConcurrentLinkedQueue<>();
    private static Collection<Map.Entry<Watcher, Map<String, Object>>>
            watchers = new ConcurrentLinkedQueue<>();
    private static Collection<Map.Entry<HostProvider, Map<String, Object>>>
            hostProviders = new ConcurrentLinkedQueue<>();

    @Reference(
            service=ZKClientConfig.class,
            cardinality=ReferenceCardinality.MULTIPLE)
    protected void addClientConfig(
            final ZKClientConfig config,
            final Map<String, Object> props) {
        configs.add(Map.entry(config, props));
    }

    protected void updatedClientConfig(
            final ZKClientConfig config,
            final Map<String, Object> props) {
        Map.Entry<ZKClientConfig, Map<String, Object>> ent =
                find(configs, config);
        if (ent != null) {
            configs.remove(ent);
            configs.add(Map.entry(config, props));
        }
    }

    protected void removeClientConfig(
            final ZKClientConfig config,
            final Map<String, Object> props) {
        Map.Entry<ZKClientConfig, Map<String, Object>> ent =
                find(configs, config);
        if (ent != null) {
            configs.remove(ent);
        }
    }

    @Reference(
            service=Watcher.class,
            cardinality=ReferenceCardinality.MULTIPLE)
    protected void addWatcher(
            final Watcher watcher,
            final Map<String, Object> props) {
        watchers.add(Map.entry(watcher, props));
    }

    protected void updatedWatcher(
            final Watcher watcher,
            final Map<String, Object> props) {
        Map.Entry<Watcher, Map<String, Object>> ent = find(watchers, watcher);
        if (ent != null) {
            watchers.remove(ent);
            watchers.add(Map.entry(watcher, props));
        }
    }

    protected void removeWatcher(
            final Watcher watcher,
            final Map<String, Object> props) {
        Map.Entry<Watcher, Map<String, Object>> ent = find(watchers, watcher);
        if (ent != null) {
            watchers.remove(ent);
        }
    }

    @Reference(
            service=HostProvider.class,
            cardinality=ReferenceCardinality.MULTIPLE)
    protected void addHostProvider(
            final HostProvider hostProvider,
            final Map<String, Object> props) {
        hostProviders.add(Map.entry(hostProvider, props));
    }

    protected void updatedHostProvider(
            final HostProvider hostProvider,
            final Map<String, Object> props) {
        Map.Entry<HostProvider, Map<String, Object>> ent =
                find(hostProviders, hostProvider);
        if (ent != null) {
            hostProviders.remove(ent);
            hostProviders.add(Map.entry(hostProvider, props));
        }
    }

    protected void removeHostProvider(
            final HostProvider hostProvider,
            final Map<String, Object> props) {
        Map.Entry<HostProvider, Map<String, Object>> ent =
                find(hostProviders, hostProvider);
        if (ent != null) {
            hostProviders.remove(ent);
        }
    }

    protected static <T> Map.Entry<T, Map<String, Object>> find(
            final Collection<Map.Entry<T, Map<String, Object>>> col,
            final T watcher) {
        return col.stream()
            .filter(e -> e.getKey().equals(watcher))
            .findAny()
            .orElse(null);
    }
    
    protected static <T> Map.Entry<T, Map<String, Object>>
            findForQuorum(
                    final Collection<Map.Entry<T, Map<String, Object>>> col,
                    final String optionName,
                    final String optionValue) {
        return col.stream()
                .filter(e -> optionValue.equals(
                        e.getValue().getOrDefault(
                                optionName,
                                null)))
                .findAny()
                .orElse(null);
    }

    protected static <T> Map.Entry<T, Map<String, Object>>
            findForQuorumId(
                    final Collection<Map.Entry<T, Map<String, Object>>> col,
                    final String quorumId) {
        Objects.requireNonNull(quorumId,
                "Supplied quorum id must be non-null");

        return findForQuorum(col,
                ClientConfigFactory.PROP_QUORUM_ID,
                quorumId);
    }

    protected static <T> Map.Entry<T, Map<String, Object>>
            findForQuorumConnect(
                    final Collection<Map.Entry<T, Map<String, Object>>> col,
                    final String quorumConnectStr) {
        Objects.requireNonNull(quorumConnectStr,
                "Supplied quorum connect string must be non-null");

        return findForQuorum(col,
                ClientConfigFactory.PROP_QUORUM_CONNECT,
                quorumConnectStr);
    }

    public static ZKClientConfig configForQuorumId(final String quorumId) {
        Map.Entry<ZKClientConfig, Map<String, Object>> ent =
                findForQuorumId(configs, quorumId);

        return ent == null ? null : ent.getKey();
    }

    public static ZKClientConfig configForQuorumConnectStr(
            final String quorumConnectStr) {
        Map.Entry<ZKClientConfig, Map<String, Object>> ent =
                findForQuorumConnect(configs, quorumConnectStr);

        return ent == null ? null : ent.getKey();
    }

    public static Watcher watcherForQuorumId(final String quorumId) {
        Map.Entry<Watcher, Map<String, Object>> ent =
                findForQuorumId(watchers, quorumId);

        return ent == null ? null : ent.getKey();
    }

    public static Watcher watcherForQuorumConnectStr(
            final String quorumConnectStr) {
        Map.Entry<Watcher, Map<String, Object>> ent =
                findForQuorumConnect(watchers, quorumConnectStr);

        return ent == null ? null : ent.getKey();
    }

    public static Collection<Watcher> watchersForQuorumConnectStr(
            final String quorumConnectStr) {
        Objects.requireNonNull(quorumConnectStr,
                "Supplied quorum connect string must be non-null");

        return watchers.stream()
            .filter(ent -> quorumConnectStr
                    .equals(
                            ent.getValue().getOrDefault(
                                ClientConfigFactory.PROP_QUORUM_CONNECT,
                                null)))
            .map(ent -> ent.getKey())
            .toList();
    }

    public static HostProvider hostProviderForQuorumId(final String quorumId) {
        Map.Entry<HostProvider, Map<String, Object>> ent =
                findForQuorumId(hostProviders, quorumId);

        return ent == null ? null : ent.getKey();
    }

    public static HostProvider hostProviderForQuorumConnectStr(
            final String quorumConnectStr) {
        Map.Entry<HostProvider, Map<String, Object>> ent =
                findForQuorumConnect(hostProviders, quorumConnectStr);

        return ent == null ? null : ent.getKey();
    }
}
