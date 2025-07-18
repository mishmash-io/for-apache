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

import javax.security.auth.callback.Callback;

public class ClientIdCallback implements Callback {

    private String clientId;
    private String defaultId;

    public ClientIdCallback() {
        this(null);
    }

    public ClientIdCallback(final String defaultVal) {
        defaultId = defaultVal;
    }

    public String getClientId() {
        return clientId;
    }

    public String getDefault() {
        return defaultId;
    }

    public void setClientId(final String newClientId) {
        this.clientId = newClientId;
    }
}
