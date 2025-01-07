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
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonObject;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.util.DateUtils;

public class Issuer {

    private static final Logger LOG = Logger.getLogger(Issuer.class.getName());

    public static final String ENDPOINT_TOKEN = "token_endpoint";
    public static final String ENDPOINT_INTROSPECTION =
            "introspection_endpoint";
    public static final String ENDPOINT_USERINFO = "userinfo_endpoint";
    public static final String ENDPOINT_JWKS = "jwks_uri";

    public static final String KEY_ISSUER = "issuer";

    public static final String DEFAULT_PATH_OIDC_CONFIG =
            "/.well-known/openid-configuration";
    public static final String DEFAULT_PATH_UMA2_CONFIG =
            "/.well-known/uma2-configuration";

    private RESTClient rest;
    private Optional<JsonObject> oidcConfig;
    private Optional<JsonObject> uma2Config;
    private Throwable oidcConfigFailureCause;
    private Throwable uma2ConfigFailureCause;
    private Map<String, String> staticConfig = new HashMap<>();
    private RESTSignatures signatures;

    public Issuer(final RESTClient client) {
        this.rest = client;
    }

    public void autoConfigureOIDC(final URI oidcConfigurationUri) {
        try {
            rest.request(rest.get(oidcConfigurationUri))
                    .thenCompose(rest::getBodyOrFail)
                    .thenAccept(this::oidcConfigSuccess)
                    .exceptionally(this::oidcConfigFailure)
                    .get();
        } catch (Exception e) {
            oidcConfigFailure(e);
        }
    }

    public void autoConfigureUMA2(final URI uma2ConfigurationUri) {
        try {
            rest.request(rest.get(uma2ConfigurationUri))
                    .thenCompose(rest::getBodyOrFail)
                    .thenAccept(this::uma2ConfigSuccess)
                    .exceptionally(this::uma2ConfigFailure)
                    .get();
        } catch (Exception e) {
            uma2ConfigFailure(e);
        }
    }

    public void autoConfigureOIDC() {
        Objects.requireNonNull(
                getIssuer(),
                "The base issuer URI must be configured");

        try {
            autoConfigureOIDC(
                new URI(getIssuer() + DEFAULT_PATH_OIDC_CONFIG));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unparsable issuer URI", e);
        }
    }

    public void autoConfigureUMA2() {
        Objects.requireNonNull(
                getIssuer(),
                "The base issuer URI must be configured");

        try {
            autoConfigureUMA2(
                new URI(getIssuer() + DEFAULT_PATH_UMA2_CONFIG));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unparsable issuer URI", e);
        }
    }

    public void initSigntures() {
        signatures = new RESTSignatures(
                rest,
                getJWKSEndpoint().get());
    }

    public void setEndpoint(
            final String endpointName,
            final URI endpointUri) {
        staticConfig.put(endpointName, endpointUri.toString());
    }

    public void setTokenEndpoint(final URI endpointUri) {
        setEndpoint(ENDPOINT_TOKEN, endpointUri);
    }

    public void setIntrospectionEndpoint(final URI endpointUri) {
        setEndpoint(ENDPOINT_INTROSPECTION, endpointUri);
    }

    public void setUserInfoEndpoint(final URI endpointUri) {
        setEndpoint(ENDPOINT_USERINFO, endpointUri);
    }

    public void setJWKSEndpoint(final URI endpointUri) {
        setEndpoint(ENDPOINT_JWKS, endpointUri);
    }

    public void setIssuer(final String issuer) {
        staticConfig.put(KEY_ISSUER, issuer);
    }

    public Optional<String> getConfigString(final String key) {
        return Optional
                .ofNullable(staticConfig.get(key))
                .or(() -> getConfigString(uma2Config, key))
                .or(() -> getConfigString(oidcConfig, key));
    }

    public Optional<URI> getEndpoint(final String endpointName) {
        return getConfigString(endpointName)
                .map(s -> {
                    try {
                        return new URI(s);
                    } catch (URISyntaxException e) {
                        LOG.log(Level.WARNING,
                                "Ignoring malformed endpoint "
                                    + endpointName, e);
                        return null;
                    }
                });
    }

    public Optional<URI> getTokenEndpoint() {
        return getEndpoint(ENDPOINT_TOKEN);
    }

    public Optional<URI> getIntrospectionEndpoint() {
        return getEndpoint(ENDPOINT_INTROSPECTION);
    }

    public Optional<URI> getUserInfoEndpoint() {
        return getEndpoint(ENDPOINT_USERINFO);
    }

    public Optional<URI> getJWKSEndpoint() {
        return getEndpoint(ENDPOINT_JWKS);
    }

    public String getIssuer() {
        return getConfigString(KEY_ISSUER)
                .orElse(null);
    }

    public URI getIssuerURI() {
        return getConfigString(KEY_ISSUER)
                .map(u -> {
                    try {
                        return new URI(u);
                    } catch (URISyntaxException e) {
                        LOG.log(Level.WARNING,
                                "Malformed issuer URI: " + u);
                        return null;
                    }
                })
                .orElse(null);
    }

    public Throwable getOIDCConfigFailureCause() {
        return oidcConfigFailureCause;
    }

    public Throwable getUMA2ConfigFailureCause() {
        return uma2ConfigFailureCause;
    }

    public boolean verifyIssuer(final SignedJWT jwt) {
        try {
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (claims == null) {
                return false;
            }
    
            String claimedIssuer = claims.getIssuer();
            if (claimedIssuer == null) {
                return false;
            }
    
            String thisIssuer = getIssuer();
            if (thisIssuer == null) {
                LOG.log(Level.SEVERE, """
                        OIDC Issuer URL not configured, \
                        Will fail JWT token verification.""");
    
                return false;
            }
    
            if (!thisIssuer.equals(claimedIssuer)) {
                return false;
            }
    
            Date currentTime = DateUtils.nowWithSecondsPrecision();
            Date issuedAt = claims.getIssueTime();
            if (issuedAt == null
                    || DateUtils.isAfter(issuedAt, currentTime, 0)) {
                return false;
            }

            return signatures.verify(jwt);
        } catch (ParseException e) {
            // malformed token
            return false;
        } catch (ExecutionException | InterruptedException ee) {
            LOG.log(Level.SEVERE, """
                    Could not get OIDC Issuer JWKS, \
                    Will fail JWT token verification.""");

            return false;
        }
    }

    protected Optional<String> getConfigString(
            final Optional<JsonObject> config,
            final String key) {
        return config
                .map(j -> j.get(key))
                .map(e -> e.getAsString());
    }

    protected void oidcConfigSuccess(
            final JsonObject resp) {
        oidcConfig = Optional.ofNullable(resp);
        oidcConfigFailureCause = null;
    }

    protected Void oidcConfigFailure(final Throwable cause) {
        oidcConfig = Optional.empty();
        oidcConfigFailureCause = cause;

        return null;
    }

    protected void uma2ConfigSuccess(
            final JsonObject resp) {
        uma2Config = Optional.ofNullable(resp);
        uma2ConfigFailureCause = null;
    }

    protected Void uma2ConfigFailure(final Throwable cause) {
        uma2Config = Optional.empty();
        uma2ConfigFailureCause = cause;

        return null;
    }
}
