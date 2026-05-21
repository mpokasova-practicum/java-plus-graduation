package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.model.EventSimilarity;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class RecommendationsMapper {

    public Stream<RecommendedEventProto> mapToProto(
            List<EventSimilarity> eventSimilarities, Long currentEventId) {
        return eventSimilarities.stream()
                .map(s -> {
                    long recommendedEventId = (s.getEventA().equals(currentEventId))
                            ? s.getEventB()
                            : s.getEventA();
                    return RecommendedEventProto.newBuilder()
                            .setEventId(recommendedEventId)
                            .setScore(s.getScore())
                            .build();
                });
    }

    public Stream<RecommendedEventProto> mapToProto(Map<Long, Double> scoreByEvent) {
        return scoreByEvent.entrySet().stream()
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build());
    }

    public Stream<RecommendedEventProto> mapFromAggregation(List<Object[]> aggregationResults) {
        return aggregationResults.stream()
                .map(row -> RecommendedEventProto.newBuilder()
                        .setEventId((Long) row[0])
                        .setScore((Double) row[1])
                        .build());
    }
}
