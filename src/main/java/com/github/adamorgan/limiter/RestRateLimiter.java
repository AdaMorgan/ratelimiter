package com.github.adamorgan.limiter;

import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Interface used to handle requests to the Reactor Netty.
 * <p>Requests are handed to the rate-limiter via {@link #enqueue(RateLimit)} and executed using {@link RateLimit#execute()}.
 * The rate-limiter is responsible to ensure that requests do not exceed the rate-limit set by {@link RateLimitConfig}.
 */
public interface RestRateLimiter {
    /** The number of seconds to wait before submitting another request */
    String RETRY_AFTER_HEADER = "Retry-After";
    /** The number of requests that can be made */
    String LIMIT_HEADER = "X-RateLimit-Limit";
    /** The number of remaining requests that can be made */
    String REMAINING_HEADER = "X-RateLimit-Remaining";
    /** Epoch time (seconds since 00:00:00 UTC on January 1, 1970) at which the rate limit resets */
    String RESET_HEADER = "X-RateLimit-Reset";
    /** Total time (in seconds) of when the current rate limit bucket will reset. Can have decimals to match previous millisecond ratelimit precision */
    String RESET_AFTER_HEADER = "X-RateLimit-Reset-After";
    /** A unique string denoting the rate limit being encountered (non-inclusive of top-level resources in the path) */
    String HASH_HEADER = "X-RateLimit-Bucket";
    /** Returned only on HTTP 429 responses if the rate limit encountered is the global rate limit (not per-route) */
    String GLOBAL_HEADER = "X-RateLimit-Global";
    /** Returned only on HTTP 429 responses. Value can be user (per bot or user limit), global (per bot or user global limit), or shared (per resource limit) */
    String SCOPE_HEADER = "X-RateLimit-Scope";

    /**
     * Enqueue a new request.
     *
     * <p>Use {@link RateLimit#getRoute()} to determine the correct bucket.
     *
     * @param task
     *        The {@link RateLimit} to enqueue
     */
    void enqueue(@NotNull RestRateLimiter.RateLimit task);

    /**
     * Indication to stop accepting new reqzuests.
     *
     * @param shutdown
     *        Whether to also cancel previously queued request
     * @param callback
     *        Function to call once all requests are completed, used for final cleanup
     */
    void stop(boolean shutdown, @NotNull Runnable callback);

    /**
     * Whether the queue has stopped accepting new requests.
     *
     * @return True, if the queue is stopped
     */
    boolean isStopped();

    /**
     * Cancel all currently queued requests, which are not marked as {@link RateLimit#isPriority() priority}.
     *
     * @return The number of cancelled requests
     */
    int cancelRequests();

    /**
     * Type representing a pending request.
     *
     * <p>Use {@link #execute()} to run the request (on the calling thread) and {@link #isDone()} to discard it once completed.
     */
    interface RateLimit {
        /**
         * The {@link HttpServerRequest request} of the request.
         * <br>This is primarily used to handle rate-limit buckets.
         *
         * <p>To correctly handle rate-limits, it is recommended to use the {@link #HASH_HEADER bucket hash} header from the response.
         *
         * @return The {@link HttpServerRequest request}
         */
        @NotNull
        HttpServerRequest getRoute();

        /**
         * Executes the request on the calling thread (blocking).
         * <br>This might return null when the request has been skipped while executing.
         * Retries for certain response codes are already handled by this method.
         *
         * <p>After completion, it is advised to use {@link #isDone()} to check whether the request should be retried.
         *
         * @return {@link HttpServerResponse} instance, used to update the rate-limit data
         */
        @NonBlocking
        HttpServerResponse execute();

        /**
         * Whether the request should be skipped.
         * <br>This can be caused by user cancellation.
         *
         * <p>The rate-limiter should handle by simply discarding the task without further action.
         *
         * @return True, if this request is skipped
         */
        boolean isSkipped();

        /**
         * Whether the request is completed.
         * <br>This means you should not try using {@link #execute()} again.
         *
         * @return True, if the request has completed.
         */
        boolean isDone();

        /**
         * Requests marked as priority should not be cancelled.
         *
         * @return True, if this request is marked as priority
         */
        boolean isPriority();

        /**
         * Whether this request was cancelled.
         * <br>Similar to {@link #isSkipped()}, but only checks cancellation.
         *
         * @return True, if this request was cancelled
         */
        boolean isCancelled();

        /**
         * Used to execute a Request. Processes request related to provided bucket.
         *
         * @param  handleOnRateLimit
         *         Whether to forward rate-limits, false if rate limit handling should take over
         *
         * @return Non-null if the request was ratelimited. Returns a Long containing retry_after milliseconds until
         *         the request can be made again. This could either be for the Per-Route ratelimit or the Global ratelimit.
         */
        Mono<Void> handle(boolean handleOnRateLimit);

        /**
         * Cancel the request.
         * <br>Primarily used for {@link SequentialRestRateLimiter#cancelRequests()}.
         */
        Mono<Void> cancel();
    }

    /**
     * Global rate-limit store.
     * <br>This can be used to share the global rate-limit information between multiple instances.
     */
    interface GlobalRateLimit {
        /**
         * The current global rate-limit reset time.
         * <br>This is the rate-limit applied on the bot token.
         *
         * @return The timestamp when the global rate-limit expires (unix timestamp in milliseconds)
         */
        long getClassic();

        /**
         * Set the current global rate-limit reset time.
         * <br>This is the rate-limit applied on the bot token.
         *
         * @param timestamp
         *        The timestamp when the global rate-limit expires (unix timestamp in milliseconds)
         */
        void setClassic(long timestamp);

        /**
         * The current cloudflare rate-limit reset time.
         * <br>This is the rate-limit applied on the current IP.
         *
         * @return The timestamp when the cloudflare rate-limit expires (unix timestamp in milliseconds)
         */
        long getCloudflare();

        /**
         * Set the current cloudflare rate-limit reset time.
         * <br>This is the rate-limit applied on the current IP.
         *
         * @param timestamp
         *        The timestamp when the cloudflare rate-limit expires (unix timestamp in milliseconds)
         */
        void setCloudflare(long timestamp);

        /**
         * Creates a default instance of this interface.
         * <br>This uses {@link AtomicLong} to keep track of rate-limits.
         *
         * @return The default implementation
         */
        @NotNull
        static GlobalRateLimit create() {
            return new GlobalRateLimit() {
                private final AtomicLong classic = new AtomicLong(-1);
                private final AtomicLong cloudflare = new AtomicLong(-1);

                @Override
                public long getClassic() {
                    return classic.get();
                }

                @Override
                public void setClassic(long timestamp) {
                    classic.set(timestamp);
                }

                @Override
                public long getCloudflare() {
                    return cloudflare.get();
                }

                @Override
                public void setCloudflare(long timestamp) {
                    classic.set(timestamp);
                }
            };
        }
    }

    /**
     * Configuration for the rate-limiter.
     */
    class RateLimitConfig {
        private final ScheduledExecutorService scheduler;
        private final ExecutorService elastic;
        private final GlobalRateLimit globalRateLimit;
        private final boolean isRelative;

        public RateLimitConfig(@NotNull ScheduledExecutorService scheduler, @NotNull GlobalRateLimit globalRateLimit, boolean isRelative) {
            this(scheduler, scheduler, globalRateLimit, isRelative);
        }

        public RateLimitConfig(@NotNull ScheduledExecutorService scheduler, @NotNull ExecutorService elastic, @NotNull GlobalRateLimit globalRateLimit, boolean isRelative) {
            this.scheduler = scheduler;
            this.elastic = elastic;
            this.globalRateLimit = globalRateLimit;
            this.isRelative = isRelative;
        }

        /**
         * The {@link ScheduledExecutorService} used to schedule rate-limit tasks.
         *
         * @return The {@link ScheduledExecutorService}
         */
        @NotNull
        public ScheduledExecutorService getScheduler() {
            return scheduler;
        }

        /**
         * The elastic {@link ExecutorService} used to execute rate-limit tasks.
         * <br>This pool can potentially scale up and down depending on use.
         *
         * <p>It is also possible that this pool is identical to {@link #getScheduler()}.
         *
         * @return The elastic {@link ExecutorService}
         */
        @NotNull
        public ExecutorService getElastic() {
            return elastic;
        }

        /**
         * The global rate-limit store.
         *
         * @return The global rate-limit store
         */
        @NotNull
        public GlobalRateLimit getGlobalRateLimit() {
            return globalRateLimit;
        }

        /**
         * Whether to use {@link #RESET_AFTER_HEADER}.
         * <br>This is primarily to avoid NTP sync issues.
         *
         * @return True, if {@link #RESET_AFTER_HEADER} should be used instead of {@link #RESET_HEADER}
         */
        public boolean isRelative() {
            return isRelative;
        }
    }
}
