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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Iterator;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.mishmash.stacks.oidc.login.OIDCClientPrincipal;
import io.mishmash.stacks.oidc.util.URIUtils;

public class OAUTHBearerServer implements SaslServer {

    private boolean isComplete = false;
    private OIDCClientPrincipal oidc;
    private String name;
    private SignedJWT jwt;

    public OAUTHBearerServer(
            final OIDCClientPrincipal oidcClient,
            final String serverName) {
        this.oidc = oidcClient;
        this.name = serverName;
    }

    @Override
    public String getMechanismName() {
        return OAUTHBearerProvider.MECHANISM;
    }

    @Override
    public byte[] evaluateResponse(final byte[] response)
            throws SaslException {
        try {
            HdrsIterator it = new HdrsIterator(response);

            while (it.hasNext()) {
                ByteBuffer hdr = it.next();
                String hdrStr = StandardCharsets.UTF_8.decode(hdr).toString();

                if (hdrStr.startsWith("auth=Bearer ")) {
                    SignedJWT receivedJWT = SignedJWT.parse(
                            hdrStr.substring("auth=Bearer ".length()));

                    if (!oidc.verify(receivedJWT)) {
                        throw new SaslException(
                                "Received an unauthorized JWT token");
                    } else {
                        isComplete = true;
                        jwt = receivedJWT;
                    }
                } else if (hdrStr.startsWith("host=")) {
                    // ignore, will deduce from jwt token
                } else {
                    // ignore, will deduce from jwt token
                }
            }

            return null;
        } catch (ParseException p) {
            throw new SaslException("Received invalid JWT from client");
        }
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public String getAuthorizationID() {
        try {
            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            boolean isUMA = oidc.isUMA()
                    && claims.getClaim("authorization") != null;
            String azp = claims.getClaimAsString("azp");
            String issuer = URIUtils.getIssuer(
                    new URI(claims.getIssuer())).get();

            return String.format("%s://%s@%s",
                    isUMA ? "uma2" : "oidc",
                    azp,
                    issuer);
        } catch (ParseException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
        jwt = null;
    }

    private class HdrsIterator implements Iterator<ByteBuffer> {

        private ByteBuffer buf;

        private HdrsIterator(final byte[] bytes) {
            buf = ByteBuffer.wrap(bytes);
        }

        @Override
        public boolean hasNext() {
            while (buf.hasRemaining()) {
                byte b = buf.get();

                if (b == 0x01) {
                    // two consecutive 0x01s signal the end
                    return buf.position() > 1;
                }
            }

            // did not find the end, ignore bytes
            return false;
        }

        @Override
        public ByteBuffer next() {
            ByteBuffer next = buf.slice();
            ByteBuffer res = buf.flip();

            buf = next;

            // remove the final delimitier
            return res.limit(res.limit() - 1);
        }
    }
}
