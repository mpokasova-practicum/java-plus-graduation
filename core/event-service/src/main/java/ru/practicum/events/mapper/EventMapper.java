package ru.practicum.events.mapper;

import org.mapstruct.*;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.dto.event.*;
import ru.practicum.events.model.Event;

@Mapper(componentModel = "spring", uses = {LocationMapper.class, CategoryMapper.class})
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiatorId", source = "initiatorId")
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    Event toEntity(NewEventDto dto, Long initiatorId, Category category);

    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "category", source = "category")
    EventShortDto toShortDto(Event event);

    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "category", source = "category")
    EventFullDto toFullDto(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEventFromUserRequest(UpdateEventUserRequest request, @MappingTarget Event event, Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEventFromAdminRequest(UpdateEventAdminRequest request, @MappingTarget Event event, Category category);
}