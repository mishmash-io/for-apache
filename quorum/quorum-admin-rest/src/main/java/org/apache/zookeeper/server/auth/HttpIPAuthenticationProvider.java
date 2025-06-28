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

package org.apache.zookeeper.server.auth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Id;

public class HttpIPAuthenticationProvider extends IPAuthenticationProvider {
    public static final String X_FORWARDED_FOR_HEADER_NAME = "X-Forwarded-For";

    public static final String USE_X_FORWARDED_FOR_KEY = "zookeeper.IPAuthenticationProvider.usexforwardedfor";

    @Override
    public <T> List<Id> authenticate(Class<T> klass, T conn, byte[] authData) throws KeeperException {
        if (HttpServletRequest.class.equals(klass)) {
            return authenticateHttp((HttpServletRequest) conn);
        } else {
            return super.authenticate(klass, conn, authData);
        }
    }

    protected List<Id> authenticateHttp(HttpServletRequest request) {
        final List<Id> ids = new ArrayList<>();

        final String ip = getClientIPAddress(request);
        ids.add(new Id(getScheme(), ip));

        return Collections.unmodifiableList(ids);
    }

    /**
     * Returns the HTTP(s) client IP address
     * @param request HttpServletRequest
     * @return IP address
     */
    public static String getClientIPAddress(final HttpServletRequest request) {
        if (!Boolean.getBoolean(USE_X_FORWARDED_FOR_KEY)) {
            return request.getRemoteAddr();
        }

        // to handle the case that a HTTP(s) client connects via a proxy or load balancer
        final String xForwardedForHeader = request.getHeader(X_FORWARDED_FOR_HEADER_NAME);
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        }
        // the format of the field is: X-Forwarded-For: client, proxy1, proxy2 ...
        return new StringTokenizer(xForwardedForHeader, ",").nextToken().trim();
    }
}
