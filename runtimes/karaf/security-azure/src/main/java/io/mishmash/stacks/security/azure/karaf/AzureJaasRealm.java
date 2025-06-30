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

package io.mishmash.stacks.security.azure.karaf;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.azure.core.management.AzureEnvironment;

import io.mishmash.stacks.compute.common.ComputeProvider;
import io.mishmash.stacks.security.azure.AzureLoginModule;
import io.mishmash.stacks.security.karaf.AccessTokenToKarafLoginModule;

@Component(
        service={JaasRealm.class},
        configurationPid= {AzureJaasRealm.AZURE_JAAS_REALM_PID},
        immediate=true)
public class AzureJaasRealm implements JaasRealm {

    public static final String AZURE_JAAS_REALM_PID = "azureJaas";

    @Reference
    private ComputeProvider computeProvider;

    private AppConfigurationEntry[] configEntries =
            new AppConfigurationEntry[0];

    @Activate
    private void activate(final Map<String, Object> props) {
        configure(props);
    }

    @Modified
    private void modified(final Map<String, Object> props) {
        configure(props);
    }

    protected void configure(final Map<String, Object> props) {
        Map<String, Object> azureOpts = new HashMap<>();
        azureOpts.put(ProxyLoginModule.PROPERTY_MODULE,
                AzureLoginModule.class.getName());
        azureOpts.put(ProxyLoginModule.PROPERTY_BUNDLE,
                String.valueOf(FrameworkUtil
                        .getBundle(AzureLoginModule.class)
                        .getBundleId()));
        // first, configure some defaults
        switch (computeProvider.getName()) {
        case "AzureChinaCLoud":
            azureOpts.put(AzureLoginModule.OPT_ENTRA_ID_ENDPOINT,
                    AzureEnvironment.AZURE_CHINA.getActiveDirectoryEndpoint());
            azureOpts.put(AzureLoginModule.OPT_TARGET_RESOURCE,
                    AzureEnvironment.AZURE_CHINA.getResourceManagerEndpoint());
            break;
        case "AzureUSGovernmentCloud":
            azureOpts.put(AzureLoginModule.OPT_ENTRA_ID_ENDPOINT,
                    AzureEnvironment.AZURE_US_GOVERNMENT
                        .getActiveDirectoryEndpoint());
            azureOpts.put(AzureLoginModule.OPT_TARGET_RESOURCE,
                    AzureEnvironment.AZURE_US_GOVERNMENT
                        .getResourceManagerEndpoint());
            break;
        case "AzurePublicCloud":
        default:
            azureOpts.put(AzureLoginModule.OPT_ENTRA_ID_ENDPOINT,
                    AzureEnvironment.AZURE.getActiveDirectoryEndpoint());
            azureOpts.put(AzureLoginModule.OPT_TARGET_RESOURCE,
                    AzureEnvironment.AZURE.getResourceManagerEndpoint());
        }
        if (props.containsKey(AzureLoginModule.OPT_DISABLE_CALLBACKS)) {
            azureOpts.put(AzureLoginModule.OPT_DISABLE_CALLBACKS,
                    props.get(AzureLoginModule.OPT_DISABLE_CALLBACKS));
        }
        if (props.containsKey(AzureLoginModule.OPT_ENTRA_ID_ENDPOINT)) {
            // overwrite default:
            azureOpts.put(AzureLoginModule.OPT_ENTRA_ID_ENDPOINT,
                    props.get(AzureLoginModule.OPT_ENTRA_ID_ENDPOINT));
        }
        if (props.containsKey(AzureLoginModule.OPT_TENANT_ID)) {
            azureOpts.put(AzureLoginModule.OPT_TENANT_ID,
                    props.get(AzureLoginModule.OPT_TENANT_ID));
        }
        if (props.containsKey(AzureLoginModule.OPT_MANAGED_ID_CLIENT_ID)) {
            azureOpts.put(AzureLoginModule.OPT_MANAGED_ID_CLIENT_ID,
                    props.get(AzureLoginModule.OPT_MANAGED_ID_CLIENT_ID));
        }
        if (props.containsKey(AzureLoginModule.OPT_TARGET_RESOURCE)) {
            // overwrite default:
            azureOpts.put(AzureLoginModule.OPT_TARGET_RESOURCE,
                    props.get(AzureLoginModule.OPT_TARGET_RESOURCE));
        }

        configEntries = new AppConfigurationEntry[] {
                // get an access token from Azure
                new AppConfigurationEntry(
                        ProxyLoginModule.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUISITE,
                        azureOpts),
                // then extract karaf principals from the token
                new AppConfigurationEntry(
                        ProxyLoginModule.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        Map.of(
                                ProxyLoginModule.PROPERTY_MODULE,
                                AccessTokenToKarafLoginModule.class.getName(),
                                ProxyLoginModule.PROPERTY_BUNDLE,
                                String.valueOf(
                                    FrameworkUtil
                                        .getBundle(
                                            AccessTokenToKarafLoginModule
                                                .class)
                                        .getBundleId())))
        };
    }

    @Override
    public String getName() {
        return "Azure";
    }

    @Override
    public int getRank() {
        return 0;
    }

    @Override
    public AppConfigurationEntry[] getEntries() {
        return this.configEntries;
    }
}
