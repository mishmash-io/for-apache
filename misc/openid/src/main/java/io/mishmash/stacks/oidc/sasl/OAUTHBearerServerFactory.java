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

package io.mishmash.stacks.oidc.sasl;

import java.security.AccessController;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;

import io.mishmash.stacks.oidc.login.OIDCClientPrincipal;

public class OAUTHBearerServerFactory implements SaslServerFactory {

    private static final Logger LOG =
            Logger.getLogger(OAUTHBearerServerFactory.class.getName());

    @Override
    public SaslServer createSaslServer(
            final String mechanism,
            final String protocol,
            final String serverName,
            final Map<String, ?> props,
            final CallbackHandler cbh) throws SaslException {
        LOG.log(Level.FINER, () ->
                "Creating OAUTHBEARER SASL Server -"
                    + " mechanism: " + mechanism
                    + " protocol: " + protocol
                    + " server name: " + serverName
                    + " props: " + props);

        // FIXME: update for Java 21
        Subject subject = Subject.getSubject(AccessController.getContext());

        if (subject == null) {
            throw new SaslException(
                    "Could not determine Subject for sasl client");
        }

        OIDCClientPrincipal client = subject
                            .getPrincipals(OIDCClientPrincipal.class)
                                .stream()
                                .findFirst()
                                .orElse(null);
        if (client == null) {
            throw new SaslException(
                    "Could not find an OIDC client");
        }

        return new OAUTHBearerServer(client, serverName);
    }

    @Override
    public String[] getMechanismNames(final Map<String, ?> props) {
        return new String[] {OAUTHBearerProvider.MECHANISM};
    }
}
