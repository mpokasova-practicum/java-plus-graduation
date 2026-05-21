package ru.practicum.events.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.SearchEventPublicRequest;
import ru.practicum.events.service.EventService;

import java.util.List;


@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class EventPublicController {
    private final EventService service;
    private final StatsClient statClient;
    private static final String MAIN_SERVICE = "ewm-main-service";

    @GetMapping
    public List<EventShortDto> allEvents(@ModelAttribute @Valid SearchEventPublicRequest request,
                                         HttpServletRequest httpRequest) {
        int size = (request.size() != null && request.size() > 0) ? request.size() : 10;
        int from = request.from() != null ? request.from() : 0;
        PageRequest pageRequest = PageRequest.of(from / size, size);
        return service.allEvents(request, pageRequest, httpRequest.getRemoteAddr());
    }

    @GetMapping("/{eventId}")
    public EventFullDto findEventById(@Positive @PathVariable Long eventId, HttpServletRequest request) {
        return service.eventById(eventId, request.getRemoteAddr());
    }
}
