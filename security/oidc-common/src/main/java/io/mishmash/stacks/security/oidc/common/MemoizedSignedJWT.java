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

import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.function.Failable;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.JWTClaimsSetVerifier;
import com.nimbusds.jwt.util.DateUtils;

import io.mishmash.stacks.common.MemoizableAction;

public abstract class MemoizedSignedJWT extends MemoizableAction<SignedJWT> {

    private static final Logger LOG =
            Logger.getLogger(MemoizedSignedJWT.class.getName());

    private SignedJWT jwt;
    private JWTClaimsSetVerifier<? extends SecurityContext> verifier;

    public MemoizedSignedJWT() {
        // use an empty verifier -> not recommended
        this((s, c) -> {});
    }

    public MemoizedSignedJWT(
            final JWTClaimsSetVerifier<? extends SecurityContext>
                    claimsVerifier) {
        this.verifier = claimsVerifier;
    }

    protected void verify(final SignedJWT jwt)
            throws BadJWTException, ParseException {
        verifier.verify(jwt.getJWTClaimsSet(), null);
    }

    public void verify() throws BadJWTException, ParseException {
        verify(uncheckedGet());
    }

    @Override
    protected void memoize(SignedJWT item) {
        this.jwt = item;
    }

    @Override
    protected boolean hasMemoized() {
        if (jwt == null) {
            return false;
        }

        try {
            Date exp = jwt.getJWTClaimsSet().getExpirationTime();
            return exp == null || DateUtils
                    .isBefore(exp, DateUtils.nowWithSecondsPrecision(), 0);
        } catch (ParseException e) {
            LOG.log(Level.WARNING, """
                    Failed to parse access token 'expires' claim, \
                    will request new token""",
                    e);

            return false;
        }
    }

    @Override
    protected SignedJWT getMemoized() {
        return jwt;
    }

    public Optional<Boolean> getBooleanClaim(final String claimName) {
        return Optional.ofNullable(uncheckedGet())
                .map(Failable.asFunction(SignedJWT::getJWTClaimsSet))
                .map(Failable
                        .asFunction(set -> set.getBooleanClaim(claimName)));
    }

    public Optional<Date> getDateClaim(final String claimName) {
        return Optional.ofNullable(uncheckedGet())
                .map(Failable.asFunction(SignedJWT::getJWTClaimsSet))
                .map(Failable
                        .asFunction(set -> set.getDateClaim(claimName)));
    }

    public Optional<Double> getDoubleClaim(final String claimName) {
        return Optional.ofNullable(uncheckedGet())
                .map(Failable.asFunction(SignedJWT::getJWTClaimsSet))
                .map(Failable
                        .asFunction(set -> set.getDoubleClaim(claimName)));
    }

    public Optional<Long> getLongClaim(final String claimName) {
        return Optional.ofNullable(uncheckedGet())
                .map(Failable.asFunction(SignedJWT::getJWTClaimsSet))
                .map(Failable
                        .asFunction(set -> set.getLongClaim(claimName)));
    }

    public Optional<String> getStringClaim(final String claimName) {
        return Optional.ofNullable(uncheckedGet())
                .map(Failable.asFunction(SignedJWT::getJWTClaimsSet))
                .map(Failable
                        .asFunction(set -> set.getStringClaim(claimName)));
    }

    public Optional<Collection<String>> getStringListClaim(
            final String claimName) {
        return Optional.ofNullable(uncheckedGet())
                .map(Failable.asFunction(SignedJWT::getJWTClaimsSet))
                .map(Failable
                        .asFunction(set -> set.getStringListClaim(claimName)));
    }
}
