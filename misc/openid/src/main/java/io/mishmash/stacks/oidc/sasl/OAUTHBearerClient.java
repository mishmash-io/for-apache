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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import com.nimbusds.jwt.SignedJWT;

import io.mishmash.stacks.oidc.login.OIDCClientPrincipal;

public class OAUTHBearerClient implements SaslClient {

    private boolean isComplete = false;
    private OIDCClientPrincipal oidc;
    private String authz;
    private String server;

    public OAUTHBearerClient(
            final OIDCClientPrincipal oidcClient,
            final String authzId,
            final String serverName) {
        this.oidc = oidcClient;
        this.authz = authzId;
        this.server = serverName;
    }

    @Override
    public String getMechanismName() {
        return OAUTHBearerProvider.MECHANISM;
    }

    @Override
    public boolean hasInitialResponse() {
        return true;
    }

    @Override
    public byte[] evaluateChallenge(final byte[] challenge)
            throws SaslException {
        if (challenge.length == 0) {
            // initial response, send ticket
            try {
                SignedJWT jwt = oidc.requestAccess(
                        new URI(authz),
                        new URI(server));

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                writeGSHeader(baos);
                writeHost(baos);
                writeAuth(baos, jwt);
                // write a final delimiter
                baos.write(0x01);

                // FIXME:
                isComplete = true;
                return baos.toByteArray();
            } catch (Exception e) {
                throw new SaslException(
                        "Could not send auth info to server", e);
            }
        }

        return null;
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public byte[] unwrap(
            final byte[] incoming,
            final int offset,
            final int len) throws SaslException {
        throw new IllegalStateException("Sasl integrity and privacy are not supported");
    }

    @Override
    public byte[] wrap(
            final byte[] outgoing,
            final int offset,
            final int len) throws SaslException {
        throw new IllegalStateException("Sasl integrity and privacy are not supported");
    }

    @Override
    public Object getNegotiatedProperty(final String propName) {
        if (Sasl.QOP.equals(propName)) {
            return "auth";
        }

        return null;
    }

    @Override
    public void dispose() throws SaslException {
        isComplete = false;
        oidc = null;
        authz = null;
        server = null;
    }

    protected void writeGSHeader(final OutputStream os) throws IOException {
        // write GS2 channel-binding flag
        os.write(new String("n,").getBytes(StandardCharsets.UTF_8));

        if (authz != null) {
            // write the GS2 authz id
            os.write(
                    new String("a=" + authz + ",")
                        .getBytes(StandardCharsets.UTF_8));
        }

        // write the delimiter
        os.write(0x01);
    }

    protected void writeHost(final OutputStream os) throws IOException {
        if (server != null) {
            // write the server name
            os.write(new String("host=" + server)
                    .getBytes(StandardCharsets.UTF_8));
            // ...and a delimiter
            os.write(0x01);
        }
    }

    protected void writeAuth(
            final OutputStream os,
            final SignedJWT jwt) throws IOException {
        os.write(new String("auth=Bearer " + jwt.serialize())
                .getBytes(StandardCharsets.UTF_8));
        os.write(0x01);
    }
}
