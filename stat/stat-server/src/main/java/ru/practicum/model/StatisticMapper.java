package ru.practicum.model;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.EndpointHitDto;

@UtilityClass
public class StatisticMapper {
    public Statistic toStatistic(EndpointHitDto dto) {
        Statistic stat = new Statistic();
        stat.setApp(dto.getApp());
        stat.setUri(dto.getUri());
        stat.setIp(dto.getIp());
        stat.setRequestDate(dto.getTimestamp());
        return stat;
    }
}
