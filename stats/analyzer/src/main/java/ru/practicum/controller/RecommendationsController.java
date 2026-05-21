package ru.practicum.controller;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.*;
import ru.practicum.service.RecommendationService;

import java.util.stream.Stream;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("gRPC запрос рекомендаций: userId={}, maxResults={}",
                request.getUserId(), request.getMaxResults());
        handleStreamResponse(recommendationService.getRecommendationsForUser(request), responseObserver);
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("gRPC запрос похожих событий: eventId={}, userId={}, maxResults={}",
                request.getEventId(), request.getUserId(), request.getMaxResults());
        handleStreamResponse(recommendationService.getSimilarEvents(request), responseObserver);
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("gRPC запрос количества взаимодействий: events={}",
                request.getEventIdList());
        handleStreamResponse(recommendationService.getInteractionsCount(request), responseObserver);
    }

    private void handleStreamResponse(Stream<RecommendedEventProto> stream,
                                      StreamObserver<RecommendedEventProto> responseObserver) {
        try (Stream<RecommendedEventProto> safeStream = stream) {
            safeStream.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
            log.debug("gRPC ответ успешно отправлен");
        } catch (Exception e) {
            log.error("Ошибка при обработке gRPC запроса: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
