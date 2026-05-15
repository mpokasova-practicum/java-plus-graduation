package ru.practicum.dto.event;

import java.util.Map;

public record EventStatistics(

        Map<Long, Long> confirmedRequests,

        Map<Long, Long> views
) {
}
