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
import jakarta.servlet.http.HttpServletRequest;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Id;

public class HttpSASLAuthenticationProvider extends SASLAuthenticationProvider {

    @Override
    public <T> List<Id> authenticate(Class<T> klass, T conn, byte[] authData) throws KeeperException {
        if (HttpServletRequest.class.equals(klass)) {
            return List.of();
        } else {
            return super.authenticate(klass, conn, authData);
        }
    }

}
