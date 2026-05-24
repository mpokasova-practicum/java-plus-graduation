package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.config.FeignRetryConfig;
import ru.practicum.enums.RequestStatus;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "request-service",
        path = "/internal/requests",
        configuration = FeignRetryConfig.class,
        fallback = RequestClientFallback.class
)
public interface RequestClient {

    @GetMapping("/event/{eventId}/count/{status}")
    Long countByStatus(@PathVariable Long eventId, @PathVariable RequestStatus status);

    @PostMapping("/events/count/confirmed")
    Map<Long, Long> getConfirmedRequestsCount(@RequestBody List<Long> eventIds);

    @GetMapping("/internal/requests/user/{userId}/event/{eventId}/confirmed")
    Boolean hasUserConfirmedRequest(@PathVariable Long userId, @PathVariable Long eventId);
}
