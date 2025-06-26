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

package io.mishmash.stacks.security.azure;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;

import io.mishmash.stacks.security.oidc.common.BaseLoginModule;
import io.mishmash.stacks.security.oidc.common.ClaimsUtil;
import io.mishmash.stacks.security.oidc.common.ClientIdCallback;
import io.mishmash.stacks.security.oidc.common.MemoizedJWKSet;
import io.mishmash.stacks.security.oidc.common.MemoizedSignedJWT;
import io.mishmash.stacks.security.oidc.common.TenantIdClaimCallback;
import io.mishmash.stacks.security.oidc.common.AccessTokenPrincipal;
import io.mishmash.stacks.security.oidc.common.AdditionalClaimsCallback;
import io.mishmash.stacks.security.oidc.common.AudienceClaimCallback;

/*
 * https://learn.microsoft.com/en-us/entra/identity-platform/access-token-claims-reference
 */
public class AzureLoginModule extends BaseLoginModule {

    private static final Logger LOG =
            Logger.getLogger(AzureLoginModule.class.getName());

    public static final String OPT_DISABLE_CALLBACKS = "disableCallbacks";
    public static final String OPT_ENTRA_ID_ENDPOINT = "entraIdEndpoint";
    public static final String OPT_TENANT_ID = "tenantId";
    public static final String OPT_MANAGED_ID_CLIENT_ID =
            "managedIdentityClientId";
    public static final String OPT_TARGET_RESOURCE = "targetResource";

    private Map<String, Object> configuration = Map.of();
    private boolean disableCallbacks = false;
    private String entraIdEndpoint = null;
    private String tenantId = null;
    private String managedIdentityClientId = null;
    private String targetAppId = null;
    private String extraClaimsJSON = null;

    private MemoizedAccessToken accessToken;
    private MemoizedJWKSet jwks;

    private AudienceClaimCallback audience = new AudienceClaimCallback() {
        @Override
        public String getValue() {
            return targetAppId;
        }
        @Override
        public void setValue(final String v) {
            targetAppId = v;
        }
        @Override
        public String getDefault() {
            return (String) configuration
                    .getOrDefault(
                            OPT_TARGET_RESOURCE,
                            null);
        }
    };

    private TenantIdClaimCallback tid = new TenantIdClaimCallback() {
        @Override
        public String getValue() {
            return tenantId;
        }
        @Override
        public void setValue(final String v) {
            tenantId = v;
        }
        @Override
        public String getDefault() {
            return (String) configuration
                    .getOrDefault(
                            OPT_TENANT_ID,
                            null);
        }
    };

    private ClientIdCallback clientId = new ClientIdCallback() {
        @Override
        public String getClientId() {
            return managedIdentityClientId;
        }
        @Override
        public void setClientId(final String v) {
            managedIdentityClientId = v;
        }
        @Override
        public String getDefault() {
            return (String) configuration
                    .getOrDefault(
                            OPT_MANAGED_ID_CLIENT_ID,
                            null);
        }
    };

    private AdditionalClaimsCallback additionalClaims =
            new AdditionalClaimsCallback();

    @SuppressWarnings("unchecked")
    @Override
    protected void configure(Map<String, ?> opts) {
        configuration = (Map<String, Object>) opts;

        if (configuration.containsKey(OPT_DISABLE_CALLBACKS)) {
            disableCallbacks =
                    Boolean.valueOf(
                            (String) configuration.get(OPT_DISABLE_CALLBACKS));
        }

        entraIdEndpoint = (String) configuration.get(OPT_ENTRA_ID_ENDPOINT);
        audience.setValue(audience.getDefault());
        tid.setValue(tid.getDefault());
        clientId.setClientId(clientId.getDefault());
        additionalClaims.setClaims(additionalClaims.getDefaults());
    }

    @Override
    protected Callback[] prepareCallbacks() {
        return disableCallbacks
                ? new Callback[0]
                : new Callback[] {
                        audience,
                        // Could also be java's RealmClaim: 
                        tid,
                        clientId,
                        additionalClaims
                };
    }

    @Override
    protected void loginState() throws LoginException {
        try {
            /*
             *  initialize with the default Azure keys URL,
             *  make sure we verify against the actual Azure keys.
             */
            jwks = new MemoizedJWKSet(new URI(
                    entraIdEndpoint + "/common/discovery/keys").toURL());
        } catch (MalformedURLException | URISyntaxException e) {
            String reason = """
                    Failed to get Azure Public keys, login will not work \
                    without signature verification.""";

            LOG.log(Level.SEVERE, reason, e);
            throw new LoginException(reason);
        }

        accessToken = new MemoizedAccessToken();

        try {
            accessToken.get();
            setSuccessful();
        } catch (ExecutionException e) {
            setUnsuccessful();
            throw new LoginException(
                    e.getCause() == null
                        ? e.getMessage()
                        : e.getCause().getMessage());
        } catch (Exception e) {
            setUnsuccessful();
            throw new LoginException(e.getMessage());
        }
    }

