package ru.practicum.events.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {
    @NotNull
    private Float lat;

    @NotNull
    private Float lon;
}
