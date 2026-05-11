package ru.practicum.events.service;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatsClient;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;
import ru.practicum.events.dto.*;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.mapper.LocationMapper;
import ru.practicum.events.model.*;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.events.repository.LocationRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.EventConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.dto.ConfirmedRequests;
import ru.practicum.request.model.QRequest;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final RequestRepository requestRepository;
    private final StatsClient client;
    private final CommentRepository commentRepository;

    @Override
    public List<EventShortDto> getEventsByOwner(Long userId, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable).toList();
        return buildEvents(events);
    }

    @Override
    @Transactional
    public EventDto addEvent(Long userId, EventCreateDto eventCreateDto) {
        log.info("Валидация даты и времени события");
        validateEventDate(eventCreateDto.getEventDate(), EventState.PENDING);

        Event event = EventMapper.toEvent(eventCreateDto);

        log.info("Добавление инициатора события");
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с данным id не найден")
        );
        event.setInitiator(user);

        log.info("Добавление категории события");
        Category category = categoryRepository.findById(eventCreateDto.getCategory()).orElseThrow(
                () -> new NotFoundException("Категория с данным id не найдена")
        );
        event.setCategory(category);

        log.info("Добавление локации события");
        Location location = getOrSaveLocation(eventCreateDto.getLocation());
        event.setLocation(location);

        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        event = eventRepository.save(event);
        EventDto res = EventMapper.toEventDto(event);
        res.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));

        res.setComments(new ArrayList<>());

        return res;
    }

    @Override
    public EventDto getEventByOwner(Long userId, Long eventId) {
        if (eventId == null || userId == null) {
            throw new ValidationException("Id должен быть указан");
        }
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(
                () -> new NotFoundException("Событие или пользователь с данным id не найдены, или событие недоступно к просмотру данным пользователем")
        );
        EventDto res = EventMapper.toEventDto(event);

        res.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));

        List<CommentDto> comments = commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED).stream()
                .map(CommentMapper::toDto)
                .toList();
        res.setComments(comments);

        return res;
    }

    @Override
    @Transactional
    public EventDto updateEvent(Long userId, Long eventId, EventUpdateDto eventUpdateDto) {
        if (eventId == null || userId == null) {
            throw new ValidationException("Id должен быть указан");
        }
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(
                () -> new NotFoundException("Событие или пользователь с данным id не найдены, или событие недоступно к редактированию данным пользователем")
        );
        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new EventConflictException("Изменить можно только отмененные события или события в состоянии ожидания модерации");
        }
        if (eventUpdateDto.getEventDate() != null) {
            log.info("Валидация новых даты и времени события");
            validateEventDate(eventUpdateDto.getEventDate(), event.getState());
            log.info("Обновление даты и времени события");
            event.setEventDate(eventUpdateDto.getEventDate());
        }
        if (eventUpdateDto.getTitle() != null) {
            log.info("Обновление заголовка события");
            event.setTitle(eventUpdateDto.getTitle());
        }
        if (eventUpdateDto.getAnnotation() != null) {
            log.info("Обновление аннотации события");
            event.setAnnotation(eventUpdateDto.getAnnotation());
        }
        if (eventUpdateDto.getDescription() != null) {
            log.info("Обновление описания события");
            event.setDescription(eventUpdateDto.getDescription());
        }
        if (eventUpdateDto.getPaid() != null) {
            log.info("Обновление флага платности события");
            event.setPaid(eventUpdateDto.getPaid());
        }
        if (eventUpdateDto.getRequestModeration() != null) {
            log.info("Обновление признака премодерации заявок на участие");
            event.setRequestModeration(eventUpdateDto.getRequestModeration());
        }
        if (eventUpdateDto.getParticipantLimit() != null) {
            log.info("Обновление лимита пользователей собятия");
            event.setParticipantLimit(eventUpdateDto.getParticipantLimit());
        }

        if (eventUpdateDto.getCategory() != null) {
            log.info("Обновление категории события");
            Category category = categoryRepository.findById(eventUpdateDto.getCategory()).orElseThrow(
                    () -> new NotFoundException("Категория с данным id не найдена")
            );
            event.setCategory(category);
        }
        if (eventUpdateDto.getLocation() != null) {
            log.info("Обновление локации события");
            Location location = getOrSaveLocation(eventUpdateDto.getLocation());
            event.setLocation(location);
        }
        if (eventUpdateDto.getStateAction() != null) {
            switch (eventUpdateDto.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }
        event = eventRepository.save(event);
        EventDto res = EventMapper.toEventDto(event);
        res.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));

        List<CommentDto> comments = commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED).stream()
                .map(CommentMapper::toDto)
                .toList();
        res.setComments(comments);

        return res;
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

    private Location getOrSaveLocation(LocationDto dto) {
        Location location = LocationMapper.toLocation(dto);
        return locationRepository.findFirstByLatAndLon(location.getLat(), location.getLon()).orElseGet(
                () -> locationRepository.save(location)
        );
    }


    @Override
    public List<EventShortDto> allEvents(EntityParam params, String ip) {

        if (params.getRangeStart() != null && params.getRangeEnd() != null
                && params.getRangeStart().isAfter(params.getRangeEnd())) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }

        BooleanExpression expression = prepareAndBuildQuery(params);
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());
        List<Event> events = eventRepository.findAll(expression, pageable).getContent();

        List<EventShortDto> shortDtos = buildEvents(events);


        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("main-service")
                .ip(ip)
                .uri("/events")
                .timestamp(LocalDateTime.now())
                .build();
        client.hit(hitDto);

        if (params.getSort() == EventSort.EVENT_DATE) {
            return shortDtos.stream().sorted(Comparator.comparing(EventShortDto::getEventDate)).toList().reversed();
        }

        return shortDtos.stream().sorted(Comparator.comparing(EventShortDto::getViews)).toList().reversed();
    }

    @Override
    public EventDto eventById(Long eventId, String ip) {
        Event event = checkEvent(eventId);
        Integer countOfConfirmedInt = requestRepository.findAllByStatusAndEvent_Id(RequestStatus.CONFIRMED, eventId).size();
        Long countOfConfirmed = countOfConfirmedInt.longValue();
        Long countOfViews = getViews(eventId);

        List<CommentDto> comments = commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED).stream()
                .map(CommentMapper::toDto)
                .toList();

        EventDto eventDto = EventMapper.mapToDto(event, countOfConfirmed, countOfViews);
        eventDto.setComments(comments);

        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("main-service")
                .ip(ip)
                .uri("/events/" + eventId)
                .timestamp(LocalDateTime.now())
                .build();
        client.hit(hitDto);

        return eventDto;
    }

    private Event checkEvent(Long eventI) {
        Event event = eventRepository.findById(eventI)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventI + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventI + " не опубликовано");
        }

        return event;
    }

    private Long getViews(Long eventId) {
        LocalDateTime start = LocalDateTime.now().minusYears(2);
        LocalDateTime end = LocalDateTime.now();
        List<String> uri = List.of("/events/" + eventId);

        List<ViewStats> statResponse = client.getStats(start, end, uri, true).getBody();
        if (statResponse != null && !statResponse.isEmpty()) {
            return statResponse.getFirst().getHits();
        }

        return 0L;
    }

    private BooleanExpression prepareAndBuildQuery(EntityParam param) {
        QEvent event = QEvent.event;
        BooleanExpression predicate = event.state.eq(EventState.PUBLISHED);

        if (param.getText() != null && !param.getText().isEmpty()) {
            predicate = predicate.and(event.annotation.likeIgnoreCase("%" + param.getText() + "%")
                    .or(event.description.likeIgnoreCase("%" + param.getText() + "%")));
        }

        if (param.getCategories() != null && !param.getCategories().isEmpty()) {
            predicate = predicate.and(event.category.id.in(param.getCategories()));
        }

        if (param.getPaid() != null) {
            predicate = predicate.and(event.paid.eq(param.getPaid()));
        }

        if (param.getRangeStart() != null && param.getRangeEnd() != null) {
            predicate = predicate.and(event.eventDate.between(param.getRangeStart(), param.getRangeEnd()));
        } else if (param.getRangeStart() != null) {
            predicate = predicate.and(event.eventDate.goe(param.getRangeStart()));
        } else if (param.getRangeEnd() != null) {
            predicate = predicate.and(event.eventDate.loe(param.getRangeEnd()));
        } else {
            predicate = predicate.and(event.eventDate.after(LocalDateTime.now()));
        }

        if (param.getOnlyAvailable() != null && param.getOnlyAvailable()) {
            QRequest request = QRequest.request;

            Expression<Integer> confirmedRequestsCount = JPAExpressions
                    .select(request.count().intValue())
                    .from(request)
                    .where(request.event.id.eq(event.id)
                            .and(request.status.eq(RequestStatus.CONFIRMED)));

            predicate = predicate.and(event.participantLimit.gt(confirmedRequestsCount)
                    .or(event.participantLimit.eq(0)));
        }

        return predicate;
    }

    private List<EventShortDto> buildEvents(List<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedRequests = requestRepository
                .countConfirmedRequestsByEvents(RequestStatus.CONFIRMED, eventIds)
                .stream()
                .collect(Collectors.toMap(
                        ConfirmedRequests::getEvent,
                        ConfirmedRequests::getCount,
                        (count1, count2) -> count1 + count2
                ));


        Map<Long, Long> views = getViewsForEvents(eventIds);
        Map<Long, List<CommentDto>> comments = getCommentsForEvents(eventIds);


        return events.stream().map(event -> {
            Long confirmedR = confirmedRequests.get(event.getId());
            Long view = views.get(event.getId());
            return EventMapper.mapToShortDto(event, confirmedR, view,
                    comments.getOrDefault(event.getId(), Collections.emptyList()).size());
        }).toList();
    }

    private Map<Long, Long> getViewsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        LocalDateTime start = LocalDateTime.now().minusYears(2);
        LocalDateTime end = LocalDateTime.now();

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .toList();

        List<ViewStats> statResponse = client.getStats(start, end, uris, true).getBody();
        Map<Long, Long> viewsMap = new HashMap<>();

        if (statResponse != null) {
            for (ViewStats stats : statResponse) {
                String uri = stats.getUri();
                Long eventId = Long.parseLong(uri.substring(uri.lastIndexOf('/') + 1));
                viewsMap.put(eventId, stats.getHits());
            }
        }

        return viewsMap;
    }

    @Override
    public List<EventDto> getEvents(List<Long> users, List<String> states, List<Long> categories,
                                    LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {
        log.info("Поиск событий с параметрами: users={}, states={}, categories={}, rangeStart={}, rangeEnd={}, from={}, size={}",
                users, states, categories, rangeStart, rangeEnd, from, size);

        validateSearchParameters(users, states, rangeStart, rangeEnd);

        List<EventState> eventStates = parseEventStates(states);

        List<Long> usersParam = (users != null && users.isEmpty()) ? null : users;
        List<EventState> statesParam = (eventStates != null && eventStates.isEmpty()) ? null : eventStates;
        List<Long> categoriesParam = (categories != null && categories.isEmpty()) ? null : categories;

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        List<Event> events = eventRepository.findEventsByAdminFilters(
                usersParam, statesParam, categoriesParam, rangeStart, rangeEnd, pageable);

        log.info("Найдено {} событий", events.size());

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedRequests = requestRepository
                .countConfirmedRequestsByEvents(RequestStatus.CONFIRMED, eventIds)
                .stream()
                .collect(Collectors.toMap(
                        ConfirmedRequests::getEvent,
                        ConfirmedRequests::getCount,
                        (count1, count2) -> count1 + count2  // ДОБАВЬТЕ ЭТО
                ));
        Map<Long, Long> views = getViewsForEvents(eventIds);

        Map<Long, List<CommentDto>> commentsMap = getCommentsForEvents(eventIds);

        return events.stream().map(event -> {
            Long confirmedR = confirmedRequests.get(event.getId());
            Long view = views.get(event.getId());
            EventDto dto = EventMapper.mapToDto(event, confirmedR, view);

            List<CommentDto> comments = commentsMap.get(event.getId());
            dto.setComments(comments != null ? comments : Collections.emptyList());

            return dto;
        }).collect(Collectors.toList());
    }

    private Map<Long, List<CommentDto>> getCommentsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        List<Comment> comments = commentRepository.findByEventIdInAndStatus(
                eventIds, CommentStatus.PUBLISHED);

        return comments.stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getEvent().getId(),
                        Collectors.mapping(CommentMapper::toDto, Collectors.toList())
                ));
    }

    @Override
    @Transactional
    public EventDto updateEvent(Long eventId, EventAdminUpdateDto updateRequest) {
        log.info("Обновление события с id = {} администратором: {}", eventId, updateRequest);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

        validateEventForAdminUpdate(event, updateRequest);

        updateEventFields(event, updateRequest);

        if (updateRequest.getStateAction() != null) {
            processAdminStateAction(event, updateRequest.getStateAction());
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие с id = {} успешно обновлено администратором", eventId);

        EventDto result = EventMapper.toEventDto(updatedEvent);
        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        Long views = getViews(eventId);
        result.setConfirmedRequests(confirmedRequests);
        result.setViews(views);

        List<CommentDto> comments = commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED).stream()
                .map(CommentMapper::toDto)
                .toList();
        result.setComments(comments);

        return result;

    }

    private void validateSearchParameters(List<Long> users, List<String> states,
                                          LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }

        if (users != null && !users.isEmpty()) {
            List<Long> existingUsers = userRepository.findExistingUserIds(users);
            if (existingUsers.size() != users.size()) {
                throw new NotFoundException("Некоторые пользователи не найдены");
            }
        }
    }

    private List<EventState> parseEventStates(List<String> states) {
        if (states == null || states.isEmpty()) {
            return null;
        }

        return states.stream()
                .map(stateStr -> {
                    try {
                        return EventState.valueOf(stateStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ValidationException("Некорректное состояние события: " + stateStr);
                    }
                })
                .collect(Collectors.toList());
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            throw new ValidationException("Некорректный формат даты: " + dateTimeStr +
                    ". Ожидается формат: yyyy-MM-dd HH:mm:ss");
        }
    }

    private void validateEventForAdminUpdate(Event event, EventAdminUpdateDto updateRequest) {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime eventDateToCheck = updateRequest.getEventDate() != null
                ? updateRequest.getEventDate()
                : event.getEventDate();

        if (updateRequest.getStateAction() == AdminUpdateStateAction.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException(
                        "Событие можно публиковать только, если оно в состоянии ожидания публикации. " +
                                "Текущее состояние: " + event.getState()
                );
            }

            if (eventDateToCheck.isBefore(now.plusHours(1))) {
                throw new ValidationException("Дата начала события должна быть не ранее чем за час от даты публикации. " +
                        "Дата события: " + eventDateToCheck + ", Текущее время: " + now
                );
            }
        }

        if (updateRequest.getEventDate() != null) {
            if (event.getState() == EventState.PUBLISHED) {
                if (updateRequest.getEventDate().isBefore(now.plusHours(1))) {
                    throw new ValidationException("Дата начала изменяемого события должна быть не ранее чем за час от даты публикации"
                    );
                }
            } else {
                if (updateRequest.getEventDate().isBefore(now.plusHours(2))) {
                    throw new ValidationException("Дата и время события должны быть не ранее чем через 2 часа от текущего момента"
                    );
                }
            }
        }

        if (updateRequest.getStateAction() == AdminUpdateStateAction.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Событие можно отклонить только, если оно еще не опубликовано");
            }
        }
    }


    private void updateEventFields(Event event, EventAdminUpdateDto updateRequest) {
        if (updateRequest.getAnnotation() != null && !updateRequest.getAnnotation().isBlank()) {
            event.setAnnotation(updateRequest.getAnnotation());
        }

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id = " + updateRequest.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        if (updateRequest.getDescription() != null && !updateRequest.getDescription().isBlank()) {
            event.setDescription(updateRequest.getDescription());
        }

        if (updateRequest.getEventDate() != null) {
            event.setEventDate(updateRequest.getEventDate());
        }

        if (updateRequest.getLocation() != null) {
            Location location = new Location();
            location.setLat(updateRequest.getLocation().getLat());
            location.setLon(updateRequest.getLocation().getLon());
            event.setLocation(location);
        }

        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }

        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }

        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }

        if (updateRequest.getTitle() != null && !updateRequest.getTitle().isBlank()) {
            event.setTitle(updateRequest.getTitle());
        }
    }

    private void processAdminStateAction(Event event, AdminUpdateStateAction stateAction) {
        switch (stateAction) {
            case PUBLISH_EVENT:
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                log.info("Событие с id = {} опубликовано", event.getId());
                break;

            case REJECT_EVENT:
                event.setState(EventState.CANCELED);
                log.info("Событие с id = {} отклонено администратором", event.getId());
                break;

            default:
                throw new ValidationException("Некорректное действие: " + stateAction);
        }
    }
}
