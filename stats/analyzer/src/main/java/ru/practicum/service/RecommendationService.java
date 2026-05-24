package ru.practicum.service;

import ru.practicum.ewm.stats.proto.*;

import java.util.stream.Stream;

public interface RecommendationService {

    Stream<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request);

    Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request);

    Stream<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request);

    HasUserInteractionResponseProto hasUserInteraction(Long userId, Long eventId);
}
