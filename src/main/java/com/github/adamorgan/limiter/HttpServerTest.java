package com.github.adamorgan.limiter;

import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;

public class HttpServerTest {
    public static void main(String[] args) {
        HttpServer.create()
                .host("localhost")
                .port(8080)
                .handle((request, response) -> {
                    return new GenericRateLimit(request, response).handle(false);
                })
                .route(routes -> {
                    routes.get("/ping", (request, response) -> {
                        return response.status(200).sendString(
                                Mono.just("pong")
                        );
                    });
                })
                .bindUntilJavaShutdown(Duration.ZERO, server -> {
                    LoggerFactory.getLogger("server").info("Finished Loading!");
                });
    }
}
