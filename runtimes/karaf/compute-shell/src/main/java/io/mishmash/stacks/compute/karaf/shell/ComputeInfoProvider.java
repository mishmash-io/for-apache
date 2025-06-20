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

package io.mishmash.stacks.compute.karaf.shell;

import java.util.Properties;

import org.apache.karaf.shell.commands.info.InfoProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import io.mishmash.stacks.compute.common.ComputeHost;
import io.mishmash.stacks.compute.common.ComputeProvider;
import io.mishmash.stacks.compute.common.Placement;

/*
 * adds info to shell:info
 */
@Component(service={InfoProvider.class}, immediate=true)
public class ComputeInfoProvider implements InfoProvider {

    @Reference(cardinality=ReferenceCardinality.OPTIONAL)
    private ComputeHost host;
    @Reference(cardinality=ReferenceCardinality.OPTIONAL)
    private ComputeProvider provider;
    @Reference(cardinality=ReferenceCardinality.OPTIONAL)
    private Placement placement;

    @Override
    public String getName() {
        return "Compute";
    }

    @Override
    public Properties getProperties() {
        Properties p = new Properties();

        if (provider != null) {
            p.put("Provider name", provider.getName());
        }

        if (host != null) {
            p.put("Host ID", host.uniqueId());
            host.getTier()
                .ifPresent(t -> p.put("Stacks compute tier", t));
            host.getModel()
                .ifPresent(m -> p.put("Model", m));
        }

        if (placement != null) {
            placement.getGeography()
                .map(g -> g.getId())
                .ifPresent(i -> p.put("Placement geography", i));
            placement.getGeography()
                .flatMap(g -> g.getJurisdiction())
                .map(j -> j.getName())
                .ifPresent(jn -> p.put("Placement jurisdiction", jn));
            placement.getRegion()
                .map(r -> r.getId())
                .ifPresent(ri -> p.put("Placement region", ri));
            placement.getZone()
                .map(z -> z.getId())
                .ifPresent(zi -> p.put("Placement zone", zi));
            placement.getGroup()
                .map(g -> g.getId())
                .ifPresent(gi -> p.put("Placement group", gi));
            placement.getFaultDomain()
                .map(f -> f.getId())
                .ifPresent(fi -> p.put("Placement fault domain", fi));
            placement.getUpdateDomain()
                .map(u -> u.getId())
                .ifPresent(ui -> p.put("Placement update domain", ui));
        }

        return p;
    }

}
