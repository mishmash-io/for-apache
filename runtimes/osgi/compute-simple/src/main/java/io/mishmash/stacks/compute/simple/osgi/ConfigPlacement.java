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

package io.mishmash.stacks.compute.simple.osgi;

import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import io.mishmash.stacks.compute.common.ComputeProvider;
import io.mishmash.stacks.compute.common.Placement;
import io.mishmash.stacks.compute.common.PlacementDomain;
import io.mishmash.stacks.compute.common.PlacementGeography;
import io.mishmash.stacks.compute.common.PlacementGroup;
import io.mishmash.stacks.compute.common.PlacementJurisdiction;
import io.mishmash.stacks.compute.common.PlacementRegion;
import io.mishmash.stacks.compute.common.PlacementZone;

@Component(
        service={Placement.class},
        immediate=true,
        configurationPolicy=ConfigurationPolicy.REQUIRE,
        configurationPid={ConfigPlacement.CONFIG_PLACEMENT_PID})
public class ConfigPlacement implements Placement {

    public static final String CONFIG_PLACEMENT_PID = "computePlacement";

    public static final String CONFIG_PLACEMENT_PROP_GEO = "geography";
    public static final String CONFIG_PLACEMENT_PROP_JURISDICTION =
            "jurisdiction";
    public static final String CONFIG_PLACEMENT_PROP_REGION = "region";
    public static final String CONFIG_PLACEMENT_PROP_ZONE = "zone";
    public static final String CONFIG_PLACEMENT_PROP_GROUP = "group";
    public static final String CONFIG_PLACEMENT_PROP_UPDATE_DOM =
            "updateDomain";
    public static final String CONFIG_PLACEMENT_PROP_FAULT_DOM =
            "faultDomain";

    @Reference
    private ConfigComputeProvider provider;

    //private String servicePid;
    private String geography;
    private String jurisdiction;
    private String region;
    private String zone;
    private String group;
    private String updateDomain;
    private String faultDomain;

    @Override
    public Optional<PlacementGeography> getGeography() {
        final String g = geography == null
                ? null
                : new String(geography);
        final String j = jurisdiction == null
                ? null
                : new String(jurisdiction);

        return Optional.ofNullable(g)
                .map(gg -> new PlacementGeography() {
                    @Override
                    public String getId() {
                        return g;
                    }
                    @Override
                    public Optional<PlacementJurisdiction> getJurisdiction() {
                        return Optional.ofNullable(j)
                                .map(j -> new PlacementJurisdiction() {
                                    @Override
                                    public String getName() {
                                        return j;
                                    }
                                });
                    }
                });
    }

    @Override
    public Optional<PlacementRegion> getRegion() {
        return Optional.ofNullable(region)
                .map(String::new)
                .map(r -> new PlacementRegion() {
                    @Override
                    public String getId() {
                        return r;
                    }
                });
    }

    @Override
    public Optional<PlacementZone> getZone() {
        return Optional.ofNullable(zone)
                .map(String::new)
                .map(z -> new PlacementZone() {
                    @Override
                    public String getId() {
                        return z;
                    }
                });
    }

    @Override
    public Optional<PlacementGroup> getGroup() {
        return Optional.ofNullable(group)
                .map(String::new)
                .map(g -> new PlacementGroup() {
                    @Override
                    public String getId() {
                        return g;
                    }
                });
    }

    @Override
    public Optional<PlacementDomain> getFaultDomain() {
        return Optional.ofNullable(faultDomain)
                .map(String::new)
                .map(f -> new PlacementDomain() {
                    @Override
                    public String getId() {
                        return f;
                    } 
                });
    }

    @Override
    public Optional<PlacementDomain> getUpdateDomain() {
        return Optional.ofNullable(updateDomain)
                .map(String::new)
                .map(u -> new PlacementDomain() {
                    @Override
                    public String getId() {
                        return u;
                    }
                });
    }

    @Override
    public ComputeProvider getProvider() {
        return provider;
    }

    @Activate
    private void configure(final Map<String, Object> conf) {
        configureGeography(conf);
        configureJurisdiction(conf);
        configureRegion(conf);
        configureZone(conf);
        configureGroup(conf);
        configureUpdateDomain(conf);
        configureFaultDomain(conf);
    }

    @Modified
    private void modify(final Map<String, Object> conf) {
        configureGeography(conf);
        configureJurisdiction(conf);
        configureRegion(conf);
        configureZone(conf);
        configureGroup(conf);
        configureUpdateDomain(conf);
        configureFaultDomain(conf);
    }

    protected void configureGeography(final Map<String, Object> conf) {
        geography = (String) conf.get(CONFIG_PLACEMENT_PROP_GEO);
    }

    protected void configureJurisdiction(final Map<String, Object> conf) {
        jurisdiction = (String) conf.get(CONFIG_PLACEMENT_PROP_JURISDICTION);
    }

    protected void configureRegion(final Map<String, Object> conf) {
        region = (String) conf.get(CONFIG_PLACEMENT_PROP_REGION);
    }

    protected void configureZone(final Map<String, Object> conf) {
        zone = (String) conf.get(CONFIG_PLACEMENT_PROP_ZONE);
    }

    protected void configureGroup(final Map<String, Object> conf) {
        group = (String) conf.get(CONFIG_PLACEMENT_PROP_GROUP);
    }

    protected void configureUpdateDomain(final Map<String, Object> conf) {
        updateDomain = (String) conf.get(CONFIG_PLACEMENT_PROP_UPDATE_DOM);
    }

    protected void configureFaultDomain(final Map<String, Object> conf) {
        faultDomain = (String) conf.get(CONFIG_PLACEMENT_PROP_FAULT_DOM);
    }
}
