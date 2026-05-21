package ru.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.mapper.SimilarityMapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityCalculator {
    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;
    private static final double EPSILON = 1e-10;

    private static final Map<ActionTypeAvro, Double> WEIGHTS = Map.of(
            ActionTypeAvro.VIEW, VIEW_WEIGHT,
            ActionTypeAvro.REGISTER, REGISTER_WEIGHT,
            ActionTypeAvro.LIKE, LIKE_WEIGHT
    );
    private final SimilarityMapper similarityMapper;
    private final Map<Long, Map<Long, Double>> weightMatrix = new ConcurrentHashMap<>();
    private final Map<Long, Double> weightSumByEvent = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> eventsByUser = new ConcurrentHashMap<>();

    public List<EventSimilarityAvro> calculateSimilarity(UserActionAvro action) {
        if (action == null) {
            return Collections.emptyList();
        }

        long eventId = action.getEventId();
        long userId = action.getUserId();

        double actionWeight = WEIGHTS.get(action.getActionType());

        Map<Long, Double> userWeights = weightMatrix.computeIfAbsent(eventId,
                k -> new ConcurrentHashMap<>());
        double oldWeight = userWeights.getOrDefault(userId, 0.0);

        if (actionWeight > oldWeight) {
            userWeights.put(userId, actionWeight);

            double totalWeight = weightSumByEvent.getOrDefault(eventId, 0.0);
            totalWeight = totalWeight - oldWeight + actionWeight;
            weightSumByEvent.put(eventId, totalWeight);

            Set<Long> userEvents = eventsByUser.computeIfAbsent(userId,
                    k -> ConcurrentHashMap.newKeySet());

            List<EventSimilarityAvro> similarities = new ArrayList<>();

            for (Long otherEventId : userEvents) {
                if (otherEventId.equals(eventId)) continue;

                double similarity = recalculatePair(eventId, otherEventId, userId,
                        oldWeight, actionWeight);

                similarities.add(similarityMapper.toAvro(
                        eventId,
                        otherEventId,
                        similarity
                ));
            }

            userEvents.add(eventId);
            return similarities;
        }

        return Collections.emptyList();
    }

    private double recalculatePair(long eventA, long eventB, long userId,
                                   double oldWeight, double newWeight) {

        double otherEventWeight = weightMatrix
                .getOrDefault(eventB, Collections.emptyMap())
                .getOrDefault(userId, 0.0);

        double oldContribution = Math.min(oldWeight, otherEventWeight);
        double newContribution = Math.min(newWeight, otherEventWeight);
        double diff = newContribution - oldContribution;

        double minSum = getMinSum(eventA, eventB) + diff;
        putMinSum(eventA, eventB, minSum);

        double totalA = weightSumByEvent.getOrDefault(eventA, 0.0);
        double totalB = weightSumByEvent.getOrDefault(eventB, 0.0);

        if (Math.abs(totalA) < EPSILON || Math.abs(totalB) < EPSILON) {
            return 0.0;
        }

        return minSum / (Math.sqrt(totalA) * Math.sqrt(totalB));
    }

    private void putMinSum(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        minWeightsSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .put(second, sum);
    }

    private double getMinSum(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return minWeightsSums
                .getOrDefault(first, Collections.emptyMap())
                .getOrDefault(second, 0.0);
    }
}
