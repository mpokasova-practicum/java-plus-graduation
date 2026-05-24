package ru.practicum.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.processor.SimilarityProcessor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregatorRunner implements CommandLineRunner {
    private final SimilarityProcessor similarityProcessor;

    @Override
    public void run(String... args) {
        log.info("Запуск AggregatorRunner");
        similarityProcessor.start();
        log.info("AggregatorRunner завершил запуск");
    }
}
