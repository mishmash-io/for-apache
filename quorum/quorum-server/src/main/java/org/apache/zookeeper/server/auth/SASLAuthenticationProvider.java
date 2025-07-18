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

import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.util.KerberosName;

public class SASLAuthenticationProvider implements AuthenticationProvider {

    public String getScheme() {
        return "sasl";
    }

    @Override
    public <T> List<Id> authenticate(Class<T> klass, T conn, byte[] authData) throws KeeperException {
        // Should never call this: SASL authentication is negotiated at session initiation.
        // TODO: consider substituting current implementation of direct ClientCnxn manipulation with
        // a call to this method (SASLAuthenticationProvider:handleAuthentication()) at session initiation.
        throw KeeperException.create(KeeperException.Code.AUTHFAILED);

    }

    public boolean matches(String id, String aclExpr) {
        if ((id.equals("super") || id.equals(aclExpr))) {
            return true;
        }
        String readAccessUser = System.getProperty("zookeeper.letAnySaslUserDoX");
        return readAccessUser != null && aclExpr.equals(readAccessUser);
    }

    public boolean isAuthenticated() {
        return true;
    }

    public boolean isValid(String id) {
        // Since the SASL authenticator will usually be used with Kerberos authentication,
        // it should enforce that these names are valid according to Kerberos's
        // syntax for principals.
        //
        // Use the KerberosName(id) constructor to define validity:
        // if KerberosName(id) throws IllegalArgumentException, then id is invalid.
        // otherwise, it is valid.
        //
        try {
            new KerberosName(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
