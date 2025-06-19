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
import java.util.Set;

import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

import io.mishmash.stacks.compute.common.Storage;

@Service
public class StorageIdCompleter implements Completer {

    @Reference(optional=true)
    List<Storage> storageList;

    @Override
    public int complete(
            final Session session,
            final CommandLine commandLine,
            final List<String> candidates) {
        StringsCompleter completer = new StringsCompleter();
        Set<String> completerSet = completer.getStrings();
        (storageList == null ? List.<Storage>of() : storageList)
            .stream()
            .map(Storage::getId)
            .forEach(completerSet::add);

        return completer.complete(session, commandLine, candidates);
    }
}
