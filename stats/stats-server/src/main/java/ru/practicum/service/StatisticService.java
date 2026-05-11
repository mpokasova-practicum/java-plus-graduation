package ru.practicum.service;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticService {
    void saveHit(EndpointHitDto dto);

    List<ViewStats> getStatistics(LocalDateTime start,
                                  LocalDateTime end,
                                  List<String> uris,
                                  Boolean unique);
}
