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

package io.mishmash.stacks.simplecloud.osgi;

import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

import io.mishmash.stacks.common.compute.CloudProvider;

@Component(
        service={CloudProvider.class, ConfigCloudProvider.class},
        immediate=true,
        configurationPolicy=ConfigurationPolicy.OPTIONAL,
        configurationPid={ConfigCloudProvider.CONFIG_PROVIDER_PID},
        property={
                ConfigCloudProvider.CONFIG_PROVIDER_PROP_NAME
                    + "="
                    + ConfigCloudProvider.STACKS_PROVIDER_CONFIG})
public class ConfigCloudProvider implements CloudProvider {

    public static final String CONFIG_PROVIDER_PID = "stacksProvider";

    public static final String CONFIG_PROVIDER_PROP_NAME = "name";

    public static final String STACKS_PROVIDER_CONFIG = "configuration-files";

    private String name = STACKS_PROVIDER_CONFIG;

    @Override
    public String getName() {
        return name;
    }

    @Activate
    private void configure(final Map<String, Object> conf) {
        configureName(conf);
    }

    @Modified
    private void modify(final Map<String, Object> conf) {
        configureName(conf);
    }

    private void configureName(final Map<String, Object> conf) {
        name = Optional.ofNullable(conf)
                .map(m -> (String) m.get(CONFIG_PROVIDER_PROP_NAME))
                .orElse(STACKS_PROVIDER_CONFIG);
    }
}
