package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.repository.StatisticRepository;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.model.StatisticMapper;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StatisticServiceImpl implements StatisticService {
    private final StatisticRepository repository;

    @Override
    public void saveHit(EndpointHitDto dto) {
        repository.save(StatisticMapper.toStatistic(dto));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStats> getStatistics(LocalDateTime start,
                                         LocalDateTime end,
                                         List<String> uris,
                                         Boolean unique) {
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректно указаны начало и конец диапазона");
        }

        if (unique) {
            if (uris != null) {
                return repository.findUniqueIpAndUrisIn(start, end, uris);
            }
            return repository.findUniqueIp(start, end);
        } else {
            if (uris != null) {
                return repository.findNotUniqueIpAndUrisIn(start, end, uris);
            }
            return repository.findNotUniqueIp(start, end);
        }
    }
}
