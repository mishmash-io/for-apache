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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.zookeeper.ZooKeeper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

@Component(
        service = {ClientFactory.class},
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        configurationPid = {"quorumClient"}
)
public class ClientFactory {

    private ServiceRegistration<ZooKeeper> serviceReg;

    @Activate
    protected void activate(
            final BundleContext ctx,
            final Map<String, Object> props) {
        String connectStr = (String) props
                .get(ClientConfigFactory.PROP_QUORUM_CONNECT);
        int sessionTimeout = 2000;
        if (props.containsKey(ClientConfigFactory.PROP_QUORUM_TIMEOUT)) {
            try {
                sessionTimeout = Integer.valueOf((String) props
                        .get(ClientConfigFactory.PROP_QUORUM_TIMEOUT));
            } catch (NumberFormatException e) {
                throw new RuntimeException("""
                                Session timeout configuration property \
                                for quorum client """
                                + connectStr
                                + " must be integer");
            }
        }

        try {
            ZooKeeper zk = new ZooKeeper(connectStr, sessionTimeout, null);
            Dictionary<String, ?> p = new Hashtable<>(props);
            serviceReg = ctx.registerService(ZooKeeper.class, zk, p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Modified
    protected void modified(
            final BundleContext ctx,
            final Map<String, Object> props) {

    }

    @Deactivate
    protected void deactivate(
            final BundleContext ctx,
            final Map<String, Object> props) {
        if (serviceReg != null) {
            ZooKeeper zk = serviceReg.getReference().adapt(ZooKeeper.class);
            serviceReg.unregister();
            serviceReg = null;

            try {
                zk.close();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}
