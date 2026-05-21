package ru.practicum.client;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.*;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    public Stream<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        try {
            Iterator<RecommendedEventProto> iterator = analyzerStub.getRecommendationsForUser(request);
            return asStream(iterator);
        } catch (StatusRuntimeException e) {
            log.error("Ошибка получения рекомендаций: {}", e.getMessage());
            return Stream.empty();
        }
    }

    public Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        try {
            Iterator<RecommendedEventProto> iterator = analyzerStub.getSimilarEvents(request);
            return asStream(iterator);
        } catch (StatusRuntimeException e) {
            log.error("Ошибка получения похожих событий: {}", e.getMessage());
            return Stream.empty();
        }
    }

    public Stream<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        try {
            Iterator<RecommendedEventProto> iterator = analyzerStub.getInteractionsCount(request);
            return asStream(iterator);
        } catch (StatusRuntimeException e) {
            log.error("Ошибка получения количества взаимодействий: {}", e.getMessage());
            return Stream.empty();
        }
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}
