package ru.practicum.dto.event;

import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.constants.DateTimePattern;
import ru.practicum.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

public record SearchEventAdminRequest(

        List<Long> users,

        List<EventState> states,

        List<Long> categories,

        @DateTimeFormat(pattern = DateTimePattern.DATE_TIME_PATTERN)
        LocalDateTime rangeStart,

        @DateTimeFormat(pattern = DateTimePattern.DATE_TIME_PATTERN)
        LocalDateTime rangeEnd,

        Integer from,

        Integer size
) {
}
