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

package io.mishmash.stacks.security.oidc.common;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public abstract class BaseLoginModule implements LoginModule {

    private static final Logger LOG =
            Logger.getLogger(BaseLoginModule.class.getName());

    private Subject subject;
    private CallbackHandler callback;
    private boolean isSuccess = false;

    protected abstract void configure(Map<String, ?> opts);
    protected abstract Callback[] prepareCallbacks();
    protected abstract void loginState() throws LoginException;
    protected abstract void loginSubject(Subject subject)
            throws LoginException;
    protected abstract void logoutSubject(Subject subject)
            throws LoginException;

    protected boolean isSuccessful() {
        return isSuccess;
    }

    protected void setSuccessful() {
        isSuccess = true;
    }

    protected void setUnsuccessful() {
        isSuccess = false;
    }

    protected void clearState() throws LoginException {
        setUnsuccessful();
        subject = null;
        callback = null;
    }

    @Override
    public boolean abort() throws LoginException {
        if (isSuccessful()) {
            clearState();
        }

        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        if (isSuccessful()) {
            loginSubject(subject);
        } else {
            clearState();
        }

        return true;
    }

    @Override
    public void initialize(
            final Subject forSubject,
            final CallbackHandler cbHandler,
            final Map<String, ?> sharedState,
            final Map<String, ?> options) {
        this.subject = forSubject;
        this.callback = cbHandler;

        configure(options);
    }

    @Override
    public boolean login() throws LoginException {
        if (callback != null) {
            Callback[] cbs = prepareCallbacks();

            /*
             * Do the callbacks one by one to allow
             * UnsupportedCallbackException to be handled.
             *
             * Existing code is probably unaware of extra Callbacks
             * that we're introducing (like the ClaimsCallback class)
             * and might throw an exception because of that.
             */
            for (Callback cb : cbs == null ? new Callback[0] : cbs) {
                try {
                    callback.handle(new Callback[] {cb});
                } catch (IOException e) {
                    LOG.log(Level.SEVERE,
                            "Failed to obtain authentication info",
                            e);

                    throw new LoginException(e.getMessage());
                } catch (UnsupportedCallbackException e) {
                    /*
                     * Log and continue, extending classes should
                     * be using callbacks with default values, or
                     * handle missing info in another way
                     */
                    LOG.log(Level.WARNING,
                            "LoginModule user (class "
                                + callback.getClass().getName()
                                + ") does not support auth info request ("
                                + cb.getClass().getName()
                                + "), ignoring exception");
                }
            }
        }

        loginState();

        return isSuccessful();
    }

    @Override
    public boolean logout() throws LoginException {
        logoutSubject(subject);

        return true;
    }
}
