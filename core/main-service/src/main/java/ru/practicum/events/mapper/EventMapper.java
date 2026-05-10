package ru.practicum.events.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.events.dto.EventCreateDto;
import ru.practicum.events.dto.EventDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.dto.LocationDto;
import ru.practicum.events.model.Event;
import ru.practicum.user.dto.UserShortDto;
import ru.practicum.user.mapper.UserMapper;

@UtilityClass
public class EventMapper {
    public EventDto mapToDto(Event event, Long confirmedRequest, Long views) {
        EventDto.EventDtoBuilder builder = EventDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .confirmedRequests(confirmedRequest != null ? confirmedRequest : 0L)
                .views(views != null ? views : 0L)
                .paid(event.getPaid())
                .requestModeration(event.getRequestModeration())
                .participantLimit(event.getParticipantLimit())
                .location(LocationDto.builder()
                        .lat(event.getLocation().getLat())
                        .lon(event.getLocation().getLon())
                        .build())
                .state(event.getState())
                .initiator(UserShortDto.builder()
                        .id(event.getInitiator().getId())
                        .name(event.getInitiator().getName())
                        .build())
                .eventDate(event.getEventDate())
                .createdOn(event.getCreatedOn())
                .publishedOn(event.getPublishedOn());

        if (event.getCategory() != null) {
            builder.category(CategoryMapper.mapToDto(event.getCategory()));
        }

        return builder.build();
    }

    public static EventShortDto mapToShortDto(Event event, Long confirmedRequest, Long views, Integer countOfComments) {
        EventShortDto.EventShortDtoBuilder builder = EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .confirmedRequests(confirmedRequest != null ? confirmedRequest : 0L)
                .views(views != null ? views : 0L)
                .paid(event.getPaid())
                .initiator(UserShortDto.builder()
                        .id(event.getInitiator().getId())
                        .name(event.getInitiator().getName())
                        .build())
                .countOfComments(countOfComments)
                .eventDate(event.getEventDate());

        if (event.getCategory() != null) {
            builder.category(CategoryDto.builder()
                    .id(event.getCategory().getId())
                    .name(event.getCategory().getName())
                    .build());
        }

        return builder.build();
    }

    public Event toEvent(EventCreateDto dto) {
        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setAnnotation(dto.getAnnotation());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setPaid(dto.getPaid() != null ? dto.getPaid() : false);
        event.setParticipantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0);
        event.setRequestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true);
        return event;
    }

    public EventShortDto toEventShortDto(Event event) {
        EventShortDto.EventShortDtoBuilder builder = EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .confirmedRequests(0L)
                .views(0L)
                .eventDate(event.getEventDate())
                .paid(event.getPaid())
                .initiator(UserMapper.toUserShortDto(event.getInitiator()));

        if (event.getCategory() != null) {
            builder.category(CategoryMapper.mapToDto(event.getCategory()));
        }

        return builder.build();
    }

    public EventDto toEventDto(Event event) {
        EventDto.EventDtoBuilder builder = EventDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .confirmedRequests(0L)
                .views(0L)
                .eventDate(event.getEventDate())
                .paid(event.getPaid())
                .requestModeration(event.getRequestModeration())
                .participantLimit(event.getParticipantLimit())
                .location(LocationMapper.toLocationDto(event.getLocation()))
                .initiator(UserMapper.toUserShortDto(event.getInitiator()))
                .state(event.getState())
                .publishedOn(event.getPublishedOn())
                .createdOn(event.getCreatedOn());

        if (event.getCategory() != null) {
            builder.category(CategoryMapper.mapToDto(event.getCategory()));
        }

        return builder.build();
    }
}