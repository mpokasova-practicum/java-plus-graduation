package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;
import ru.practicum.mapper.RecommendationsMapper;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.UserAction;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserActionRepository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {
    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;
    private final RecommendationsMapper recommendationsMapper;

    @Override
    public Stream<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        Long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        Pageable recentPage = PageRequest.of(0, maxResults,
                Sort.by(Sort.Direction.DESC, "timestamp"));
        List<Long> recentUserEvents = userActionRepository.findRecentEventIdsByUserId(userId, recentPage);

        if (recentUserEvents.isEmpty()) {
            log.info("У пользователя userId={} нет взаимодействий, рекомендации не найдены", userId);
            return Stream.empty();
        }

        Set<Long> allUserEvents = userActionRepository.findEventIdsByUserId(userId);

        Pageable similarPage = PageRequest.of(0, maxResults, Sort.by(Sort.Direction.DESC, "score"));
        List<EventSimilarity> candidates = eventSimilarityRepository.findNewSimilarEvents(
                recentUserEvents,
                new ArrayList<>(allUserEvents),
                similarPage
        );

        if (candidates.isEmpty()) {
            log.info("Не найдено похожих мероприятий для пользователя userId={}", userId);
            return Stream.empty();
        }

        Map<Long, Double> userScores = userActionRepository.findAllByUserId(userId)
                .stream()
                .collect(Collectors.toMap(
                        UserAction::getEventId,
                        UserAction::getUserScore,
                        (existing, replacement) -> existing
                ));

        Map<Long, Double> predictedScores = predictScores(candidates, recentUserEvents, userScores, maxResults);

        List<Map.Entry<Long, Double>> sorted = predictedScores.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .collect(Collectors.toList());

        Map<Long, Double> result = new LinkedHashMap<>();
        sorted.forEach(entry -> result.put(entry.getKey(), entry.getValue()));

        log.info("Сформировано {} рекомендаций для пользователя userId={}", result.size(), userId);
        return recommendationsMapper.mapToProto(result);
    }

    @Override
    public Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        Long eventId = request.getEventId();
        Long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        Pageable pageable = PageRequest.of(0, maxResults, Sort.by(Sort.Direction.DESC, "score"));
        List<EventSimilarity> similarities = eventSimilarityRepository.findSimilarEventsForUser(
                eventId, userId, pageable
        );

        log.info("Найдено {} похожих мероприятий для eventId={}, userId={}",
                similarities.size(), eventId, userId);

        return recommendationsMapper.mapToProto(similarities, eventId);
    }

    @Override
    public Stream<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        List<Long> eventIds = request.getEventIdList();
        Map<Long, Double> sumOfScoresByEvent = new HashMap<>();
        List<UserAction> userActions = userActionRepository.findByEventIdIn(new HashSet<>(eventIds));

        for (UserAction userAction : userActions) {
            long eventId = userAction.getEventId();
            double sum = sumOfScoresByEvent.getOrDefault(eventId, 0.0);
            double score = userAction.getUserScore();
            sum += score;
            sumOfScoresByEvent.put(eventId, sum);
        }

        log.info("Рассчитаны суммы взаимодействий для {} мероприятий", sumOfScoresByEvent.size());

        return recommendationsMapper.mapToProto(sumOfScoresByEvent);
    }

    @Override
    public HasUserInteractionResponseProto hasUserInteraction(Long userId, Long eventId) {
        boolean hasInteraction = userActionRepository.existsByUserIdAndEventId(userId, eventId);
        return HasUserInteractionResponseProto.newBuilder()
                .setHasInteraction(hasInteraction)
                .build();
    }

    private Map<Long, Double> predictScores(List<EventSimilarity> candidates,
                                            List<Long> recentUserEvents,
                                            Map<Long, Double> userScores,
                                            int maxResults) {

        if (candidates.isEmpty() || recentUserEvents.isEmpty()) {
            return Map.of();
        }

        List<Long> candidateIds = new ArrayList<>();
        for (EventSimilarity candidate : candidates) {
            long candidateId = recentUserEvents.contains(candidate.getEventA())
                    ? candidate.getEventB()
                    : candidate.getEventA();
            candidateIds.add(candidateId);
        }

        List<EventSimilarity> allNeighbours = eventSimilarityRepository.findAllNeighboursForCandidates(
                candidateIds, recentUserEvents
        );

        Map<Long, List<EventSimilarity>> neighboursByCandidate = new HashMap<>();
        for (EventSimilarity neighbour: allNeighbours) {
            Long candidateId = null;
            if (candidateIds.contains(neighbour.getEventA())
                    && recentUserEvents.contains(neighbour.getEventB())) {
                candidateId = neighbour.getEventA();
            } else if (candidateIds.contains(neighbour.getEventB()) && recentUserEvents.contains(neighbour.getEventA())) {
                candidateId = neighbour.getEventB();
            }

            if (candidateId != null) {
                neighboursByCandidate
                        .computeIfAbsent(candidateId, k -> new ArrayList<>())
                        .add(neighbour);
            }
        }

        Map<Long, Double> predictedScores = new HashMap<>();

        for (Long candidateId : candidateIds) {
            List<EventSimilarity> neighbours = neighboursByCandidate.getOrDefault(candidateId, List.of());

            if (neighbours.isEmpty()) {
                log.debug("Нет соседей среди просмотренных для кандидата {}", candidateId);
                continue;
            }

            List<EventSimilarity> topNeighbours = neighbours.stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(maxResults)
                    .toList();

            double weightedSum = 0.0;
            double similaritySum = 0.0;

            for (EventSimilarity neighbour : topNeighbours) {
                long neighbourId = neighbour.getEventA().equals(candidateId)
                        ? neighbour.getEventB()
                        : neighbour.getEventA();

                Double score = userScores.get(neighbourId);
                if (score != null && score > 0) {
                    weightedSum += score * neighbour.getScore();
                    similaritySum += neighbour.getScore();
                }
            }

            if (similaritySum > 0) {
                double predictedScore = weightedSum / similaritySum;
                predictedScores.put(candidateId, predictedScore);
                log.debug("Предсказана оценка {} для мероприятия {}", predictedScore, candidateId);
            }
        }

        return predictedScores;
    }
}
