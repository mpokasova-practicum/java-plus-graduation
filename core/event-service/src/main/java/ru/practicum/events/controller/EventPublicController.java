package ru.practicum.events.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.SearchEventPublicRequest;
import ru.practicum.events.service.EventService;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class EventPublicController {
    private final EventService service;
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
    public EventFullDto findEventById(@PathVariable @Positive Long eventId, HttpServletRequest request,
                                      @RequestHeader("X-EWM-USER-ID") Long userId) {
        return service.eventById(eventId, request.getRemoteAddr(), userId);
    }

    @GetMapping("/recommendations")
    public Stream<RecommendedEventProto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @RequestParam(defaultValue = "10") int maxResults) {

        return service.getRecommendations(userId, maxResults);
    }

    @PutMapping("/{eventId}/like")
    @ResponseStatus(HttpStatus.OK)
    public void likeEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") Long userId) {

        service.likeEvent(userId, eventId);
    }
}
