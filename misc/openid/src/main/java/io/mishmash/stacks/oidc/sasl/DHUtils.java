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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DHUtils {

    public static ByteBuffer encryptChallenge(
            final SecretKeySpec key,
            final byte[] response)
                    throws GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[16];
        SecureRandom r = new SecureRandom();
        r.nextBytes(iv);
        GCMParameterSpec pSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, pSpec);
        byte[] encrypted = cipher.doFinal(response);
        ByteBuffer resp = ByteBuffer.allocate(
                Integer.BYTES + iv.length + encrypted.length);
        resp.putInt(iv.length);
        resp.put(iv);
        resp.put(encrypted);

        return resp;
    }

    public static byte[] decryptChallenge(
            final SecretKeySpec key,
            final ByteBuffer challenge)
                    throws GeneralSecurityException, IOException {
        int paramsLen;
        if (challenge.remaining() <= Integer.BYTES
                || (paramsLen = challenge.getInt()) > 16 * 1024
                || challenge.remaining() <= paramsLen
                || challenge.remaining() > paramsLen + (16 * 1024)) {
            throw new IllegalArgumentException("Corrupt buffer received");
        }

        byte[] params = new byte[paramsLen];
        challenge.get(params);
        byte[] encrypted = new byte[challenge.remaining()];
        challenge.get(encrypted);

        GCMParameterSpec pSpec = new GCMParameterSpec(128, params);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, pSpec);

        return cipher.doFinal(encrypted);
    }
}
