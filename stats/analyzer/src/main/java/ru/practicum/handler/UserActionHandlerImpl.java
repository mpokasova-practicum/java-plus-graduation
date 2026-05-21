package ru.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.mapper.UserActionMapper;
import ru.practicum.model.UserAction;
import ru.practicum.service.UserActionService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionHandlerImpl implements UserActionHandler {
    private final UserActionService userActionService;
    private final UserActionMapper mapper;

    @Override
    public void handle(UserActionAvro avro) {
        UserAction entity = mapper.toEntity(avro);
        userActionService.save(entity);

        log.debug("Сохранено действие: userId={}, eventId={}, weight={}",
                entity.getUserId(), entity.getEventId(), entity.getUserScore());
    }
}
