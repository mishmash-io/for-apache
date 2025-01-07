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

package io.mishmash.stacks.oidc.login;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.text.ParseException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.security.auth.login.LoginException;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;

import io.mishmash.stacks.oidc.protocol.Issuer;
import io.mishmash.stacks.oidc.protocol.RESTClient;
import io.mishmash.stacks.oidc.protocol.RESTClientAuth;
import io.mishmash.stacks.oidc.protocol.RESTClientCredentialsAuth;
import io.mishmash.stacks.oidc.util.URIUtils;

public class OIDCClientPrincipal implements Principal {

    private static final Logger LOG = Logger.getLogger(OIDCClientPrincipal.class.getName());

    public static final String OPT_WHO_AM_I = "whoAmI";
    public static final String OPT_ISSUER = "issuer";
    public static final String OPT_CLIENT_ID = "clientId";
    public static final String OPT_CLIENT_SECRET = "clientSecret";
    public static final String OPT_VERIFICATION_MODE = "verifyTokensBy";
    public static final String OPT_VERIFICATION_SIGNATURE = "signature";
    public static final String OPT_VERIFICATION_INTROSPECTION = "introspection";
    public static final String OPT_ENDPOINT_OIDC_DISCOVERY =
            "endpoint.oidc.discovery";
    public static final String OPT_ENDPOINT_UMA2_DISCOVERY =
            "endpoint.uma2.discovery";
    public static final String OPT_ENDPOINT_TOKEN = "endpoint.token";
    public static final String OPT_ENDPOINT_INTROSPECT = "endpoint.introspection";
    public static final String OPT_ENDPOINT_JWKS = "endpoint.jwks";
    public static final String OPT_ENDPOINT_USERINFO = "endpoint.userinfo";

    private RESTClient rest;
    private RESTClientAuth restAuth;
    private Issuer issuer;
    private Collection<Map.Entry<String, String>> myAuth;

    private boolean shouldIntrospect = true;
    private boolean isUMA = false;

    public void loginSelf() throws LoginException {
        validateConfig();

        restAuth = new RESTClientCredentialsAuth(rest, issuer, myAuth);

        // try to log in by creating a dummy request
        try {
            restAuth.authenticate(
                    rest.get(issuer.getTokenEndpoint().get()));
        } catch (Exception e) {
            LoginException le = new LoginException("OIDC client login failed");

            le.initCause(e);

            throw le;
        }
    }

    public boolean verify(final SignedJWT jwt) {
        if (!issuer.verifyIssuer(jwt)) {
            return false;
        }

        DefaultJWTClaimsVerifier<SecurityContext> claimsVerifier =
                new DefaultJWTClaimsVerifier<>(
                        getClientId().orElse(null),
                        new JWTClaimsSet.Builder()
                            .claim("typ", "Bearer")
                            .build(),
                        Set.of("exp", "azp"));

        try {
            claimsVerifier.verify(jwt.getJWTClaimsSet(), null);

            return true;
        } catch (BadJWTException | ParseException e) {
            return false;
        }
    }

