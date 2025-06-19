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

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import io.mishmash.stacks.compute.common.Storage;

@Service
@Command(scope="stacks", name="storage-get", description="Get details of a compute storage")
public class StorageGet implements Action {

    @Reference(optional=true)
    List<Storage> storageList;

    @Option(name="-i", aliases="--id", description="The storage ID", required=true, multiValued=false)
    @Completion(StorageIdCompleter.class)
    String idOpt;

    @Option(name="-o", aliases="--model", description="Show storage model", required=false, multiValued=false)
    Boolean modelOpt = Boolean.FALSE;

    @Option(name="-o", aliases="--model", description="Show storage size (in GB)", required=false, multiValued=false)
    Boolean sizeOpt = Boolean.FALSE;

    @Option(name="-t", aliases="--tier", description="Show stacks storage tier", required=false, multiValued=false)
    Boolean tierOpt = Boolean.FALSE;

    @Option(name="-u", aliases={"--uri", "--url"}, description="Show disk access URI", required=false, multiValued=false)
    Boolean uriOpt = Boolean.FALSE;

    private List<Triple<Supplier<Boolean>, String, Function<Storage, String>>> opts = List.of(
            Triple.of(() -> modelOpt, "Model", this::getModel),
            Triple.of(() -> sizeOpt, "Size (GB)", this::getSize),
            Triple.of(() -> tierOpt, "Stacks tier", this::getTier),
            Triple.of(() -> uriOpt, "URI", this::getURI));

    @Override
    public Object execute() throws Exception {
        Optional<Storage> storage = (storageList == null
                                        ? List.<Storage>of()
                                        : storageList)
                .stream()
                .filter(s -> idOpt.equals(s.getId()))
                .findAny();
        List<Triple<Supplier<Boolean>, String, Function<Storage, String>>> givenOpts =
                opts.stream()
                    .filter(t -> t.getLeft().get())
                    .toList();

        if (givenOpts.size() == 1) {
            // when only one option is requested don't print a table
            if (storage.isEmpty()) {
                // we didn't find a disk
                return null;
            } else {
                // return the value
                return givenOpts.getFirst().getRight().apply(storage.get());
            }
        } else {
            // more than one, or all options requested, print a table
            ShellTable tbl = new ShellTable();
            tbl.emptyTableText("Storage " + idOpt + " not found");

            // set table headers
            List<String> hdrs = givenOpts.isEmpty()
                    ? opts.stream().map(Triple::getMiddle).toList()
                    : givenOpts.stream().map(Triple::getMiddle).toList();
            hdrs.forEach(tbl::column);

            if (!storage.isEmpty()) {
                // set the table values
                List<String> vals = givenOpts.isEmpty()
                        ? opts.stream()
                                .map(Triple::getRight)
                                .map(f -> f.apply(storage.get()))
                                .toList()
                        : givenOpts.stream()
                                .map(Triple::getRight)
                                .map(f -> f.apply(storage.get()))
                                .toList();
                tbl.addRow().addContent(vals.toArray());
            }

            tbl.print(System.out);

            return null;
        }
    }

    private String getModel(final Storage s) {
        return s.getModel().orElse("");
    }

    private String getSize(final Storage s) {
        return String.valueOf(s.getSizeGB());
    }

    private String getTier(final Storage s) {
        return s.getTier().orElse("");
    }

    private String getURI(final Storage s) {
        return s.getURI().map(URI::toString).orElse("");
    }
}
