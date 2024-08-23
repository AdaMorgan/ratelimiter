package com.github.adamorgan.limiter;

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

    private final Map<String, Bucket> buckets = new HashMap<>();
    private final Set<HttpServerRequest> hitRatelimit = new HashSet<>(5);

    public SequentialRestRateLimiter(@NotNull RateLimitConfig config) {
        this.config = config;
    }

    @Override
    public void enqueue(@NotNull RestRateLimiter.RateLimit task) {

    }

    @Override
    public void stop(boolean shutdown, @NotNull Runnable callback) {

    }

    @Override
    public boolean isStopped() {
        return isStopped;
    }

    @Override
    public int cancelRequests() {
        return 0;
    }

    public RateLimitConfig getConfig() {
        return config;
    }

    public abstract class Bucket implements Runnable {
        protected final String bucketId;

        protected Bucket(String bucketId) {
            this.bucketId = bucketId;
        }

        public abstract long getGlobalRateLimit(long now);

        @Override
        public void run() {

        }
    }

    private class ClassicBucket extends Bucket {
        private final @NotNull RestRateLimiter.GlobalRateLimit holder;

        protected ClassicBucket(String bucketId) {
            super(bucketId);
            this.holder = config.getGlobalRateLimit();
        }

        @Override
        public long getGlobalRateLimit(long now) {
            return Math.max(holder.getClassic(), holder.getCloudflare()) - now;
        }

        @Override
        public String toString() {
            return bucketId;
        }

        @Override
        public int hashCode() {
            return bucketId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (!(obj instanceof Bucket))
                return false;
            return this.bucketId.equals(((Bucket) obj).bucketId);
        }
    }

    private class InteractionBucket extends Bucket {

        protected InteractionBucket(String bucketId) {
            super(bucketId);
        }

        @Override
        public long getGlobalRateLimit(long now) {
            return config.getGlobalRateLimit().getCloudflare() - now;
        }
    }
}
