package ru.practicum.client;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

@Slf4j
@Service
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public void sendUserAction(UserActionProto action) {
        try {
            collectorStub.collectUserAction(action);
            log.debug("Действие отправлено: userId={}, eventId={}",
                    action.getUserId(), action.getEventId());
        } catch (StatusRuntimeException e) {
            log.error("Ошибка отправки действия: {}", e.getMessage());
            throw e;
        }
    }
}
