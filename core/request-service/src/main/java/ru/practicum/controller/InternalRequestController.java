package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.enums.RequestStatus;
import ru.practicum.repository.RequestRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final RequestRepository requestRepository;

    @GetMapping("/event/{eventId}/count/{status}")
    public Long countByStatus(@PathVariable Long eventId, @PathVariable RequestStatus status) {
        return requestRepository.countByEventIdAndStatus(eventId, status);
    }

    @PostMapping("/events/count/confirmed")
    public Map<Long, Long> getConfirmedRequestsCount(@RequestBody List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();

        List<Object[]> results = requestRepository.countConfirmedByEventIds(eventIds);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
