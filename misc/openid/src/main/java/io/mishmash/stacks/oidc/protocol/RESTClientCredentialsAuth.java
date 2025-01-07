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

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

import com.nimbusds.jwt.SignedJWT;

public class RESTClientCredentialsAuth extends RESTClientAuth {

    private Issuer iss;
    private Collection<Map.Entry<String, String>> params;

    public RESTClientCredentialsAuth(
            final RESTClient client,
            final Issuer issuer,
            final Collection<Map.Entry<String, String>> authParams) {
        super(client);
        this.iss = issuer;
        this.params = authParams;
    }

    @Override
    protected CompletableFuture<SignedJWT> requestNew(
            final SignedJWT currentJWT) {
        RESTClient client = restClient();
        final CompletableFuture<SignedJWT> res = new CompletableFuture<>();

        try {
            client
                .request(
                    client.postForm(
                            iss.getTokenEndpoint().get(),
                            params))
                .thenCompose(client::getBodyOrFail)
                .thenAccept(j -> {
                    if (!j.has("access_token")) {
                        res.completeExceptionally(
                                new NoSuchElementException(
                                        "Did not receive JWT access token"));
                    }

                    try {
                        SignedJWT jwt = SignedJWT
                                .parse(j
                                        .get("access_token")
                                        .getAsString());

                        // make sure the issuer is right
                        if (iss.verifyIssuer(jwt)) {
                            res.complete(jwt);
                        } else {
                            throw new RuntimeException(
                                    "OIDC client logged into wrong Issuer.");
                        }
                    } catch (Exception e) {
                        res.completeExceptionally(e);
                    }
                })
                .exceptionally(t -> {
                    res.completeExceptionally(t);
                    return null;
                });
        } catch (Exception e) {
            res.completeExceptionally(e);
        }

        return res;
    }
}
