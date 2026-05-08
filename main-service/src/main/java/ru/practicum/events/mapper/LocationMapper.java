package ru.practicum.events.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.events.dto.LocationDto;
import ru.practicum.events.model.Location;

@UtilityClass
public class LocationMapper {
    public Location toLocation(LocationDto dto) {
        Location location = new Location();
        location.setLat(dto.getLat());
        location.setLon(dto.getLon());
        return location;
    }

    public LocationDto toLocationDto(Location location) {
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }
}