    @Override
    protected void loginSubject(final Subject subject) throws LoginException {
        // use an internal class for simpler identification in logoutSubject()
        subject.getPrincipals().add(new AzurePrincipal(accessToken));
    }

    @Override
    protected void logoutSubject(final Subject subject) throws LoginException {
        subject.getPrincipals().removeIf(p -> p instanceof AzurePrincipal);
    }

    @Override
    protected void clearState() throws LoginException {
        accessToken = null;
        super.clearState();
    }

    protected AccessToken newAccessToken() {
        DefaultAzureCredentialBuilder credBuilder
                = new DefaultAzureCredentialBuilder();

        if (tenantId != null && !tenantId.isBlank()) {
            credBuilder = credBuilder.tenantId(tenantId);
        }

        if (managedIdentityClientId != null
                && !managedIdentityClientId.isBlank()) {
            credBuilder = credBuilder
                    .managedIdentityClientId(managedIdentityClientId);
        }

        TokenCredential cred = credBuilder.build();

        TokenRequestContext req = new TokenRequestContext()
                // must be only one:
                .addScopes(targetAppId);

        if (tenantId != null && !tenantId.isBlank()) {
            req = req.setTenantId(tenantId);
        }

        if (extraClaimsJSON != null && !extraClaimsJSON.isBlank()) {
            req = req.setClaims(extraClaimsJSON);
        }

        return cred.getTokenSync(req);
    }

    private class MemoizedAccessToken extends MemoizedSignedJWT {
        private MemoizedAccessToken() {
            super(new DefaultJWTClaimsVerifier<>(
                    targetAppId,
                    // TODO: check for other claims
                    new JWTClaimsSet.Builder().build(),
                    Set.of()));
        }

        @Override
        protected CompletableFuture<SignedJWT> prepareAction() {
            return CompletableFuture
                    .supplyAsync(AzureLoginModule.this::newAccessToken)
                    .thenApply(t -> {
                        try {
                            return SignedJWT.parse(t.getToken());
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .thenApply(jwt -> {
                        try {
                            if (!jwks.verify(jwt)) {
                                throw new BadJWTException(
                                        "Token failed signature verification");
                            }

                            verify(jwt);
                        } catch (BadJWTException | ParseException e) {
                            LOG.log(Level.WARNING, """
                                    Azure Token failed verification, \
                                    will renew""",
                                    e);
                            throw new RuntimeException(e);
                        }

                        return jwt;
                    });
        }
    }

    private class AzurePrincipal extends AccessTokenPrincipal {

        private MemoizedAccessToken token;

        private AzurePrincipal(
                final MemoizedAccessToken accessToken) {
            token = accessToken;
        }

        @Override
        public Optional<String> getStringClaim(final String claim) {
            return token.getStringClaim(claim);
        }

        @Override
        public Optional<Boolean> getBooleanClaim(final String claimName) {
            return token.getBooleanClaim(claimName);
        }

        @Override
        public Optional<Date> getDateClaim(final String claimName) {
            return token.getDateClaim(claimName);
        }

        @Override
        public Optional<Double> getDoubleClaim(final String claimName) {
            return token.getDoubleClaim(claimName);
        }

        @Override
        public Optional<Long> getLongClaim(final String claimName) {
            return token.getLongClaim(claimName);
        }

        @Override
        public Optional<Collection<String>> getStringListClaim(
                final String claimName) {
            return token.getStringListClaim(claimName);
        }

        @Override
        public Optional<Collection<String>> getGroups() {
            /*
             * FIXME: won't work if Azure replies with a 'hasgroups'
             * claim or, when there are too many groups Azure replies
             * with a 'groups' claim equal to 'src1'.
             * In both cases should actually follow a link to get the
             * actual list of groups.
             *
             * See Azure docs for more:
             * https://learn.microsoft.com/en-us/entra/identity-platform/access-token-claims-reference
             */
            return getStringListClaim("groups");
        }

        @Override
        public Optional<Collection<String>> getRoles() {
            return getStringListClaim(ClaimsUtil.CLAIM_ROLES);
        }
    }
}
