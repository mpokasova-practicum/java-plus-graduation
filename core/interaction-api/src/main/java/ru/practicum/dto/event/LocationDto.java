package ru.practicum.dto.event;

import jakarta.validation.constraints.NotNull;

public record LocationDto(

       @NotNull
       Float lat,

       @NotNull
       Float lon
) {
}
