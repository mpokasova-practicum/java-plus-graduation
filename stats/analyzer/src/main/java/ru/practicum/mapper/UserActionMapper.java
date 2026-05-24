package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.model.UserAction;

import java.util.Map;

@Component
public class UserActionMapper {

    private static final Map<ActionTypeAvro, Double> WEIGHTS = Map.of(
            ActionTypeAvro.VIEW, 0.4,
            ActionTypeAvro.REGISTER, 0.8,
            ActionTypeAvro.LIKE, 1.0
    );

    public UserAction toEntity(UserActionAvro avro) {

        return UserAction.builder()
                .userId(avro.getUserId())
                .eventId(avro.getEventId())
                .userScore(WEIGHTS.get(avro.getActionType()))
                .timestamp(avro.getTimestamp())
                .build();
    }
}
