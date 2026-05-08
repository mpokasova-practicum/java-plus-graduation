package ru.practicum.events.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.StatClient;
import ru.practicum.events.dto.EntityParam;
import ru.practicum.events.dto.EventDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.model.EventSort;
import ru.practicum.events.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class EventPublicController {
    private final EventService service;
    private final StatClient statClient;
    private static final String MAIN_SERVICE = "ewm-main-service";

    @GetMapping
    public List<EventShortDto> allEvents(@RequestParam(required = false) String text,
                                         @RequestParam(defaultValue = "views") String sort,
                                         @Min(0) @RequestParam(defaultValue = "0") Integer from,
                                         @Positive @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam(required = false) List<Long> categories,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                         @RequestParam(required = false) Boolean paid,
                                         @RequestParam(defaultValue = "false") Boolean onlyOnAvailable,
                                         HttpServletRequest request) {
        EntityParam entityParam = EntityParam.builder()
                .text(text)
                .sort(EventSort.from(sort))
                .from(from)
                .size(size)
                .categories(categories)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .paid(paid)
                .onlyAvailable(onlyOnAvailable)
                .build();
        return service.allEvents(entityParam, request.getRemoteAddr());
    }

    @GetMapping("/{eventId}")
    public EventDto findEventById(@Positive @PathVariable Long eventId, HttpServletRequest request) {
        return service.eventById(eventId, request.getRemoteAddr());
    }
}
