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

package io.mishmash.stacks.oidc.util;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class URIUtils {

    public static boolean isUMA(final URI u) {
        return "uma2".equalsIgnoreCase(u.getScheme());
    }

    public static boolean isOIDC(final URI u) {
        return "oidc".equalsIgnoreCase(u.getScheme());
    }

    public static boolean isJWT(final URI u) {
        return "jwt".equalsIgnoreCase(u.getScheme());
    }

    public static Optional<String> getClientId(final URI u) {
        return getParamLast(u, "client_id")
                .or(() -> Optional.ofNullable(u.getUserInfo())
                    .map(ui -> {
                        int p = ui.indexOf(':');
    
                        return p == 0
                                ? null
                                : URLDecoder.decode(
                                        p < 0
                                            ? ui
                                            : ui.substring(0, p),
                                        StandardCharsets.UTF_8);
                }));
    }

    public static Optional<String> getClientSecret(final URI u) {
        return getParamLast(u, "client_secret")
                .or(() -> Optional.ofNullable(u.getUserInfo())
                    .map(ui -> {
                        int p = ui.indexOf(':');
    
                        return p < 0 || p == ui.length() - 1
                                ? null
                                : URLDecoder.decode(
                                        ui.substring(p + 1),
                                        StandardCharsets.UTF_8);
                }));
    }

    public static Optional<String> getParamLast(
            final URI u,
            final String paramName) {
        List<String> p = getParam(u, paramName);

        return p.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(p.get(p.size() - 1));
    }

    public static List<String> getParam(
            final URI u,
            final String paramName) {
        return getParamsStream(u)
                .filter(e -> paramName.equals(e.getKey()))
                .map(e -> e.getValue())
                .collect(Collectors.toList());
    }

    /**
     * Parse the query string of the given URI and return a stream
     * of (param name, param value) entries. If the query string is
     * missing - returns an empty stream.
     *
     * @param u the URI to extract the parameters from
     * @return a (potentially empty) stream of parameter name,value pairs 
     */
    public static Stream<Map.Entry<String, String>> getParamsStream(
            final URI u) {
        if (u.getQuery() == null) {
            return Stream.empty();
        } else {
            return Arrays
                    .stream(u.getQuery().split("&"))
                    .map(s -> {
                        int p = s.indexOf('=');

                        return new SimpleEntry<>(
                                p == 0
                                    ? null
                                    : URLDecoder.decode(
                                            p < 0
                                                ? s
                                                : s.substring(0, p),
                                            StandardCharsets.UTF_8),
                                p < 0 || p == s.length() - 1
                                    ? null
                                    : URLDecoder.decode(
                                            s.substring(p + 1),
                                            StandardCharsets.UTF_8)
                                );
                    });
        }
    }

    public static Optional<String> getIssuer(final URI u) {
        return Optional.ofNullable(u.getHost())
                .map(h -> h + Optional.ofNullable(u.getPath()).orElse("/"));
    }
}
