package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.enums.RequestStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RequestClientFallback implements RequestClient {

    @Override
    public Long countByStatus(Long eventId, RequestStatus status) {
        log.warn("Сервис запросов недоступен, eventId={}, status={}", eventId, status);
        return 0L;
    }

    @Override
    public Map<Long, Long> getConfirmedRequestsCount(List<Long> eventIds) {
        log.warn("Сервис запросов недоступен для batch, eventIds={}", eventIds);
        return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0L));
    }
}
