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

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;
import javax.security.sasl.SaslException;

import io.mishmash.stacks.oidc.login.OIDCClientPrincipal;

public class OAUTHBearerClientDH extends OAUTHBearerClient {

    private int keyLength;
    private KeyPairGenerator keyPairGenerator;
    private KeyPair keyPair;
    private KeyAgreement agreement;
    private boolean keyExchangeComplete = false;
    private PublicKey serverPublicKey;
    private SecretKeySpec secretKey;

    public OAUTHBearerClientDH(
            final OIDCClientPrincipal oidcClient,
            final String authzId,
            final String serverName,
            final int keyLen) throws SaslException {
        super(oidcClient, authzId, serverName);

        this.keyLength = keyLen;

        try {
            keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyPairGenerator.initialize(keyLength);
            keyPair = keyPairGenerator.generateKeyPair();
            agreement = KeyAgreement.getInstance("DH");
            agreement.init(keyPair.getPrivate());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new SaslException(
                    getMechanismName()
                        + " SASL client initialization error");
        }
    }

    @Override
    public byte[] evaluateChallenge(final byte[] challenge)
            throws SaslException {
        if (challenge == null) {
            throw new SaslException(
                    getMechanismName() + " authentication failed");
        }

        if (keyExchangeComplete) {
            // decrypt, pass to parent and encrypt response
            return encrypt(super.evaluateChallenge(decrypt(challenge)));
        } else {
            if (serverPublicKey != null) {
                throw new SaslException(
                        getMechanismName()
                        + " SASL client unexpected challenge from server");
            }

            if (challenge.length == 0) {
                // begin DH exchange, send public key
                return keyPair.getPublic().getEncoded();
            } else {
                // received server public key
                try {
                    KeyFactory factory = KeyFactory.getInstance("DH");
                    X509EncodedKeySpec keySpec =
                            new X509EncodedKeySpec(challenge);
                    serverPublicKey = factory.generatePublic(keySpec);
                    agreement.doPhase(serverPublicKey, true);
                    byte[] sharedSecret = agreement.generateSecret();
                    secretKey = new SecretKeySpec(
                            sharedSecret, 0, 16, "AES");
                    keyExchangeComplete = true;

                    // get the initial response from parent
                    return encrypt(super.evaluateChallenge(new byte[0]));
                } catch (NoSuchAlgorithmException
                        | InvalidKeySpecException
                        | InvalidKeyException
                        | IllegalStateException e) {
                    throw new SaslException(
                            getMechanismName()
                            + " SASL client failed during key exchange");
                }
            }
        }
    }

    @Override
    public String getMechanismName() {
        return super.getMechanismName() + "-DH" + keyLength;
    }

    @Override
    public void dispose() throws SaslException {
        keyLength = 0;
        keyPairGenerator = null;
        keyPair = null;
        agreement = null;
        keyExchangeComplete = false;
        serverPublicKey = null;
        secretKey = null;

        super.dispose();
    }

    protected byte[] encrypt(final byte[] challenge) throws SaslException {
        if (challenge == null || challenge.length == 0) {
            return challenge;
        }

        try {
            return DHUtils
                    .encryptChallenge(secretKey, challenge)
                    .array();
        } catch (Exception e) {
            throw new SaslException(
                    getMechanismName()
                    + " SASL client failed to encrypt");
        }
    }

    protected byte[] decrypt(final byte[] challenge)
            throws SaslException {
        if (challenge == null || challenge.length == 0) {
            return challenge;
        }

        try {
            return DHUtils
                    .decryptChallenge(secretKey, ByteBuffer.wrap(challenge));
        } catch (Exception e) {
            throw new SaslException(
                    getMechanismName()
                    + " SASL client failed to encrypt");
        }
    }
}
