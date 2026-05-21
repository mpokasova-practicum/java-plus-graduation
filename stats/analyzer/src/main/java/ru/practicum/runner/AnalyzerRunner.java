package ru.practicum.runner;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.processor.SimilarityProcessor;
import ru.practicum.processor.UserActionProcessor;

@Component
@RequiredArgsConstructor
public class AnalyzerRunner implements CommandLineRunner {
    private final UserActionProcessor userActionProcessor;
    private final SimilarityProcessor similarityProcessor;

    @Override
    public void run(String... args) throws Exception {
        Thread userActionThread = new Thread(userActionProcessor);
        userActionThread.setName("UserActionHandlerThread");
        userActionThread.start();

        similarityProcessor.start();
    }
}