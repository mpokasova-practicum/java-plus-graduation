package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.model.EventSimilarity;
import ru.practicum.repository.EventSimilarityRepository;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSimilarityServiceImpl implements EventSimilarityService {
    private final EventSimilarityRepository eventSimilarityRepository;

    @Override
    @Transactional
    public void save(EventSimilarity eventSimilarity) {
        Optional<EventSimilarity> maybeSimilarity = eventSimilarityRepository
                .findByEventAAndEventB(
                        eventSimilarity.getEventA(),
                        eventSimilarity.getEventB()
                );

        if (maybeSimilarity.isPresent()) {
            EventSimilarity oldSimilarity = maybeSimilarity.get();

            if (!oldSimilarity.getScore().equals(eventSimilarity.getScore())) {
                oldSimilarity.setScore(eventSimilarity.getScore());
                eventSimilarityRepository.save(oldSimilarity);

                log.debug("Обновлено сходство: {} и {}, score={}",
                        oldSimilarity.getEventA(),
                        oldSimilarity.getEventB(),
                        oldSimilarity.getScore());
            }
        } else {
            eventSimilarityRepository.save(eventSimilarity);

            log.debug("Добавлено новое сходство: {} и {}, score={}",
                    eventSimilarity.getEventA(),
                    eventSimilarity.getEventB(),
                    eventSimilarity.getScore());
        }
    }
}
