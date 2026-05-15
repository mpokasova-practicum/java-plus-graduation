package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ru.practicum.constants.DateTimePattern;
import ru.practicum.enums.AdminUpdateStateAction;

import java.time.LocalDateTime;

public record UpdateEventAdminRequest(

        @Size(min = 20, max = 2000)
        String annotation,

        Long category,

        @Size(min = 20, max = 7000)
        String description,

        @Future
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateTimePattern.DATE_TIME_PATTERN)
        LocalDateTime eventDate,

        LocationDto location,

        Boolean paid,

        @PositiveOrZero
        Integer participantLimit,

        Boolean requestModeration,

        AdminUpdateStateAction stateAction,

        @Size(min = 3, max = 120)
        String title
) {
}
