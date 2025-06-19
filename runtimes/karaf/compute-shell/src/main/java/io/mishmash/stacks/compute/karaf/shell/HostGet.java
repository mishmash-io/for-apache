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
import org.apache.commons.lang3.arch.Processor;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import io.mishmash.stacks.compute.common.ComputeHost;

@Service
@Command(scope="stacks", name="host-get", description="Show this stacks host")
public class HostGet implements Action {

    @Reference(optional=true)
    ComputeHost host;

    @Option(name="-i", aliases="--id", description="Show host id", required=false, multiValued=false)
    Boolean idOpt = Boolean.FALSE;

    @Option(name="-o", aliases="--model", description="Show host model", required=false, multiValued=false)
    Boolean modelOpt = Boolean.FALSE;

    @Option(name="-a", aliases={"--arch", "--architecture"}, description="Show CPU architecture", required=false, multiValued=false)
    Boolean archOpt = Boolean.FALSE;

    @Option(name="-m", aliases="--memory", description="Show memory (in MB)", required=false, multiValued=false)
    Boolean memOpt = Boolean.FALSE;

    @Option(name="-c", aliases="--cpus", description="Show number of CPU cores", required=false, multiValued=false)
    Boolean coresOpt = Boolean.FALSE;

    @Option(name="-p", aliases="--provider", description="Show compute provider name", required=false, multiValued=false)
    Boolean providerNameOpt = Boolean.FALSE;

    @Option(name="-t", aliases="--tier", description="Show stacks compute tier", required=false, multiValued=false)
    Boolean tierOpt = Boolean.FALSE;

    private List<Triple<Supplier<Boolean>, String, Supplier<String>>> opts = List.of(
            Triple.of(() -> this.idOpt, "ID", this::getId),
            Triple.of(() -> this.modelOpt, "Model", this::getModel),
            Triple.of(() -> this.memOpt, "Memory (MB)", this::getMemoryMB),
            Triple.of(() -> this.coresOpt, "CPU cores", this::getNumCpuCores),
            Triple.of(() -> this.archOpt, "CPU architecture", this::getArch),
            Triple.of(() -> this.tierOpt, "Stacks Tier", this::getTier),
            Triple.of(() -> this.providerNameOpt, "Provider", this::getProviderName));

    @Override
    public Object execute() throws Exception {
        List<Triple<Supplier<Boolean>, String, Supplier<String>>> givenOpts =
                opts.stream()
                    .filter(t -> t.getLeft().get())
                    .toList();

        if (givenOpts.size() == 1) {
            // when only one option is requested don't print a table
            if (host == null) {
                // we don't have the value
                return null;
            } else {
                // return the value
                return givenOpts.getFirst().getRight().get();
            }
        } else {
            // more than one, or all options requested, print a table
            ShellTable tbl = new ShellTable();
            tbl.emptyTableText("Compute host not present");

            // set table headers
            List<String> hdrs = givenOpts.isEmpty()
                    ? opts.stream().map(Triple::getMiddle).toList()
                    : givenOpts.stream().map(Triple::getMiddle).toList();
            hdrs.forEach(tbl::column);

            if (host != null) {
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

    private String getId() {
        return host.uniqueId();
    }

    private String getArch() {
        return host.getCPUArchitecture()
                .map(Processor::toString)
                .orElse("");
    }

    private String getMemoryMB() {
        return host.getMemoryMB()
                .map(i -> i.toString())
                .orElse("");
    }

    private String getModel() {
        return host.getModel()
                .orElse("");
    }

    private String getNumCpuCores() {
        return host.getNumCPUCores()
                .map(i -> i.toString())
                .orElse("");
    }

    private String getTier() {
        return host.getTier()
                .orElse("");
    }

    private String getProviderName() {
        return host.getProvider().getName();
    }
}
