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

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;

public class AdditionalClaimsCallback implements Callback {

    private Map<String, Object> claims = null;
    private Map<String, Object> defaults = null;

    public AdditionalClaimsCallback() {
        this(null);
    }

    public AdditionalClaimsCallback(Map<String, Object> defaultVal) {
        defaults = defaultVal;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    public Map<String, Object> getDefaults() {
        return defaults;
    }

    public void setClaims(final Map<String, Object> newClaims) {
        this.claims = newClaims == null
                ? null
                : new HashMap<>(newClaims);
    }
}
