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

import java.net.InetAddress;
import java.util.HexFormat;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import io.mishmash.stacks.compute.common.NetworkAccess;

@Service
@Command(scope="stacks", name="network-list", description="List distributed computing stacks network access options")
public class NetworkList implements Action {

    @Reference(optional=true)
    List<NetworkAccess> networks;

    @Override
    public Object execute() throws Exception {
        ShellTable tbl = new ShellTable();
        tbl.column("Public address");
        tbl.column("Private address");
        tbl.column("Network prefix");
        tbl.column("Hardware address");
        tbl.emptyTableText("Network addresses not found");

        if (networks != null) {
            for (NetworkAccess n : networks) {
                tbl.addRow().addContent(
                        n.getExternalAddress()
                            .map(InetAddress::toString)
                            .orElse(""),
                        n.getAddress().toString(),
                        n.getPrefixLength(),
                        n.getHardwareAddress()
                            .map(a -> HexFormat.ofDelimiter(":").formatHex(a))
                            .orElse(""));
            }
        }
        tbl.print(System.out);

        return null;
    }

}
