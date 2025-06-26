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

import java.net.InetSocketAddress;
import java.util.Collection;

import org.apache.zookeeper.client.ConnectStringParser;
import org.apache.zookeeper.client.HostProvider;
import org.apache.zookeeper.client.StaticHostProvider;

public class DefaultOsgiHostProvider implements HostProvider {

    private StaticHostProvider defaultProvider;
    private String connectString;

    public DefaultOsgiHostProvider(final String connectStr) {
        defaultProvider = new StaticHostProvider(
                new ConnectStringParser(connectStr)
                    .getServerAddresses());
        connectString = connectStr;
    }

    @Override
    public int size() {
        return getOrDefault().size();
    }

    @Override
    public InetSocketAddress next(final long spinDelay) {
        return getOrDefault().next(spinDelay);
    }

    @Override
    public void onConnected() {
        getOrDefault().onConnected();
    }

    @Override
    public boolean updateServerList(
            final Collection<InetSocketAddress> serverAddresses,
            final InetSocketAddress currentHost) {
        HostProvider current = getOrDefault();
        boolean dRes = defaultProvider
                .updateServerList(serverAddresses, currentHost);
        if (current == defaultProvider) {
            return dRes;
        }

        return current.updateServerList(serverAddresses, currentHost);
    }

    protected HostProvider getOrDefault() {
        HostProvider p = ClientConfigsTracker
                .hostProviderForQuorumConnectStr(connectString);
        if (p == null) {
            return defaultProvider;
        }

        return p;
    }
}
