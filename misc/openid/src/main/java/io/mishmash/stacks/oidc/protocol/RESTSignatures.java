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

package io.mishmash.stacks.oidc.protocol;

import java.net.URI;
import java.security.Key;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.util.DateUtils;

public class RESTSignatures extends RESTRenewer<JWKSet> {

    private static final Logger LOG =
            Logger.getLogger(RESTSignatures.class.getName());

    private URI jwks;

    public RESTSignatures(
            final RESTClient client,
            final URI jwksUri) {
        super(client);
        jwks = jwksUri;
    }

    public boolean verify(final SignedJWT jwt)
            throws InterruptedException, ExecutionException {
        JWSHeader hdr = jwt.getHeader();
        JWSAlgorithm alg = hdr.getAlgorithm();
        String keyId = hdr.getKeyID();

        if (keyId == null) {
            return false;
        }

        JWK signingKey = get().getKeyByKeyId(keyId);
        if (signingKey == null) {
            return false;
        }

        try {
            Key key;

            if (MACVerifier.SUPPORTED_ALGORITHMS.contains(alg)) {
                key = signingKey.toOctetSequenceKey().toSecretKey();
            } else if (RSASSAVerifier.SUPPORTED_ALGORITHMS.contains(alg)) {
                key = signingKey.toRSAKey().toPublicKey();
            } else if (ECDSAVerifier.SUPPORTED_ALGORITHMS.contains(alg)) {
                key = signingKey.toECKey().toPublicKey();
            } else {
                LOG.log(Level.SEVERE,
                        """
                        Unsupported JWT signing algorithm, token \
                        verification will fail.
                        """);

                return false;
            }
    
            JWSVerifier verifier = new DefaultJWSVerifierFactory()
                    .createJWSVerifier(hdr, key);
            return jwt.verify(verifier);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Got exception during JWT verification", e);

            return false;
        }
    }

    @Override
    protected CompletableFuture<JWKSet> requestNew(
            final JWKSet current) {
        CompletableFuture<JWKSet> res = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                res.complete(
                        JWKSet.load(jwks.toURL())
                            .filter(new JWKMatcher.Builder()
                                    .keyUse(KeyUse.SIGNATURE)
                                    .build()));
            } catch (Exception e) {
                res.completeExceptionally(e);
            }
        });

        return res;
    }

    @Override
    protected boolean needsRefresh(final JWKSet current) {
        if (current == null || current.isEmpty()) {
            return true;
        }

        for (JWK k : current.getKeys()) {
            if (k.getExpirationTime() == null
                    || DateUtils.isBefore(
                            k.getExpirationTime(),
                            DateUtils.nowWithSecondsPrecision(),
                            0)) {
                continue;
            }

            return true;
        }

        return false;
    }
}
