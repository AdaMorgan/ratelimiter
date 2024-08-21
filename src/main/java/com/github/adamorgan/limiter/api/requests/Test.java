package com.github.adamorgan.limiter.api.requests;

import org.omg.CORBA.TIMEOUT;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class Test {
    public static final int ERROR_CODE = 429;


    public static void main(String[] args) {
        // Создаем поток данных
        Flux<Integer> dataStream = Flux.range(1, 100)
                .delayElements(Duration.ofMillis(100)); // Эмулируем задержку между элементами

        // Ограничиваем скорость обработки
        Flux<Integer> rateLimitedStream = dataStream
                .window(Duration.ofSeconds(1)) // Создаем окна по 1 секунде
                .flatMap(window -> window.take(10)) // Ограничиваем 10 элементами в окне
                .subscribeOn(Schedulers.parallel()); // Обработка в параллельных потоках

        // Подписываемся на поток и выводим элементы
        rateLimitedStream.subscribe(
                item -> System.out.println("Processed: " + item),
                error -> System.err.println("Error: " + error),
                () -> System.out.println("Completed")
        );
    }
}
