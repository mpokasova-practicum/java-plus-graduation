package ru.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Slf4j
@Component
public class UserActionMapper {

    public UserActionAvro mapToAvro(UserActionProto proto) {
        Instant instant = Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        );

        ActionTypeAvro actionType = convertActionType(proto.getActionType());

        return UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(actionType)
                .setTimestamp(instant)
                .build();
    }

    private ActionTypeAvro convertActionType(ActionTypeProto protoType) {
        return switch (protoType) {
            case VIEW -> ActionTypeAvro.VIEW;
            case REGISTER -> ActionTypeAvro.REGISTER;
            case LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> throw new IllegalArgumentException(
                    "Неизвестный тип действия: " + protoType);
        };
    }
}
