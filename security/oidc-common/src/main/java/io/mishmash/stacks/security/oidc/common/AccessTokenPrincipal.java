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

import java.security.Principal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public abstract class AccessTokenPrincipal implements Principal {

    public abstract Optional<Boolean> getBooleanClaim(String claimName);
    public abstract Optional<Date> getDateClaim(String claimName);
    public abstract Optional<Double> getDoubleClaim(String claimName);
    public abstract Optional<Long> getLongClaim(String claimName);
    public abstract Optional<String> getStringClaim(String claimName);
    public abstract Optional<Collection<String>>
            getStringListClaim(String claimName);

    public Optional<String> getAudience() {
        return getStringClaim(ClaimsUtil.CLAIM_AUDIENCE);
    }

    public Optional<String> getSubject() {
        return getStringClaim(ClaimsUtil.CLAIM_SUBJECT);
    }

    public Optional<String> getIssuer() {
        return getStringClaim(ClaimsUtil.CLAIM_ISSUER);
    }

    @Override
    public String getName() {
        return getSubject().orElse(null);
    }

    public Optional<Collection<String>> getGroups() {
        return Optional.empty();
    }

    public Optional<Collection<String>> getRoles() {
        return Optional.empty();
    }

    public Optional<Collection<String>> getScopesCollection() {
        return getStringClaim(ClaimsUtil.CLAIM_SCOPES)
                .map(s -> s.split("\\s+"))
                .map(strs -> List.of(strs));
    }
}
