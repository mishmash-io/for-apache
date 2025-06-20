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

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import io.mishmash.stacks.compute.common.Storage;

@Service
@Command(scope="stacks", name="storage-list", description="List distributed computing stacks disks")
public class StorageList implements Action {

    @Reference(optional=true)
    List<Storage> storageList;

    @Override
    public Object execute() throws Exception {
        ShellTable tbl = new ShellTable();
        tbl.column("ID");
        tbl.column("Model");
        tbl.column("Size (GB)");
        tbl.column("Stacks tier");
        tbl.column("Access URI");
        tbl.emptyTableText("Disks not found");

        if (storageList != null) {
            for (Storage s : storageList) {
                tbl.addRow().addContent(
                        s.getId(),
                        s.getModel().orElse(""),
                        s.getSizeGB(),
                        s.getTier().orElse(""),
                        s.getURI()
                            .map(URI::toString)
                            .orElse("")
                        );
            }
        }
        tbl.print(System.out);

        return null;
    }

}
