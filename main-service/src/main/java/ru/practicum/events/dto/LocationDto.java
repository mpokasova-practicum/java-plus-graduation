package ru.practicum.events.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class LocationDto {
    @NotNull
    private Float lat;
    @NotNull
    private Float lon;
}
