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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RESTClient {

    public static final Duration DEFAULT_CONNECTION_TIMEOUT =
            Duration.of(2, ChronoUnit.SECONDS);
    public static final Duration DEFAULT_RESPONSE_TIMEOUT =
            Duration.of(2, ChronoUnit.SECONDS);

    private Duration connectTimeout;
    private Duration respondTimeout;

    public RESTClient() {
        this(DEFAULT_CONNECTION_TIMEOUT, DEFAULT_RESPONSE_TIMEOUT);
    }

    public RESTClient(
            final Duration connectionTimeout,
            final Duration responseTimeout) {
        this.connectTimeout = connectionTimeout;
        this.respondTimeout = responseTimeout;
    }

    public HttpClient.Builder client() {
        return HttpClient.newBuilder()
                .connectTimeout(connectTimeout);
    }

    public CompletableFuture<HttpResponse<JsonObject>>
            request(
                    final HttpRequest.Builder requestBuilder,
                    final HttpClient.Builder clientBuilder) {
        HttpRequest r = requestBuilder.build();
        HttpClient client = clientBuilder.build();

        return client.sendAsync(r,
                BodyHandlers.fromSubscriber(
                        BodySubscribers.ofString(StandardCharsets.UTF_8),
                        this::handleResponseString));
    }

    public CompletableFuture<HttpResponse<JsonObject>>
            request(final HttpRequest.Builder requestBuilder) {
        return request(requestBuilder, client());
    }

    public HttpRequest.Builder requestBuilder(final URI uri) {
        HttpRequest.Builder b = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json;charset=UTF-8")
                .timeout(respondTimeout);

        return b;
    }

    public HttpRequest.Builder get(final URI uri) {
        return requestBuilder(uri)
                .GET();
    }

    public HttpRequest.Builder postForm(
            final URI uri,
            final Collection<Map.Entry<String, String>> formData) {
        String postFormData = formData.stream()
                .map(e -> URLEncoder.encode(
                            e.getKey(),
                            StandardCharsets.UTF_8)
                        + (e.getValue() == null
                            ? ""
                            : "="
                                + URLEncoder.encode(
                                        e.getValue(),
                                        StandardCharsets.UTF_8)))
                .collect(Collectors.joining("&"));

        return post(uri, postFormData)
                .header(
                        "Content-Type",
                        "application/x-www-form-urlencoded");
    }

    public HttpRequest.Builder postJson(
            final URI uri,
            final Map<String, Object> json) {
        return post(uri, new Gson().toJson(json))
                .header(
                        "Content-Type",
                        "application/json;charset=UTF-8");
    }

    public HttpRequest.Builder post(
            final URI uri,
            final String body) {
        return requestBuilder(uri)
                .POST(BodyPublishers.ofString(body));
    }

    public CompletableFuture<JsonObject> getBodyOrFail(
            final HttpResponse<JsonObject> resp) {
        if (resp.statusCode() != 200) {
            return CompletableFuture.failedFuture(
                    new OIDCResponseException(resp));
        } else {
            return CompletableFuture.completedFuture(resp.body());
        }
    }

    protected JsonObject handleResponseString(
            final BodySubscriber<String> subscriber) {
        try {
            return subscriber.getBody()
                    .thenApply(JsonParser::parseString)
                    .thenApply(e -> e.getAsJsonObject())
                    .toCompletableFuture()
                    .get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
