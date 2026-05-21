package ru.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Instant;

@Slf4j
@Component
public class SimilarityMapper {

    public EventSimilarityAvro toAvro(Long eventA, Long eventB, double score) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return EventSimilarityAvro.newBuilder()
                .setEventA(first)
                .setEventB(second)
                .setScore(score)
                .setTimestamp(Instant.now())
                .build();
    }
}
