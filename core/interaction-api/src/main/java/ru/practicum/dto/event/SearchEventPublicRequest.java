package ru.practicum.dto.event;

import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.constants.DateTimePattern;
import ru.practicum.enums.EventSort;

import java.time.LocalDateTime;
import java.util.List;

public record SearchEventPublicRequest(

        String text,

        List<Long> categories,

        Boolean paid,

        @DateTimeFormat(pattern = DateTimePattern.DATE_TIME_PATTERN)
        LocalDateTime rangeStart,

        @DateTimeFormat(pattern = DateTimePattern.DATE_TIME_PATTERN)
        LocalDateTime rangeEnd,

        Boolean onlyAvailable,

        EventSort sort,

        Integer from,

        Integer size
) {
    public SearchEventPublicRequest {
        if (onlyAvailable == null) {
            onlyAvailable = false;
        }
        if (sort == null) {
            sort = EventSort.EVENT_DATE;
        }
        if (from == null) {
            from = 0;
        }
        if (size == null) {
            size = 10;
        }
    }
}
