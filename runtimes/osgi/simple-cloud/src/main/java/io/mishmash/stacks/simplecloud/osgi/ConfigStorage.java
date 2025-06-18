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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

import io.mishmash.stacks.common.compute.Storage;

@Component(
        service={Storage.class},
        immediate=true,
        configurationPolicy=ConfigurationPolicy.REQUIRE,
        configurationPid={ConfigStorage.CONFIG_STORAGE_PID})
public class ConfigStorage implements Storage {

    private static final Logger LOG = Logger.getLogger(
            ConfigStorage.class.getName());

    public static final String CONFIG_STORAGE_PID = "stacksStorage";

    public static final String CONFIG_STORAGE_PROP_ID = "id";
    public static final String CONFIG_STORAGE_PROP_TIER = "tier";
    public static final String CONFIG_STORAGE_PROP_EPHEMERAL = "isEphemeral";
    public static final String CONFIG_STORAGE_PROP_SIZE = "sizeGB";
    public static final String CONFIG_STORAGE_PROP_MODEL = "model";
    public static final String CONFIG_STORAGE_PROP_URI = "uri";

    private String servicePid;
    private String id;
    private String tier;
    private String isEphemeral;
    private String sizeGB;
    private String model;
    private String uri;

    @Override
    public String getId() {
        return Optional.ofNullable(id)
                // use servicePid as default
                .orElse(servicePid);
    }

    @Override
    public Optional<String> getTier() {
        return Optional.ofNullable(tier);
    }

    @Override
    public boolean isEphemeral() {
        return Optional.ofNullable(isEphemeral)
                .filter(s -> {
                    if ("true".equalsIgnoreCase(s)
                            || "false".equalsIgnoreCase(s)) {
                        return true;
                    }

                    LOG.log(Level.WARNING,
                            "Failed to parse boolean "
                            + CONFIG_STORAGE_PROP_EPHEMERAL
                            + " "
                            + isEphemeral
                            + " configured in "
                            + servicePid
                            + ", returning false.");

                    return false;
                })
                .map(Boolean::valueOf)
                .orElse(Boolean.FALSE);
    }

    @Override
    public long getSizeGB() {
        if (sizeGB == null || sizeGB.isBlank()) {
            String reason = "Missing required "
                    + CONFIG_STORAGE_PROP_SIZE
                    + " in configuration "
                    + servicePid;

            LOG.log(Level.SEVERE, reason);
            throw new RuntimeException(reason);
        }

        try {
            return Long.valueOf(sizeGB);
        } catch (NumberFormatException e) {
            String reason = "Failed to parse integer "
                    + CONFIG_STORAGE_PROP_SIZE
                    + " in configuration "
                    + servicePid
                    + ": "
                    + e.getMessage();

            LOG.log(Level.SEVERE, reason);
            throw new RuntimeException(reason, e);
        }
    }

    @Override
    public Optional<String> getModel() {
        return Optional.ofNullable(model);
    }

    @Override
    public Optional<URI> getURI() {
        return Optional.ofNullable(uri)
                .map(u -> {
                    try {
                        return new URI(u);
                    } catch (URISyntaxException e) {
                        LOG.log(Level.WARNING,
                                "Failed to parse URI "
                                + CONFIG_STORAGE_PROP_URI
                                + " in configuration "
                                + servicePid
                                + ": " + e.getMessage()
                                + ". Treating as missing.");

                        return null;
                    }
                });
    }

    @Activate
    private void configure(final Map<String, Object> conf) {
        servicePid = (String) conf.get("service.pid");
        configureId(conf);
        configureTier(conf);
        configureEphemeral(conf);
        configureSize(conf);
        configureModel(conf);
        configureUri(conf);
    }

    @Modified
    private void modify(final Map<String, Object> conf) {
        configureId(conf);
        configureTier(conf);
        configureEphemeral(conf);
        configureSize(conf);
        configureModel(conf);
        configureUri(conf);
    }

    protected void configureId(final Map<String, Object> conf) {
        id = (String) conf.get(CONFIG_STORAGE_PROP_ID);
    }

    protected void configureTier(final Map<String, Object> conf) {
        tier = (String) conf.get(CONFIG_STORAGE_PROP_TIER);
    }

    protected void configureEphemeral(final Map<String, Object> conf) {
        isEphemeral = (String) conf.get(CONFIG_STORAGE_PROP_EPHEMERAL);
    }

    protected void configureSize(final Map<String, Object> conf) {
        sizeGB = (String) conf.get(CONFIG_STORAGE_PROP_SIZE);
    }

    protected void configureModel(final Map<String, Object> conf) {
        model = (String) conf.get(CONFIG_STORAGE_PROP_MODEL);
    }

    protected void configureUri(final Map<String, Object> conf) {
        uri = (String) conf.get(CONFIG_STORAGE_PROP_URI);
    }
}
