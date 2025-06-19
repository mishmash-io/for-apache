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

package io.mishmash.stacks.compute.azure.osgi;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.resources.models.Subscriptions;
import com.azure.resourcemanager.resources.models.Tenants;

import io.mishmash.stacks.common.SoftRefMemoizableAction;

@Component(service={AzureResource.class}, immediate=true)
public class AzureResource extends AzureAPIBase {

    /*
     * Refresh our ResourceManager instance once in a while,
     * as the info it uses to authenticate might change (like
     * the tenant id, for example). See parent class for details.
     */
    private static final Duration MAX_AGE_RESOURCE =
            Duration.of(30, ChronoUnit.SECONDS);

    private SoftRefMemoizableAction<ResourceManager.Authenticated> resourceMemo
        = new SoftRefMemoizableAction<
                ResourceManager.Authenticated>(MAX_AGE_RESOURCE) {
            @Override
            protected CompletableFuture<ResourceManager.Authenticated>
                    prepareAction() {
                return awaitAzureProfile()
                        .thenApply(p ->
                                ResourceManager.authenticate(
                                        getDefaultCredential(p),
                                        p));
            }
        };

    @Activate
    public AzureResource(
            @Reference final MemoizedIMDSInstance instance,
            @Reference final MemoizedIMDSIdentityInfo identity) {
        super(instance, identity);
    }

    protected Optional<ResourceManager.Authenticated> safeGetRef() {
        return Optional.ofNullable(resourceMemo.uncheckedGet());
    }

    public Optional<Subscriptions> getSubscriptions() {
        return safeGetRef()
                .map(ResourceManager.Authenticated::subscriptions);
    }

    public Optional<Tenants> getTenants() {
        return safeGetRef()
                .map(ResourceManager.Authenticated::tenants);
    }

    public ResourceManager getResourceManager() {
        return safeGetRef()
                .map(ResourceManager.Authenticated::withDefaultSubscription)
                .orElse(null);
    }

    public ResourceManager getResourceManager(
            final String subscriptionId) {
        return safeGetRef()
                .map(a -> a.withSubscription(subscriptionId))
                .orElse(null);
    }

    public Optional<ResourceGroup> getResrouceGroupByName(final String name) {
        return Optional.ofNullable(getResourceManager())
                .map(ResourceManager::resourceGroups)
                .map(g -> g.getByName(name));
    }
}
