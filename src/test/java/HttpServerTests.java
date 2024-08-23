import com.github.adamorgan.limiter.RestRateLimiter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;


public class HttpServerTests {
    public static final Logger LOG = LoggerFactory.getLogger("test");

    public static void main(String[] args) {
        HttpServer.create()
                .host("localhost")
                .port(8080)
                .handle((request, response) -> {
                    return Mono.empty();
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

    @Test
    public void run() {
        LOG.info("Hello JUnit!");
    }
}
