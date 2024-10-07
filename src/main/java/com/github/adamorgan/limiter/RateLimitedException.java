package com.github.adamorgan.limiter;

import org.jetbrains.annotations.NotNull;
import reactor.netty.http.server.HttpServerRequest;

/**
 * Indicates that we received a {@code 429: Too Many Requests} response
 */
public class RateLimitedException extends Exception {
    private final long retryAfter;
    private final boolean isGlobal;
    private final HttpServerRequest request;

    public RateLimitedException(@NotNull HttpServerRequest request, long retryAfter) {
        this(request, retryAfter, true);
    }

    public RateLimitedException(@NotNull HttpServerRequest request, long retryAfter, boolean isGlobal) {
        super(String.format("The request was ratelimited! Retry-After: %d  Route: %s", retryAfter, request.path()));
        this.request = request;
        this.retryAfter = retryAfter;
        this.isGlobal = isGlobal;
    }

    public RateLimitedException(@NotNull HttpServerRequest request, String message, long retryAfter, boolean isGlobal) {
        super(message);
        this.request = request;
        this.retryAfter = retryAfter;
        this.isGlobal = isGlobal;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    /**
     * The route responsible for the rate limit bucket that is used in
     * the responsible RateLimiter
     *
     * @return The corresponding route
     */
    public HttpServerRequest getRateLimitedRoute() {
        return this.request;
    }

    /**
     * The back-off delay in milliseconds that should be respected
     * before trying to query the {@link #getRateLimitedRoute() route} again
     *
     * @return The back-off delay in milliseconds
     */
    public long getRetryAfter() {
        return retryAfter;
    }
}