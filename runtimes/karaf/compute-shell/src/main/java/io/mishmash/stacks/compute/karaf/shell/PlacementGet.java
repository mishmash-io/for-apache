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

import java.util.List;
import java.util.function.Supplier;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import io.mishmash.stacks.compute.common.Placement;

@Service
@Command(scope="stacks", name="placement-get", description="Show the compute placement of the local host")
public class PlacementGet implements Action {

    @Reference(optional=true)
    Placement placement;

    @Option(name="-p", aliases="--provider-name", description="Show compute provider name", required=false, multiValued=false)
    Boolean providerNameOpt = Boolean.FALSE;

    @Option(name="-o", aliases="--geography", description="Show placement geography", required=false, multiValued=false)
    Boolean geographyOpt = Boolean.FALSE;

    @Option(name="-j", aliases="--jurisdiction", description="Show placement jurisdiction", required=false, multiValued=false)
    Boolean jurisdictionOpt = Boolean.FALSE;

    @Option(name="-r", aliases="--region", description="Show placement region", required=false, multiValued=false)
    Boolean regionOpt = Boolean.FALSE;

    @Option(name="-z", aliases="--zone", description="Show placement zone", required=false, multiValued=false)
    Boolean zoneOpt = Boolean.FALSE;

    @Option(name="-g", aliases="--group", description="Show placement group", required=false, multiValued=false)
    Boolean groupOpt = Boolean.FALSE;

    @Option(name="-f", aliases="--fault-domain", description="Show fault domain", required=false, multiValued=false)
    Boolean faultDomainOpt = Boolean.FALSE;

    @Option(name="-u", aliases="--update-domain", description="Show update domain", required=false, multiValued=false)
    Boolean updateDomainOpt = Boolean.FALSE;

    private List<Triple<Supplier<Boolean>, String, Supplier<String>>> opts = List.of(
            Triple.of(() -> this.providerNameOpt, "Provider", this::getProviderName),
            Triple.of(() -> this.geographyOpt, "Geography", this::getGeography),
            Triple.of(() -> this.jurisdictionOpt, "Jurisdiction", this::getJurisdiction),
            Triple.of(() -> this.regionOpt, "Region", this::getRegion),
            Triple.of(() -> this.zoneOpt, "Zone", this::getZone),
            Triple.of(() -> this.groupOpt, "Group", this::getGroup),
            Triple.of(() -> this.faultDomainOpt, "Fault Domain", this::getFaultDomain),
            Triple.of(() -> this.updateDomainOpt, "Update Domain", this::getUpdateDomain));

    @Override
    public Object execute() throws Exception {
        List<Triple<Supplier<Boolean>, String, Supplier<String>>> givenOpts =
                opts.stream()
                    .filter(o -> o.getLeft().get())
                    .toList();

        if (givenOpts.size() == 1) {
            // when only one option is requested, don't print a table
            if (placement == null) {
                // don't have a registered placement
                return null;
            } else {
                // just return the value
                return opts.getFirst().getRight().get();
            }
        } else {
            // more than one (or all) options requested, print a table
            ShellTable tbl = new ShellTable();
            tbl.emptyTableText("Compute placement not present");

            // set table headers
            List<String> hdrs = givenOpts.isEmpty()
                    ? opts.stream().map(Triple::getMiddle).toList()
                    : givenOpts.stream().map(Triple::getMiddle).toList();
            hdrs.forEach(tbl::column);

            if (placement != null) {
                // set the table values
                List<String> vals = givenOpts.isEmpty()
                        ? opts.stream()
                                .map(Triple::getRight)
                                .map(Supplier::get)
                                .toList()
                        : givenOpts.stream()
                                .map(Triple::getRight)
                                .map(Supplier::get)
                                .toList();
                tbl.addRow().addContent(vals.toArray());
            }

            tbl.print(System.out);

            return null;
        }
    }

    private String getProviderName() {
        return placement.getProvider().getName();
    }

    private String getGeography() {
        return placement.getGeography()
                .map(g -> g.getId())
                .orElse("");
    }

    private String getJurisdiction() {
        return placement.getGeography()
                .flatMap(g -> g.getJurisdiction())
                .map(j -> j.getName())
                .orElse("");
    }

    private String getRegion() {
        return placement.getRegion()
                .map(r -> r.getId())
                .orElse("");
    }

    private String getZone() {
        return placement.getZone()
                .map(z -> z.getId())
                .orElse("");
    }

    private String getGroup() {
        return placement.getGroup()
                .map(g -> g.getId())
                .orElse("");
    }

    private String getFaultDomain() {
        return placement.getFaultDomain()
                .map(d -> d.getId())
                .orElse("");
    }

    private String getUpdateDomain() {
        return placement.getUpdateDomain()
                .map(d -> d.getId())
                .orElse("");
    }
}
