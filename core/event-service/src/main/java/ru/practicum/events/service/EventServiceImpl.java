package ru.practicum.events.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.*;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.dto.event.*;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.enums.*;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.*;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.events.repository.SearchEventSpecifications;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final CategoryRepository categoryRepository;
    private final AnalyzerClient analyzerClient;
    private final CollectorClient collectorClient;

    @Override
    public EventFullDto addEvent(Long userId, NewEventDto eventCreateDto) {
        log.info("Валидация даты и времени события");
        validateEventDate(eventCreateDto.eventDate(), EventState.PENDING);

        checkUserExists(userId);
        return addEventInTransaction(userId, eventCreateDto);
    }

    @Transactional
    protected EventFullDto addEventInTransaction(Long userId, NewEventDto eventCreateDto) {
        Category category = categoryRepository.findById(eventCreateDto.category())
                .orElseThrow(() -> new NotFoundException("Категория с id=" + eventCreateDto.category() + " не найдена"));

        Event event = eventMapper.toEntity(eventCreateDto, userId, category);
        event.setRating(0.0);
        event = eventRepository.save(event);

        log.info("Создано событие с id={}, title={}, initiatorId={}", event.getId(), event.getTitle(), userId);

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setInitiator(getUserShortDto(userId));
        dto.setConfirmedRequests(0L);
        dto.setRating(0.0);
        return dto;
    }

    @Override
    public List<EventShortDto> getEventsByOwner(Long userId, Pageable pageable) {
        checkUserExists(userId);

        List<Event> events = eventRepository.findAllByInitiatorIdOrderByCreatedOnDesc(userId, pageable);

        if (events.isEmpty()) return List.of();

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        Map<Long, Long> confirmedRequests = getConfirmedRequests(eventIds);
        Map<Long, Double> ratings = getRatingsForEvents(eventIds);

        Map<Long, UserShortDto> userCache = new HashMap<>();

        return events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toShortDto(event);
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
                    dto.setRating(ratings.getOrDefault(event.getId(), 0.0));

                    UserShortDto initiatorDto = userCache.computeIfAbsent(
                            event.getInitiatorId(),
                            this::getUserShortDto
                    );
                    dto.setInitiator(initiatorDto);
                    return dto;
                })
                .toList();
    }

    @Override
    public EventFullDto getEventByOwner(Long userId, Long eventId, String ip) {
        try {
            userClient.getUser(userId);
            Event event = getEventOrThrow(eventId);

            if (!event.getInitiatorId().equals(userId)) {
                throw new NotFoundException("Событие c id " + eventId + " не найдено у пользователя с id " + userId);
            }

            return buildFullDto(event);

        } catch (Exception e) {
            log.warn("Сервис пользователей недоступен или пользователь не найден, userId={}", userId);
            Event event = getEventOrThrow(eventId);
            return buildFullDto(event);
        }
    }

    @Override
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest eventUpdateDto) {
        checkUserExists(userId);

        Event updatedEvent = updateEventInTransaction(userId, eventId, eventUpdateDto);
        return buildFullDto(updatedEvent);
    }

    @Transactional
    public Event updateEventInTransaction(Long userId, Long eventId, UpdateEventUserRequest eventUpdateDto) {
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId))
            throw new ValidationException("Пользователь не является инициатором события и не может его редактировать");

        if (event.getState() == EventState.PUBLISHED)
            throw new ValidationException("Изменять можно только не опубликованные события");

        if (eventUpdateDto.eventDate() != null) {
            validateDateEvent(eventUpdateDto.eventDate(), 2);
        }

        Category category = null;
        if (eventUpdateDto.category() != null) {
            category = categoryRepository.findById(eventUpdateDto.category())
                    .orElseThrow(() -> new NotFoundException("Категория с id=" + eventUpdateDto.category() + " не найдена"));
        }
        eventMapper.updateEventFromUserRequest(eventUpdateDto, event, category);

        if (eventUpdateDto.stateAction() != null) {
            if (eventUpdateDto.stateAction() == UserUpdateStateAction.SEND_TO_REVIEW)
                event.pending();
            if (eventUpdateDto.stateAction() == UserUpdateStateAction.CANCEL_REVIEW)
                event.canceled();
        }

        return eventRepository.save(event);
    }

    @Override
    public List<EventFullDto> getEventsAdmin(SearchEventAdminRequest request, Pageable pageable) {
        log.info("Поиск событий с параметрами: users={}, states={}, categories={}, rangeStart={}, rangeEnd={}, from={}, size={}",
                request.users(), request.states(), request.categories(), request.rangeStart(), request.rangeEnd(), request.from(), request.size());

        validateRangeStartAndEnd(request.rangeStart(), request.rangeEnd());

        Specification<Event> specification = SearchEventSpecifications.addWhereNull();
        if (request.users() != null && !request.users().isEmpty())
            specification = specification.and(SearchEventSpecifications.addWhereUsers(request.users()));
        if (request.states() != null && !request.states().isEmpty())
            specification = specification.and(SearchEventSpecifications.addWhereStates(request.states()));
        if (request.categories() != null && !request.categories().isEmpty())
            specification = specification.and(SearchEventSpecifications.addWhereCategories(request.categories()));
        if (request.rangeStart() != null)
            specification = specification.and(SearchEventSpecifications.addWhereStartsBefore(request.rangeStart()));
        if (request.rangeEnd() != null)
            specification = specification.and(SearchEventSpecifications.addWhereEndsAfter(request.rangeEnd()));
        if (request.rangeStart() == null && request.rangeEnd() == null)
            specification = specification.and(SearchEventSpecifications.addWhereStartsBefore(LocalDateTime.now()));

        Page<Event> eventsPage = eventRepository.findAll(specification, pageable);

        if (eventsPage.isEmpty()) {
            return List.of();
        }

        List<Event> events = eventsPage.getContent();
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        Map<Long, Long> confirmedRequests = getConfirmedRequests(eventIds);
        Map<Long, Double> ratings = getRatingsForEvents(eventIds);
        Map<Long, UserShortDto> userCache = new HashMap<>();

        return events.stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toFullDto(event);
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
                    dto.setRating(ratings.getOrDefault(event.getId(), 0.0));

                    UserShortDto initiatorDto = userCache.computeIfAbsent(
                            event.getInitiatorId(),
                            this::getUserShortDto
                    );
                    dto.setInitiator(initiatorDto);
                    return dto;
                })
                .toList();
    }

    @Override
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest request) {
        log.info("Обновление события с id = {} администратором: {}", eventId, request);

        Event updatedEvent = updateEventAdminInTransaction(eventId, request);

        return buildFullDto(updatedEvent);

    }

    @Transactional
    protected Event updateEventAdminInTransaction(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

        if (request.stateAction() != null) {
            AdminUpdateStateAction stateActionAdmin = request.stateAction();
            EventState currentState = event.getState();

            if (stateActionAdmin == AdminUpdateStateAction.PUBLISH_EVENT) {
                if (currentState != EventState.PENDING)
                    throw new ConflictException(
                            "Событие можно опубликовать только если оно в состоянии ожидания публикации");
                if (event.getEventDate() != null) {
                    validateDateEvent(event.getEventDate(), 1);
                }
                event.publish();
            }
            if (stateActionAdmin == AdminUpdateStateAction.REJECT_EVENT) {
                if (currentState == EventState.PUBLISHED)
                    throw new ConflictException("Событие можно отклонить пока оно не опубликовано");
                event.canceled();
            }
        }

        Category category = null;
        if (request.category() != null) {
            category = categoryRepository.findById(request.category())
                    .orElseThrow(() -> new NotFoundException("Категория с id=" + request.category() + " не найдена"));
        }

        eventMapper.updateEventFromAdminRequest(request, event, category);
        Event updatedEvent = eventRepository.save(event);

        log.info("Обновлено событие с id={}, title={}, initiatorId={}",
                updatedEvent.getId(), updatedEvent.getTitle(), updatedEvent.getInitiatorId());

        return updatedEvent;
    }

    @Override
    public List<EventShortDto> allEvents(SearchEventPublicRequest request, Pageable pageable, String ip) {

        validateRangeStartAndEnd(request.rangeStart(), request.rangeEnd());

        Specification<Event> specification = buildSpecification(request);
        List<Event> events = eventRepository.findAll(specification, pageable).getContent();

        if (events.isEmpty()) {
            return List.of();
        }

        List<EventShortDto> result = enrichEventsWithDetails(events);

        result = sortByRequest(result, request.sort());
        return result;
    }

    private Specification<Event> buildSpecification(SearchEventPublicRequest request) {
        Specification<Event> spec = SearchEventSpecifications.addWhereNull();

        if (request.text() != null && !request.text().trim().isEmpty()) {
            spec = spec.and(SearchEventSpecifications.addLikeText(request.text()));
        }
        if (request.categories() != null && !request.categories().isEmpty()) {
            spec = spec.and(SearchEventSpecifications.addWhereCategories(request.categories()));
        }
        if (request.paid() != null) {
            spec = spec.and(SearchEventSpecifications.isPaid(request.paid()));
        }
        LocalDateTime rangeStart = (request.rangeStart() == null && request.rangeEnd() == null) ?
                LocalDateTime.now() : request.rangeStart();
        if (rangeStart != null) {
            spec = spec.and(SearchEventSpecifications.addWhereStartsBefore(rangeStart));
        }
        if (request.rangeEnd() != null) {
            spec = spec.and(SearchEventSpecifications.addWhereEndsAfter(request.rangeEnd()));
        }
        if (request.onlyAvailable()) {
            spec = spec.and(SearchEventSpecifications.addWhereAvailableSlots());
        }
        return spec;
    }

    private List<EventShortDto> enrichEventsWithDetails(List<Event> events) {
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        Map<Long, Long> confirmedRequests = getConfirmedRequests(eventIds);
        Map<Long, Double> ratings = getRatingsForEvents(eventIds);
        Map<Long, UserShortDto> userCache = new HashMap<>();

        return events.stream()
                .map(event -> enrichEventWithDetails(event, confirmedRequests, ratings, userCache))
                .toList();
    }

    private EventShortDto enrichEventWithDetails(Event event,
                                                 Map<Long, Long> confirmedRequests,
                                                 Map<Long, Double> ratings,
                                                 Map<Long, UserShortDto> userCache) {
        EventShortDto dto = eventMapper.toShortDto(event);
        dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
        dto.setRating(ratings.getOrDefault(event.getId(), 0.0));

        UserShortDto initiatorDto = userCache.computeIfAbsent(
                event.getInitiatorId(),
                this::getUserShortDto
        );
        dto.setInitiator(initiatorDto);
        return dto;
    }

    private List<EventShortDto> sortByRequest(List<EventShortDto> dtos, EventSort sort) {
        if (sort == null) return dtos;

        return switch (sort) {
            case VIEWS -> dtos.stream()
                    .sorted(Comparator.comparing(EventShortDto::getRating).reversed())
                    .toList();
            case EVENT_DATE -> dtos.stream()
                    .sorted(Comparator.comparing(EventShortDto::getEventDate))
                    .toList();
            default -> dtos;
        };
    }

    @Override
    public EventFullDto eventById(Long eventId, String ip, Long userId) {
        Event event = eventRepository.findByIdNew(eventId)
                .filter(ev -> ev.getState() == EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие c id " + eventId + " не найдено"));

        UserActionProto action = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(ActionTypeProto.ACTION_VIEW)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(Instant.now().getEpochSecond())
                        .setNanos(Instant.now().getNano())
                        .build())
                .build();

        collectorClient.sendUserAction(action);

        return buildFullDto(event);
    }

    @Override
    public Stream<RecommendedEventProto> getRecommendations(Long userId, int maxResults) {
        log.info("Получение рекомендаций для пользователя userId={}, maxResults={}", userId, maxResults);

        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        return analyzerClient.getRecommendationsForUser(request);
    }

    @Override
    @Transactional
    public void likeEvent(Long userId, Long eventId) {
        log.info("Лайк события eventId={} от пользователя userId={}", eventId, userId);

        Event event = getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ValidationException("Нельзя лайкнуть неопубликованное событие");
        }

        if (!hasUserVisitedEvent(userId, eventId)) {
            throw new BadRequestException("Нельзя лайкнуть непосещенное мероприятие");
        }

        UserActionProto action = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(ActionTypeProto.ACTION_LIKE)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(Instant.now().getEpochSecond())
                        .setNanos(Instant.now().getNano())
                        .build())
                .build();

        collectorClient.sendUserAction(action);
    }

    private void validateEventDate(LocalDateTime eventDate, EventState currentState) {
        LocalDateTime now = LocalDateTime.now();

        int minHours;
        if (currentState == EventState.PUBLISHED) {
            minHours = 1;
        } else {
            minHours = 2;
        }

        LocalDateTime minValidDate = now.plusHours(minHours);
        if (eventDate.isBefore(minValidDate)) {
            String message;
            if (minHours == 1) {
                message = "Дата начала изменяемого события должна быть не ранее чем за час от даты публикации";
            } else {
                message = "Дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента";
            }
            throw new ValidationException(message);
        }
    }
    private void validateRangeStartAndEnd(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd))
            throw new BadRequestException("Дата начала не может быть позже даты окончания");
    }

    private void validateDateEvent(LocalDateTime eventDate, long minHoursBeforeStartEvent) {
        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(minHoursBeforeStartEvent)))
            throw new ValidationException(
                    "Дата начала события не может быть ранее чем через " + minHoursBeforeStartEvent + " часа(ов)");
    }

    private EventFullDto buildFullDto(Event event) {
        Map<Long, Long> confirmedRequests = getConfirmedRequests(List.of(event.getId()));
        Double rating = getEventRating(event.getId());

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
        dto.setRating(rating);

        UserShortDto initiatorDto = getUserShortDto(event.getInitiatorId());
        dto.setInitiator(initiatorDto);

        return dto;
    }

    private Map<Long, Long> getConfirmedRequests(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();

        Map<Long, Long> result = new HashMap<>();

        for (Long eventId : eventIds) {
            try {
                Long count = requestClient.countByStatus(eventId, RequestStatus.CONFIRMED);
                result.put(eventId, count != null ? count : 0L);
                log.debug("Получено {} подтвержденных запросов для события {}", count, eventId);
            } catch (Exception e) {
                log.warn("Не удалось получить подтвержденные запросы для события {}: {}",
                        eventId, e.getMessage());
                result.put(eventId, 0L);
            }
        }

        return result;
    }

    private UserShortDto getUserShortDto(Long userId) {
        try {
            return userClient.getUser(userId);
        } catch (Exception e) {
            log.warn("Сервис пользователей недоступен, используем заглушку. userId={}", userId);
            return new UserShortDto(userId, "Пользователь-" + userId);
        }
    }

    private void checkUserExists(Long userId) {
        try {
            userClient.getUser(userId);
        } catch (Exception e) {
            log.warn("Сервис пользователей недоступен, пропускаем проверку. userId={}", userId);
        }
    }

    @Override
    public Event getEventOrThrow(Long eventId) {
        return eventRepository.findByIdNew(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c id " + eventId + " не найдено"));
    }

    private Double getEventRating(Long eventId) {
        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addEventId(eventId)
                    .build();

            return analyzerClient.getInteractionsCount(request)
                    .findFirst()
                    .map(RecommendedEventProto::getScore)
                    .orElse(0.0);
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинг для события {}", eventId, e);
            return 0.0;
        }
    }

    private Map<Long, Double> getRatingsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();

        Map<Long, Double> ratings = eventIds.stream()
                .collect(Collectors.toMap(id -> id, id -> 0.0));

        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addAllEventId(eventIds)
                    .build();

            analyzerClient.getInteractionsCount(request)
                    .forEach(proto -> ratings.put(proto.getEventId(), proto.getScore()));
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинги для событий", e);
        }

        return ratings;
    }

    private boolean hasUserVisitedEvent(Long userId, Long eventId) {
        try {
            return requestClient.hasUserConfirmedRequest(userId, eventId);
        } catch (Exception e) {
            log.warn("Не удалось проверить посещение события", e);
            return false;
        }
    }
}
