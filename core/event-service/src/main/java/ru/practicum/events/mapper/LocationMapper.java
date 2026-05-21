package ru.practicum.events.mapper;

import org.mapstruct.Mapper;
import ru.practicum.dto.event.LocationDto;
import ru.practicum.events.model.Location;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    Location toLocation(LocationDto dto);

    LocationDto toDto(Location entity);
}
