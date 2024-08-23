package com.github.adamorgan.limiter;

import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public class GenericRateLimit implements RestRateLimiter.RateLimit {
    private final HttpServerRequest request;
    private final HttpServerResponse response;
    private boolean done;

    public GenericRateLimit(HttpServerRequest request, HttpServerResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public @NotNull HttpServerRequest getRoute() {
        return this.request;
    }

    @Override
    public HttpServerResponse execute() {
        return this.response;
    }

    @Override
    public boolean isSkipped() {
        return false;
    }

    @Override
    public boolean isDone() {
        return isSkipped() || done;
    }

    @Override
    public boolean isPriority() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public Mono<Void> handle(boolean handleOnRateLimit) {
        return this.request.receive().then();
    }

    @Override
    public Mono<Void> cancel() {
        return Mono.empty();
    }

    public long retryAfter() {
        return 0;
    }
}
