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

import java.security.Provider;

import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslServerFactory;

public class OAUTHBearerProvider extends Provider {

    private static final long serialVersionUID = -8986843382844219582L;

    public static final String MECHANISM = "OAUTHBEARER";

    public OAUTHBearerProvider() {
        super(MECHANISM, "1.0", "OIDC/UMA2 Sasl Provider");

        put(SaslClientFactory.class.getSimpleName() + "." + MECHANISM,
                OAUTHBearerClientFactory.class.getName());
        put(SaslServerFactory.class.getSimpleName() + "." + MECHANISM,
                OAUTHBearerServerFactory.class.getName());        
        put(SaslClientFactory.class.getSimpleName()
                + "." + MECHANISM + "-DH4096",
                OAUTHBearerClientFactory.class.getName());
        put(SaslServerFactory.class.getSimpleName()
                + "." + MECHANISM + "-DH4096",
                OAUTHBearerServerFactory.class.getName());        
    }
}
