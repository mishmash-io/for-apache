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

package io.mishmash.stacks.oidc.login;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class OIDCClientLoginModule implements LoginModule {

    private Subject subject;
    private OIDCClientPrincipal oidc;
    private boolean selfLoginSuccess = false;

    @Override
    public void initialize(
            final Subject forSubject,
            final CallbackHandler callbackHandler,
            final Map<String, ?> sharedState,
            final Map<String, ?> options) {
        this.subject = forSubject;
        this.oidc = OIDCClientPrincipal.configure(options);
    }

    @Override
    public boolean login() throws LoginException {
        oidc.loginSelf();

        selfLoginSuccess = true;

        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        if (selfLoginSuccess) {
            subject.getPrincipals().add(oidc);
        } else {
            destroyState();
        }

        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        if (selfLoginSuccess) {
            destroyState();
        }

        return false;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().remove(oidc);
        destroyState();

        return true;
    }

    protected void destroyState() {
        subject = null;

        if (oidc != null) {
            oidc.destroy();
            oidc = null;
        }
    }
}