    public SignedJWT requestAccess(
            final URI from,
            final URI to) {
        List<Map.Entry<String, String>> ticketOpts = new ArrayList<>();

        if (URIUtils.isUMA(to) && isUMA()) {
            ticketOpts.add(Map.entry(
                    "grant_type",
                    "urn:ietf:params:oauth:grant-type:uma-ticket"));
        } else {
            ticketOpts.add(Map.entry(
                    "grant_type",
                    "client_credentials"));
        }

        ticketOpts.addAll(requestAccessFromOpts(from));
        ticketOpts.addAll(requestAccessToOpts(to));

        try {
            return rest.request(
                    restAuth.authenticate(
                            rest.postForm(
                                    issuer.getTokenEndpoint().get(),
                                    ticketOpts)))
                .thenCompose(rest::getBodyOrFail)
                .thenApply(j -> {
                    try {
                        return SignedJWT.parse(
                                j.get("access_token").getAsString());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void destroy() {
        myAuth = null;
        rest = null;
        restAuth = null;
        issuer = null;
    }

    @Override
    public String getName() {
        URI issuerURI = issuer.getIssuerURI();

        return String.format("%s://%s@%s",
                isUMA() ? "uma2" : "oidc",
                getClientId().orElse(""),
                URIUtils.getIssuer(issuerURI).get());
    }

    public boolean isUMA() {
        return isUMA;
    }

    protected Collection<Map.Entry<String, String>>
            requestAccessFromOpts(final URI from) {
        if (URIUtils.isOIDC(from) || URIUtils.isUMA(from)) {
            if (getClientId().orElse("a")
                        .equals(URIUtils.getClientId(from).orElse("b"))
                    && URIUtils.getIssuer(issuer.getIssuerURI()).orElse("a")
                        .equals(URIUtils.getIssuer(from).orElse("b"))) {
                // it's for us
                return List.of();
            } else {
                // TODO:
                throw new UnsupportedOperationException(
                        "Unsupported OIDC identity scheme: " + from.getScheme());
            }
        } else if (URIUtils.isJWT(from)) {
            // TODO:
            throw new UnsupportedOperationException(
                    "Unsupported OIDC identity scheme: " + from.getScheme());
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported OIDC identity scheme: " + from.getScheme());
        }
    }

    protected Collection<Map.Entry<String, String>>
            requestAccessToOpts(final URI to) {
        List<Map.Entry<String, String>> res = new ArrayList<>();

        if (URIUtils.isOIDC(to) || URIUtils.isUMA(to)) {
            res.add(Map.entry(
                    "audience",
                    URIUtils
                        .getClientId(to)
                        .or(() -> URIUtils.getParamLast(to, "audience"))
                        .orElseThrow(() ->
                            new NoSuchElementException(
                                    "Missing OIDC target audience"))));

            res.add(Map.entry("scope",
                    URIUtils.getParamLast(to, "scope")
                        .orElse("openid roles")));

            if (URIUtils.isUMA(to)) {
                if (!isUMA()) {
                    LOG.log(Level.WARNING,
                            """
                            Target for OIDC access ticket is UMA, but this \
                            client is not configured for UMA, ignoring \
                            potential permissions settings on the target.""");
                } else {
                    res.addAll(URIUtils.getParamsStream(to)
                            .filter(e -> e.getKey().equals("permission"))
                            .collect(Collectors.toList()));
                }
            }

            return res;
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported OIDC identity scheme: " + to.getScheme());
        }
    }

    protected Optional<String> findAuth(final String key) {
        return myAuth.stream()
                .filter(e -> e.getKey().equals(key))
                .findAny()
                .map(e -> e.getValue());
    }

    protected Optional<String> getClientId() {
        return findAuth("client_id");
    }

    protected Optional<String> getClientSecret() {
        return findAuth("client_secret");
    }

    protected void validateConfig() throws LoginException {
        if (issuer == null) {
            throw new LoginException(
                String.format(
                    """
                    OIDC client requires a valid issuer. \
                    Set the {} or {} config options; or enable auto-discovery \
                    through {}""",
                    OPT_WHO_AM_I, OPT_ISSUER, OPT_ENDPOINT_OIDC_DISCOVERY));
        }

        Optional<String> clientId = getClientId();
        Optional<String> clientSecret = getClientSecret();
        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            throw new LoginException(
                        """
                        OIDC client requires valid client credentials. \
                        Set the correct configuration options.""");
        }

        if (issuer.getTokenEndpoint().isEmpty()) {
            throw new LoginException(
                String.format(
                    """
                    OIDC client requires a valid token endpoint URL. \
                    Set the {} or {} config options; or enable auto-discovery \
                    through {}""",
                    OPT_WHO_AM_I,
                    OPT_ENDPOINT_TOKEN,
                    OPT_ENDPOINT_OIDC_DISCOVERY));
        }

        if (issuer.getJWKSEndpoint().isEmpty()) {
            throw new LoginException(
                    String.format(
                        """
                        The OIDC client requires a valid JWKS endpoint URL, \
                        but one was not found. \
                        Set the {} config option, or enable auto-discovery \
                        through {} and make sure the issuer supports it.""",
                        OPT_ENDPOINT_JWKS,
                        OPT_ENDPOINT_OIDC_DISCOVERY));
        } else {
            issuer.initSigntures();
        }

        if (shouldIntrospect && issuer.getIntrospectionEndpoint().isEmpty()) {
            throw new LoginException(
                    String.format(
                        """
                        JWT verification mode is {}, but no introspection \
                        endpoint is provided in config. Set the {} option \
                        to a valid URL or enable auto-discovery through {}""",
                        OPT_VERIFICATION_INTROSPECTION,
                        OPT_ENDPOINT_INTROSPECT,
                        OPT_ENDPOINT_OIDC_DISCOVERY));
        }
    }

    public static OIDCClientPrincipal configure(final Map<String, ?> opts) {
        OIDCClientPrincipal client = new OIDCClientPrincipal();
        RESTClient rest = new RESTClient();
        client.rest = rest;
        String optName = null;

        try {
            String issuerCandidate = null;
            String clientIdCandidate = null;
            String clientSecretCandidate = null;
            String clientGrantTypeCandidate = null;
            String scopeCandidate = null;
            List<Map.Entry<String, String>> clientAuthParams =
                    new ArrayList<>();

            optName = OPT_WHO_AM_I;
            if (opts.containsKey(optName)) {
                // parse the provided 'who am I' uri and extract config
                URI me = getURIOpt(opts, optName);
                Optional<String> issuerOpt = URIUtils.getIssuer(me);

                if (issuerOpt.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Issuer not set in " + optName);
                }

                // set a potential issuer
                issuerCandidate = "https://" + URIUtils.getIssuer(me).get();

                // check the scheme
                if (URIUtils.isOIDC(me)) {
                    // valid, nothing to do
                } else if (URIUtils.isUMA(me)) {
                    // set UMA2
                    client.isUMA = true;
                } else {
                    throw new UnsupportedOperationException(
                            "Unsupported URI scheme " + me.getScheme());
                }

                clientIdCandidate = URIUtils.getClientId(me).orElse(null);
                clientSecretCandidate = URIUtils
                        .getClientSecret(me)
                        .orElse(null);
                clientGrantTypeCandidate = URIUtils
                        .getParamLast(me, "grant_type")
                        .orElse("client_credentials");
                scopeCandidate = URIUtils
                        .getParamLast(me, "scope")
                        .orElse("openid");

                // process other oidc parameters we might need to authenticate
                List<String> ignoreParams = List.of(
                        "client_id", "client_secret", "grant_type", "scope");
                clientAuthParams.addAll(URIUtils.getParamsStream(me)
                        .filter(p -> !ignoreParams.contains(p.getKey()))
                        .collect(Collectors.toList()));
            }

            // configure the issuer
            Issuer issuer = new Issuer(rest);
            if (opts.containsKey(OPT_ISSUER)) {
                optName = OPT_ISSUER;
                issuer.setIssuer((String) opts.get(optName));
            } else if (issuerCandidate != null) {
                optName = OPT_WHO_AM_I;
                issuer.setIssuer(issuerCandidate);
            }

            optName = OPT_ENDPOINT_OIDC_DISCOVERY;
            if (opts.containsKey(optName)) {
                issuer.autoConfigureOIDC(getURIOpt(opts, optName));
            } else {
                issuer.autoConfigureOIDC();
            }

            if (issuer.getOIDCConfigFailureCause() != null) {
                LOG.log(Level.WARNING,
                        """
                        Could not fetch OIDC configuration from \
                        discovery endpoint. \
                        This may result in improper configuration. \
                        (Original error was: """
                        + issuer.getOIDCConfigFailureCause().getMessage()
                        + ")");
            }

            optName = OPT_ENDPOINT_UMA2_DISCOVERY;
            if (opts.containsKey(optName)) {
                client.isUMA = true;
                issuer.autoConfigureUMA2(getURIOpt(opts, optName));
            } else if (client.isUMA) {
                issuer.autoConfigureUMA2();
            }

            if (client.isUMA && issuer.getUMA2ConfigFailureCause() != null) {
                LOG.log(Level.WARNING,
                        """
                        Could not fetch UMA2 configuration from \
                        discovery endpoint. \
                        This may result in improper configuration. \
                        (Original error was: """
                        + issuer.getUMA2ConfigFailureCause().getMessage()
                        + ")");       
            }

            // Overwrite issuer settings
            optName = OPT_ENDPOINT_TOKEN;
            if (opts.containsKey(optName)) {
                issuer.setTokenEndpoint(getURIOpt(opts, optName));
            }

            optName = OPT_ENDPOINT_INTROSPECT;
            if (opts.containsKey(optName)) {
                issuer.setIntrospectionEndpoint(getURIOpt(opts, optName));
            }

            optName = OPT_ENDPOINT_JWKS;
            if (opts.containsKey(optName)) {
                issuer.setJWKSEndpoint(getURIOpt(opts, optName));
            }

            optName = OPT_ENDPOINT_USERINFO;
            if (opts.containsKey(optName)) {
                issuer.setUserInfoEndpoint(getURIOpt(opts, optName));
            }

            client.issuer = issuer;

            // configure our client id
            optName = OPT_CLIENT_ID;
            clientIdCandidate = Optional
                    .ofNullable((String) opts.get(optName))
                    .orElse(clientIdCandidate);
            clientAuthParams.add(
                    new SimpleEntry<>("client_id", clientIdCandidate));

            // configure our client secret
            optName = OPT_CLIENT_SECRET;
            clientSecretCandidate = Optional
                    .ofNullable((String) opts.get(optName))
                    .orElse(clientSecretCandidate);
            clientAuthParams.add(
                    new SimpleEntry<>("client_secret", clientSecretCandidate));

            // configure our grant type
            clientAuthParams.add(
                    new SimpleEntry<>("grant_type", clientGrantTypeCandidate));

            // configure the scope
            clientAuthParams.add(
                    new SimpleEntry<>("scope", scopeCandidate));

            client.myAuth = clientAuthParams;

            /*
             * Configure how we verify tokens
             */
            optName = OPT_VERIFICATION_MODE;
            Optional.ofNullable(opts.get(optName))
                .map(o -> (String) o)
                .ifPresent(s -> {
                    if (OPT_VERIFICATION_SIGNATURE.equalsIgnoreCase(s)) {
                        client.shouldIntrospect = false;
                    } else if (OPT_VERIFICATION_INTROSPECTION
                            .equalsIgnoreCase(s)) {
                        client.shouldIntrospect = true;
                    } else {
                        LOG.log(Level.WARNING,
                            String.format(
                                """
                                Incorrect value for {} config option. \
                                Should be one of {} or {}. Using default.""",
                                OPT_VERIFICATION_MODE,
                                OPT_VERIFICATION_SIGNATURE,
                                OPT_VERIFICATION_INTROSPECTION));
                    }
                });
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to process OIDC client config option "
                    + optName + ", due to: "
                    + e.getClass().getSimpleName()
                    + ". Check the option syntax and correctness.");
        }

        return client;
    }

    private static URI getURIOpt(
            final Map<String, ?> opts,
            final String optName) throws URISyntaxException {
        if (opts.containsKey(optName)) {
            return new URI((String) opts.get(optName));
        } else {
            return null;
        }
    }
}
