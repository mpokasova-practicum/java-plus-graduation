package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventFullDto;

@Component
@Slf4j
public class EventClientFallback implements EventClient {

    @Override
    public EventFullDto getEvent(Long eventId) {
        log.warn("Сервис событий недоступен");
        return null;
    }
}
