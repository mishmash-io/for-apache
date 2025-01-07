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

import java.net.http.HttpRequest;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.util.DateUtils;

public abstract class RESTClientAuth extends RESTRenewer<SignedJWT> {

    private static final Logger LOG =
            Logger.getLogger(RESTClientAuth.class.getName());

    public RESTClientAuth(final RESTClient client) {
        super(client);
    }

    public HttpRequest.Builder authenticate(
            final HttpRequest.Builder request)
                    throws InterruptedException, ExecutionException {
        SignedJWT bearer = get();

        return request.header("Authorization",
                "Bearer " + bearer.serialize());
    }

    @Override
    protected boolean needsRefresh(final SignedJWT currentJWT) {
        try {
            Date expiry = currentJWT.getJWTClaimsSet().getExpirationTime();

            return expiry == null
                    ? false
                    : !DateUtils.isAfter(expiry, new Date(), 60);
        } catch (ParseException e) {
            LOG.log(Level.WARNING, "Malformed JWT, will try to update", e);

            return true;
        }
    }
}
