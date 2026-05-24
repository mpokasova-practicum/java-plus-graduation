package ru.practicum.events.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.event.*;
import ru.practicum.events.model.Event;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.List;
import java.util.stream.Stream;

public interface EventService {
    List<EventShortDto> getEventsByOwner(Long userId, Pageable pageable);

    EventFullDto addEvent(Long userId, NewEventDto eventCreateDto);

    List<EventShortDto> allEvents(SearchEventPublicRequest requestParams, Pageable pageable, String ip);

    EventFullDto getEventByOwner(Long userId, Long eventId, String ip);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest eventUpdateDto);

    EventFullDto eventById(Long evenId, String ip, Long userId);

    List<EventFullDto> getEventsAdmin(SearchEventAdminRequest request, Pageable pageable);

    EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest request);

    Event getEventOrThrow(Long eventId);

    Stream<RecommendedEventProto> getRecommendations(Long userId, int maxResults);

    void likeEvent(Long userId, Long eventId);
}
