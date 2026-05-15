package ru.practicum.events.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.client.UserClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.exception.NotFoundException;

@Slf4j
@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserClient userClient;

    @GetMapping("/{eventId}")
    public EventFullDto getEvent(@PathVariable @Positive Long eventId) {
        log.debug("Controller: getEvent eventId={}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found with id: " + eventId));

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setInitiator(userClient.getUser(event.getInitiatorId()));

        return dto;
    }
}
