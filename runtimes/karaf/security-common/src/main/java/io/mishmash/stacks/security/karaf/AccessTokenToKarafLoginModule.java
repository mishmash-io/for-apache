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

package io.mishmash.stacks.security.karaf;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.karaf.jaas.boot.principal.ClientPrincipal;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;

import io.mishmash.stacks.security.oidc.common.AccessTokenPrincipal;

public class AccessTokenToKarafLoginModule implements LoginModule {

    private Subject subject;

    @Override
    public boolean abort() throws LoginException {
        subject = null;

        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        /*
         * find the access token which should've been added
         * by a previous login module:
         */
        Optional<AccessTokenPrincipal> principalOpt = subject
                .getPrincipals(AccessTokenPrincipal.class)
                .stream()
                .findAny();

        if (principalOpt.isEmpty()) {
            throw new LoginException("""
                            Preceding LoginModules did not register \
                            an AccessTokenPrincipal, cannot commit.""");
        }

        AccessTokenPrincipal principal = principalOpt.get();

        // add karaf principals by extracting info from the access token:
        principal.getSubject()
            .ifPresent(s -> subject.getPrincipals().add(new UserPrincipal(s)));
        principal.getRoles()
            .ifPresent(col -> {
                for (String role : col) {
                    subject.getPrincipals().add(new RolePrincipal(role));
                }
            });
        principal.getGroups()
            .ifPresent(col -> {
                for (String group : col) {
                    subject.getPrincipals().add(new GroupPrincipal(group));
                }
            });

        return true;
    }

    @Override
    public void initialize(
            final Subject subject,
            final CallbackHandler cbHandler,
            final Map<String, ?> sharedState,
            final Map<String, ?> opts) {
        this.subject = subject;
    }

    @Override
    public boolean login() throws LoginException {
        /*
         * always return true, we're not doing an actual login,
         * we're just adding karaf principals extracting them
         * from an access token
         */
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        // clear whatever all karaf principals from the subject
        HashSet<Principal> principals = new HashSet<>();

        principals.addAll(subject.getPrincipals(UserPrincipal.class));
        principals.addAll(subject.getPrincipals(GroupPrincipal.class));
        principals.addAll(subject.getPrincipals(RolePrincipal.class));
        principals.addAll(subject.getPrincipals(ClientPrincipal.class));

        subject.getPrincipals().removeAll(principals);
        subject = null;

        return true;
    }
}
