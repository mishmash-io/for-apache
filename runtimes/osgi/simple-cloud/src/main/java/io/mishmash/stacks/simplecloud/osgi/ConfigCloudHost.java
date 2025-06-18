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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ArchUtils;
import org.apache.commons.lang3.arch.Processor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import io.mishmash.stacks.common.compute.CloudHost;
import io.mishmash.stacks.common.compute.CloudProvider;

@Component(
        service={CloudHost.class},
        immediate=true,
        configurationPolicy=ConfigurationPolicy.REQUIRE,
        configurationPid={ConfigCloudHost.CONFIG_HOST_PID})
public class ConfigCloudHost implements CloudHost {

    private static final Logger LOG = Logger.getLogger(
            ConfigCloudHost.class.getName());

    public static final String CONFIG_HOST_PID = "stacksHost";

    public static final String CONFIG_HOST_PROP_ID = "id";
    public static final String CONFIG_HOST_PROP_MEM = "memoryMB";
    public static final String CONFIG_HOST_PROP_CPU = "cpuCores";
    public static final String CONFIG_HOST_PROP_ARCH = "cpuArchitecture";
    public static final String CONFIG_HOST_PROP_VM = "isVirtual";
    public static final String CONFIG_HOST_PROP_TIER = "tier";
    public static final String CONFIG_HOST_PROP_MODEL = "model";

    @Reference
    private ConfigCloudProvider provider;

    private String servicePid;
    private String id;
    private String memoryMB;
    private String cpuCores;
    private String cpuArch;
    private String isVirtual;
    private String tier;
    private String model;

    @Override
    public String uniqueId() {
        if (id == null || id.isBlank()) {
            // return a default from the hostname
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }

        return id;
    }

    @Override
    public Optional<Integer> getMemoryMB() {
        if (memoryMB == null || memoryMB.isBlank()) {
            // return a default from java
            return Optional.of((int)
                    (Runtime
                            .getRuntime()
                            .totalMemory() / 1024));
        }

        try {
            return Optional.of(Integer.parseInt(cpuCores));
        } catch (NumberFormatException e) {
            LOG.log(Level.WARNING,
                    "Failed to parse integer "
                    + CONFIG_HOST_PROP_CPU
                    + " "
                    + cpuCores
                    + " configured in "
                    + servicePid
                    + ", treating as missing. Error is: "
                    , e);

            return Optional.empty();
        }
    }

    @Override
    public Optional<Integer> getNumCPUCores() {
        if (cpuCores == null || cpuCores.isBlank()) {
            // return a default with the number of java cores
            return Optional.of(Runtime.getRuntime().availableProcessors());
        }

        try {
            return Optional.of(Integer.parseInt(cpuCores));
        } catch (NumberFormatException e) {
            LOG.log(Level.WARNING,
                    "Failed to parse integer "
                    + CONFIG_HOST_PROP_CPU
                    + " "
                    + cpuCores
                    + " configured in "
                    + servicePid
                    + ", treating as missing. Error is: "
                    , e);

            return Optional.empty();
        }
    }

    @Override
    public Optional<Processor> getCPUArchitecture() {
        if (cpuArch == null || cpuArch.isBlank()) {
            // determine from Java (the os.arch property)
            return Optional.of(ArchUtils.getProcessor());
        }

        return Optional
                .ofNullable(ArchUtils.getProcessor(cpuArch))
                .or(() -> {
                    LOG.log(Level.WARNING,
                            "Failed to parse "
                            + CONFIG_HOST_PROP_ARCH
                            + " configured in "
                            + servicePid
                            + ", should be one of java's os.arch values, but is "
                            + cpuArch
                            + ". Treating as missing.");
                    return Optional.empty();
                });
    }

    @Override
    public Optional<Boolean> isVirtual() {
        return Optional.ofNullable(isVirtual)
                .filter(s -> {
                    if ("true".equalsIgnoreCase(s)
                            || "false".equalsIgnoreCase(s)) {
                        return true;
                    }

                    LOG.log(Level.WARNING,
                            "Failed to parse boolean "
                            + CONFIG_HOST_PROP_VM
                            + " "
                            + isVirtual
                            + " configured in "
                            + servicePid
                            + ", treating as missing.");

                    return false;
                })
                .map(Boolean::valueOf);
    }

    @Override
    public Optional<String> getTier() {
        return Optional.ofNullable(tier);
    }

    @Override
    public Optional<String> getModel() {
        return Optional.ofNullable(model);
    }

    @Override
    public CloudProvider getProvider() {
        return provider;
    }

    @Activate
    private void configure(final Map<String, Object> conf) {
        servicePid = (String) conf.get("service.pid");
        configureId(conf);
        configureMemory(conf);
        configureCpuCores(conf);
        configureCpuArch(conf);
        configureIsVirtual(conf);
        configureTier(conf);
        configureModel(conf);
    }

    @Modified
    private void modify(final Map<String, Object> conf) {
        configureId(conf);
        configureMemory(conf);
        configureCpuCores(conf);
        configureCpuArch(conf);
        configureIsVirtual(conf);
        configureTier(conf);
        configureModel(conf);
    }

    protected void configureId(final Map<String, Object> conf) {
        id = (String) conf.get(CONFIG_HOST_PROP_ID);
    }

    protected void configureMemory(final Map<String, Object> conf) {
        memoryMB = (String) conf.get(CONFIG_HOST_PROP_MEM);
    }

    protected void configureCpuCores(final Map<String, Object> conf) {
        cpuCores = (String) conf.get(CONFIG_HOST_PROP_CPU);
    }

    protected void configureCpuArch(final Map<String, Object> conf) {
        cpuArch = (String) conf.get(CONFIG_HOST_PROP_ARCH);
    }

    protected void configureIsVirtual(final Map<String, Object> conf) {
        isVirtual = (String) conf.get(CONFIG_HOST_PROP_VM);
    }

    protected void configureTier(final Map<String, Object> conf) {
        tier = (String) conf.get(CONFIG_HOST_PROP_TIER);
    }

    protected void configureModel(final Map<String, Object> conf) {
        model = (String) conf.get(CONFIG_HOST_PROP_MODEL);
    }
}
