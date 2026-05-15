package ru.practicum.events.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.event.UpdateEventUserRequest;
import ru.practicum.events.service.EventService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
@Validated
public class EventPrivateController {
    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEventsByOwner(@PathVariable Long userId,
                                                @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                                @RequestParam(defaultValue = "10") @Positive Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        log.debug("Controller: getEvents userId={}, pageable={}", userId, pageable);
        return eventService.getEventsByOwner(userId, pageable);

    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(@PathVariable Long userId,
                             @Valid @RequestBody NewEventDto eventCreateDto) {
        return eventService.addEvent(userId, eventCreateDto);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEventByOwner(@PathVariable Long userId,
                                        @PathVariable Long eventId,
                                        HttpServletRequest request) {
        return eventService.getEventByOwner(userId, eventId, request.getRemoteAddr());
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable Long userId,
                                @PathVariable Long eventId,
                                @Valid @RequestBody UpdateEventUserRequest eventUpdateDto) {
        return eventService.updateEvent(userId, eventId, eventUpdateDto);
    }


}
