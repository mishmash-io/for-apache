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

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import io.mishmash.stacks.compute.common.ComputeProvider;

@Service
@Command(scope="stacks", name="provider-get", description="Show the stacks compute provider")
public class ProviderGet implements Action {

    @Reference(optional=true)
    ComputeProvider provider;

    @Option(name="-n", aliases="--name", description="Show compute provider name", required=false, multiValued=false)
    Boolean nameOpt = Boolean.FALSE;

    @Override
    public Object execute() throws Exception {
        if (nameOpt) {
            if (provider == null) {
                // fail, return null
                return null;
            } else {
                // do not print a table when a single value is requested
                return provider.getName();
            }
        } else {
            // by default - print a table
            ShellTable tbl = new ShellTable();
            tbl.column("Name");
            tbl.emptyTableText("Compute provider not present");

            if (provider != null) {
                tbl.addRow()
                    .addContent(provider.getName());
            }

            tbl.print(System.out);
        }

        return null;
    }
}
