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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.zookeeper.client.ZKClientConfig;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

@Component(
        service = {ZKClientConfig.class},
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        configurationPid = {"quorumClient"}
)
public class ClientConfigFactory extends ZKClientConfig {

    public static final String PROP_QUORUM_ID = "quorum.id";

    private static final Logger LOG = Logger.getLogger(ClientConfigFactory.class.getName());

    @Activate
    private void start(final BundleContext ctx, final Map<String, Object> props) {
        LOG.info("Publishing new client configuration for quorum '"
            + props.getOrDefault(PROP_QUORUM_ID, "<unspecified>")
            + "'");

        setAll(props);
    }

    @Deactivate
    private void stop() {
        LOG.info("Dropping client configuration for quorum '"
            + getProperty(PROP_QUORUM_ID, "<unspecified>")
            + "'");
    }

    @Modified
    private void modify(final Map<String, Object> props) {
        LOG.info("Modifying client configuration for quorum '"
            + getProperty(PROP_QUORUM_ID, "<unspecified>")
            + "'");

        init();
        setAll(props);
        if (props != null) {
            for (String prop : props.keySet()) {
                LOG.info("Got new zk config prop: " + prop);
            }
        }
    }

    private void setAll(final Map<String, Object> props) {
        for (Map.Entry<String, Object> ent : props.entrySet()) {
            String k = ent.getKey();
            Object v = ent.getValue();

            if (v != null) {
                setProperty(k, v.toString());
            }
        }
    }
}
