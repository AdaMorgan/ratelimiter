package com.github.adamorgan.limiter.api.requests;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.http.server.HttpServerRequest;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public final class SequentialRestRateLimiter implements RestRateLimiter {
    private static final Logger log = LoggerFactory.getLogger(SequentialRestRateLimiter.class);
    private static final String UNINIT_BUCKET = "uninit";

    private final CompletableFuture<?> shutdownHandle = new CompletableFuture<>();
    private final RateLimitConfig config;
    private boolean isStopped, isShutdown;

    private final ReentrantLock lock = new ReentrantLock();
    private final Set<HttpServerRequest> hitRatelimit = new HashSet<>(5);

    public SequentialRestRateLimiter(@NotNull RateLimitConfig config) {
        this.config = config;
    }

    @Override
    public void enqueue(@NotNull RestRateLimiter.Work task) {

    }

    @Override
    public void stop(boolean shutdown, @NotNull Runnable callback) {

    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public int cancelRequests() {
        return 0;
    }

    public RateLimitConfig getConfig() {
        return config;
    }
}
