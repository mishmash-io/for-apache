/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.cli;

import java.util.HexFormat;

public class HexDumpOutputFormatter implements OutputFormatter {

    public static final HexDumpOutputFormatter INSTANCE = new HexDumpOutputFormatter();

    @Override
    public String format(byte[] data) {
        HexFormat format = HexFormat.ofDelimiter(" ");
        StringBuilder res = new StringBuilder();
        for (int i = 0; i <= data.length % 16; i++) {
            if (data.length > (i + 1) * 16) {
                res.append(format.formatHex(data, i * 16, (i + 1) * 16));
                res.append(System.lineSeparator());
            } else {
                res.append(format.formatHex(data, i * 16, data.length + 1));
            }
        }

        return res.toString();
    }
}
