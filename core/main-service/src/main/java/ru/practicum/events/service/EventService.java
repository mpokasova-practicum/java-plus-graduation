package ru.practicum.events.service;

import ru.practicum.events.dto.*;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    List<EventShortDto> getEventsByOwner(Long userId, Integer from, Integer size);

    EventDto addEvent(Long userId, EventCreateDto eventCreateDto);

    List<EventShortDto> allEvents(EntityParam params, String ip);

    EventDto getEventByOwner(Long userId, Long eventId);

    EventDto updateEvent(Long userId, Long eventId, EventUpdateDto eventUpdateDto);

    EventDto eventById(Long evenId, String ip);

    List<EventDto> getEvents(List<Long> users, List<String> states, List<Long> categories,
                             LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size);

    EventDto updateEvent(Long eventId, EventAdminUpdateDto updateRequest);
}
