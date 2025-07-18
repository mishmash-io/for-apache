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
import org.apache.zookeeper.server.ServerCnxn;

/**
 * This interface is implemented by authentication providers to add new kinds of
 * authentication schemes to ZooKeeper.
 */
public interface AuthenticationProvider {

    /**
     * The String used to represent this provider. This will correspond to the
     * scheme field of an Id.
     *
     * @return the scheme of this provider.
     */
    String getScheme();

    /**
     * Authenticate a given object and optional authentication data.
     *
     * This method attempts to authenticate (typically) a connection object
     * (such as a server connection object or a HTTP request) and returns
     * the associated Id's that were extracted.
     *
     * All the specifics required by a particular access implementation
     * (a server connection object or a HTTP request) are also done by this
     * method.
     *
     * @param <T> the type of access method
     * @param klass the class of the access method
     * @param conn an instance of the access method
     * @param authData some additional data, if it was available
     * @return a list of extracted identities, may be empty
     * @throws KeeperException if authentication fails
     * @throws UnsupportedOperationException if the requested access method is not
     * supported
     */
    public <T> List<Id> authenticate(Class<T> klass, T conn, byte[] authData) throws KeeperException;

    /**
     * This method is called when a client passes authentication data for this
     * scheme. The authData is directly from the authentication packet. The
     * implementor may attach new ids to the authInfo field of cnxn or may use
     * cnxn to send packets back to the client.
     *
     * @param cnxn
     *                the cnxn that received the authentication information.
     * @param authData
     *                the authentication data received.
     * @return TODO
     */
    default KeeperException.Code handleAuthentication(ServerCnxn cnxn, byte[] authData) {
        try {
            authenticate(ServerCnxn.class, cnxn, authData);
            return KeeperException.Code.OK;
        } catch (KeeperException e) {
            return e.code();
        }
    }

    /**
     * This method is called to see if the given id matches the given id
     * expression in the ACL. This allows schemes to use application specific
     * wild cards.
     *
     * @param id
     *                the id to check.
     * @param aclExpr
     *                the expression to match ids against.
     * @return true if the id can be matched by the expression.
     */
    boolean matches(String id, String aclExpr);

    /**
     * This method is used to check if the authentication done by this provider
     * should be used to identify the creator of a node. Some ids such as hosts
     * and ip addresses are rather transient and in general don't really
     * identify a client even though sometimes they do.
     *
     * @return true if this provider identifies creators.
     */
    boolean isAuthenticated();

    /**
     * Validates the syntax of an id.
     *
     * @param id
     *                the id to validate.
     * @return true if id is well formed.
     */
    boolean isValid(String id);

    /**
     * <param>id</param> represents the authentication info which is set in server connection.
     * id may contain both user name as well as password.
     * This method should be implemented to extract the user name.
     *
     * @param id authentication info set by client.
     * @return String user name
     */
    default String getUserName(String id) {
        // Most of the authentication providers id contains only user name.
        return id;
    }

}
