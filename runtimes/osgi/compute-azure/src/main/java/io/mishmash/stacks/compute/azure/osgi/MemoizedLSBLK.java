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

package io.mishmash.stacks.compute.azure.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mishmash.stacks.common.SoftRefMemoizableAction;

@Component(service={MemoizedLSBLK.class}, immediate=true)
public class MemoizedLSBLK extends
        SoftRefMemoizableAction<Collection<OsDiskPartition>> {

    private static final Logger LOG = Logger.getLogger(
            MemoizedLSBLK.class.getName());

    private static final Duration DEFAULT_MAX_AGE =
            Duration.of(30, ChronoUnit.SECONDS);

    public static final String[] DEFAULT_CMD = new String[] {
            "/usr/bin/lsblk",
            "-o",
            "ID,SIZE,FSSIZE,HCTL,MODEL,NAME,MOUNTPOINT,TYPE,UUID",
            "--bytes",
            "--json"
    };

    protected static final String COL_ID = "id";
    protected static final String COL_SIZE = "size";
    protected static final String COL_FSSIZE = "fssize";
    protected static final String COL_HCTL = "hctl";
    protected static final String COL_MODEL = "model";
    protected static final String COL_NAME = "name";
    protected static final String COL_MOUNT = "mountpoint";
    protected static final String COL_TYPE = "type";
    protected static final String COL_UUID = "uuid";

    protected static final String F_BLOCKDEVS = "blockdevices";
    protected static final String F_CHILDREN = "children";

    public MemoizedLSBLK() {
        super(DEFAULT_MAX_AGE);
    }

    @Override
    protected CompletableFuture<Collection<OsDiskPartition>> prepareAction() {
        try {
            Process p = new ProcessBuilder()
                .command(DEFAULT_CMD)
                .start();

            return CompletableFuture.supplyAsync(() -> {
                try {
                    return parseLslbk(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).whenComplete((c, e) -> {
                if (e != null) {
                    LOG.log(Level.SEVERE, """
                            Could not run lsblk to get disk devices, \
                            disk discovery might fail.
                            """,
                            e);

                    // on error - see if we can kill the process
                    if (p.isAlive()) {
                        p.destroyForcibly();
                    }
                }
            });
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    protected Collection<OsDiskPartition> parseLslbk(final Process proc)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = proc.getInputStream()) {
            JsonNode node = mapper.readTree(in);

            return (Collection<OsDiskPartition>)
                streamArrayChildren(node, F_BLOCKDEVS, false, false)
                    .flatMap(jn -> Stream.concat(
                        Stream.of(jn),
                        streamArrayChildren(jn, F_CHILDREN, true, true)))
                    .map(this::toOsDisk)
                    .toList();
        }
    }

    protected Stream<JsonNode> streamArrayChildren(
            final JsonNode node,
            final String arrayName,
            final boolean fillMissing,
            final boolean recursive) {
        Stream<JsonNode> base = node.optional(arrayName)
                .filter(JsonNode::isArray)
                .map(JsonNode::valueStream)
                .orElse(Stream.empty());

        if (fillMissing) {
            // fill some missing fields with values from parent
            base = base.map(jn -> {
                if (!jn.hasNonNull(COL_HCTL)
                        && node.hasNonNull(COL_HCTL)) {
                    return ((ObjectNode)jn).put(
                            COL_HCTL,
                            node.get(COL_HCTL).asText());
                }

                return jn;
            }).map(jn -> {
                if (!jn.hasNonNull(COL_MODEL)
                        && node.hasNonNull(COL_MODEL)) {
                    return ((ObjectNode)jn).put(
                            COL_MODEL,
                            node.get(COL_MODEL).asText());
                }

                return jn;
            });
        }

        if (!recursive) {
            return base;
        }

        return base
                // recursively extract children
                .flatMap(jn -> Stream.concat(
                        Stream.of(jn),
                        streamArrayChildren(
                                jn,
                                arrayName,
                                fillMissing,
                                recursive)));
    }

    protected OsDiskPartition toOsDisk(final JsonNode node) {
        String hctl = node.optional(COL_HCTL)
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .orElse(null);

        Integer host = null;
        Integer chan = null;
        Integer target = null;
        Integer lun = null;

        if (hctl != null && !hctl.isBlank()) {
            String hctlParts[] = hctl.split(":");
            if (hctlParts.length != 4) {
                throw new IllegalArgumentException("""
                        HCTL parameter must be in the form \
                        <host>:<channel>:<target>:<lun>""");
            }

            host = Integer.valueOf(hctlParts[0]);
            chan = Integer.valueOf(hctlParts[1]);
            target = Integer.valueOf(hctlParts[2]);
            lun = Integer.valueOf(hctlParts[3]);
        }

        return new OsDiskPartition(
                node.optional(COL_ID)
                        .filter(JsonNode::isTextual)
                        .map(JsonNode::asText)
                        .orElse(null),
                node.optional(COL_FSSIZE)
                    .filter(JsonNode::canConvertToLong)
                    .or(() -> node.optional(COL_SIZE)
                                .filter(JsonNode::canConvertToLong))
                    .map(JsonNode::asLong)
                    .map(l -> l / 1024 / 1024)
                    .orElse(null),
                node.optional(COL_MODEL)
                    .filter(JsonNode::isTextual)
                    .map(JsonNode::asText)
                    .orElse(null),
                node.optional(COL_NAME)
                    .filter(JsonNode::isTextual)
                    .map(JsonNode::asText)
                    .orElse(null),
                node.optional(COL_MOUNT)
                    .filter(JsonNode::isTextual)
                    .map(JsonNode::asText)
                    .orElse(null),
                node.optional(COL_TYPE)
                    .filter(JsonNode::isTextual)
                    .map(JsonNode::asText)
                    .orElse(null),
                host, chan, target, lun);
    }
}
