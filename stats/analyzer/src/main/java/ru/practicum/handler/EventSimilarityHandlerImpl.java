package ru.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.mapper.EventSimilarityMapper;
import ru.practicum.model.EventSimilarity;
import ru.practicum.service.EventSimilarityService;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityHandlerImpl implements EventSimilarityHandler {
    private final EventSimilarityService eventSimilarityService;
    private final EventSimilarityMapper mapper;

    @Override
    public void handle(EventSimilarityAvro avro) {
        EventSimilarity entity = mapper.toEntity(avro);
        eventSimilarityService.save(entity);

        log.debug("Сохранено сходство: {} и {}, score={}",
                entity.getEventA(), entity.getEventB(), entity.getScore());
    }
}
