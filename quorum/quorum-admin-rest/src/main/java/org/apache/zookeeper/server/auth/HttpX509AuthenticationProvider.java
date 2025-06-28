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

import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.common.X509Exception;
import org.apache.zookeeper.data.Id;

public class HttpX509AuthenticationProvider extends X509AuthenticationProvider {

    public static final String X509_CERTIFICATE_ATTRIBUTE_NAME = "jakarta.servlet.request.X509Certificate";

    public HttpX509AuthenticationProvider() throws X509Exception {
        super();
    }

    public HttpX509AuthenticationProvider(X509TrustManager trustManager, X509KeyManager keyManager) {
        super(trustManager, keyManager);
    }

    @Override
    public <T> List<Id> authenticate(Class<T> klass, T conn, byte[] authData) throws KeeperException {
        if (HttpServletRequest.class.equals(klass)) {
            return authenticateHttp((HttpServletRequest) conn);
        } else {
            return super.authenticate(klass, conn, authData);
        }
    }

    protected List<Id> authenticateHttp(HttpServletRequest request) {
        final X509Certificate[] certChain =
                (X509Certificate[]) request.getAttribute(X509_CERTIFICATE_ATTRIBUTE_NAME);
        return handleAuthentication(certChain);
    }

}
