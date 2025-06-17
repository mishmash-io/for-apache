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

package io.mishmash.stacks.azure.osgi;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Component;

import io.mishmash.gen.openapi.azure.imds.ApiException;
import io.mishmash.gen.openapi.azure.imds.client.api.DefaultApi;
import io.mishmash.gen.openapi.azure.imds.client.model.ScheduledEventsDocument;
import io.mishmash.stacks.azure.utils.AzureConstants;
import io.mishmash.stacks.common.SoftRefMemoizableAction;

@Component(service={MemoizedIMDSScheduledEvents.class}, immediate=true)
public class MemoizedIMDSScheduledEvents
        extends SoftRefMemoizableAction<ScheduledEventsDocument> {

    private static final Logger LOG = Logger.getLogger(
            MemoizedIMDSScheduledEvents.class.getName());

    private static final Duration DEFAULT_MAX_AGE =
            Duration.of(30, ChronoUnit.SECONDS);

    public MemoizedIMDSScheduledEvents() {
        super(DEFAULT_MAX_AGE);
    }

    @Override
    protected CompletableFuture<ScheduledEventsDocument> prepareAction() {
        try {
            return new DefaultApi()
                    .scheduledEventsGetEvents(
                            AzureConstants.IMDS_VER_SCHEDULED_EVENTS,
                            AzureConstants.IMDS_PARAM_META)
                    .orTimeout(2, TimeUnit.SECONDS)
                    .whenComplete(this::postRunAction);
        } catch (ApiException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    protected void postRunAction(
            final ScheduledEventsDocument d,
            final Throwable t) {
        if (t != null) {
            LOG.log(Level.WARNING,
                    """
                    Failed to get Azure scheduled events service, \
                    might not be able to act on platform restarts, \
                    evictions, etc.""",
                    t);
        }
    }
}
