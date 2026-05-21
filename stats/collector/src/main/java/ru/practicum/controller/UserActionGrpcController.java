package ru.practicum.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.processor.UserActionProcessor;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionGrpcController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final UserActionProcessor processor;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        log.info("Получен запрос на обработку действия: userId={}, eventId={}, action={}",
                request.getUserId(), request.getEventId(), request.getActionType());

        try {
            processor.process(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

            log.info("Действие пользователя успешно обработано: userId={}",
                    request.getUserId());

        } catch (IllegalArgumentException e) {
            log.warn("Некорректный запрос: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (Exception e) {
            log.error("Ошибка обработки действия пользователя: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Внутренняя ошибка сервера")
                    .asRuntimeException());
        }
    }
}
