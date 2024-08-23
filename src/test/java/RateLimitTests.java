import com.github.adamorgan.limiter.ErrorResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class RateLimitTests {
    public final static Map<Long, Integer> map = new HashMap<>();
    public static final long DISCORD_EPOCH = 1420070400000L;
    public static final long TIMESTAMP_OFFSET = 22;

    public static synchronized long getTimestamp(long millisTimestamp) {
        return (millisTimestamp - DISCORD_EPOCH) << TIMESTAMP_OFFSET;
    }

    public static synchronized long getTimestamp() {
        return getTimestamp(System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000));
    }


    @Test
    public void getResponse() {
        String json = ErrorResponse.GLOBAL_RATE_LIMIT.toJson(3030);
        System.out.println(json);
    }

    @Test
    public void run() throws InterruptedException {
        long id = getTimestamp();
        Flux.range(1, 100)
                .window(Duration.ofMillis(1000))
                .flatMap(window -> window.take(10))
                .subscribeOn(Schedulers.parallel())
                .doOnNext(System.out::println)
                .last()
                .block();
    }
}
