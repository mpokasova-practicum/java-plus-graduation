package ru.practicum.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enums.EventState;
import ru.practicum.enums.RequestStatus;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.repository.RequestRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final UserClient userClient;
    private final EventClient eventClient;
    @Override
    public List<ParticipationRequestDto> getUserEventRequests(Long userId, Long eventId) {
        checkUserExists(userId);
        EventFullDto event = eventClient.getEvent(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ValidationException("Пользователь с id=" + userId + " не является создателем события");
        }

        log.info("Получение информации о запросах на участие в событии с id={}", eventId);
        List<Request> requests = requestRepository.findByEventId(eventId);
        return requestMapper.toDtoList(requests);
    }

    @Override
    public EventRequestStatusUpdateResult updateUserEventRequests(Long userId, Long eventId, EventRequestStatusUpdateRequest dto) {
        checkUserExists(userId);
        return updateUserEventRequestsInTransaction(userId, eventId, dto);
    }

    @Transactional
    public EventRequestStatusUpdateResult updateUserEventRequestsInTransaction(Long userId, Long eventId, EventRequestStatusUpdateRequest dto) {
        EventFullDto event = eventClient.getEvent(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ValidationException("Пользователь с id=" + userId + " не является создателем события");
        }

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            throw new ValidationException("Для данного события подтверждение заявок не требуется");
        }

        RequestStatus newStatus = dto.getStatus();
        if (newStatus == RequestStatus.PENDING) {
            throw new ValidationException("Устанавливать можно только статусы CONFIRMED или REJECTED");
        }

        List<Request> requestsForUpdate = requestRepository.findByIdIn(dto.getRequestIds());
        long currentConfirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        List<Request> pendingRequests = currentConfirmedCount + requestsForUpdate.size() >= event.getParticipantLimit()
                ? requestRepository.findByEventIdAndStatus(eventId, RequestStatus.PENDING)
                : List.of();

        validateAllRequestsExist(dto.getRequestIds(), requestsForUpdate);
        validateRequestsState(requestsForUpdate, eventId);

        long availableSlots = event.getParticipantLimit() - currentConfirmedCount;

        List<Request> confirmedRequests = new ArrayList<>();
        List<Request> rejectedRequests = new ArrayList<>();

        if (newStatus == RequestStatus.CONFIRMED) {
            if (availableSlots <= 0) {
                throw new ConflictException("Свободных мест больше нет");
            }

            int confirmedCount = 0;
            for (Request req : requestsForUpdate) {
                if (confirmedCount < availableSlots) {
                    req.confirmed();
                    confirmedRequests.add(req);
                    confirmedCount++;
                } else {
                    req.rejected();
                    rejectedRequests.add(req);
                }
            }

            if (!pendingRequests.isEmpty()) {
                for (Request pendingReq : pendingRequests) {
                    pendingReq.rejected();
                    rejectedRequests.add(pendingReq);
                }

                List<Request> allRequestsToSave = new ArrayList<>();
                allRequestsToSave.addAll(requestsForUpdate);
                allRequestsToSave.addAll(pendingRequests);
                requestRepository.saveAll(allRequestsToSave);

                log.info("Автоматически отклонено {} заявок из-за исчерпания лимита на событие с id={}",
                        pendingRequests.size(), eventId);
            } else {
                requestRepository.saveAll(requestsForUpdate);
            }
        } else {
            for (Request req : requestsForUpdate) {
                req.rejected();
                rejectedRequests.add(req);
            }
            requestRepository.saveAll(requestsForUpdate);
        }

        log.info("Обновление статусов заявок на участие в событии с id={}: подтверждено={}, отклонено={}",
                eventId, confirmedRequests.size(), rejectedRequests.size());

        return new EventRequestStatusUpdateResult(
                requestMapper.toDtoList(confirmedRequests),
                requestMapper.toDtoList(rejectedRequests)
        );
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByRequester(Long userId) {
        checkUserExists(userId);

        log.info("Получение информации о заявках на участие пользователя с id={}", userId);
        List<Request> requests = requestRepository.findByRequesterId(userId);
        return requestMapper.toDtoList(requests);
    }

    @Override
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        checkUserExists(userId);
        EventFullDto event = eventClient.getEvent(eventId);
        return addRequestInTransaction(userId, eventId, event);
    }

    @Transactional
    public ParticipationRequestDto addRequestInTransaction(Long userId, Long eventId, EventFullDto event) {
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Нельзя добавить повторный запрос");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (event.getParticipantLimit() != 0 &&
                requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)
                        >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит по количеству участников события с id=" + eventId);
        }

        Request request = Request.builder()
                .requesterId(userId)
                .eventId(eventId)
                .status(RequestStatus.PENDING)
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            log.debug("Модерация заявок на участие в событии с id={} не требуется", eventId);
            request.confirmed();
        }

        request = requestRepository.save(request);

        log.info("Добавление нового запроса на участие в событии с id={} от пользователя с id={}", eventId, userId);
        return requestMapper.toDto(request);
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        checkUserExists(userId);
        return cancelRequestInTransaction(userId, requestId);
    }

    @Transactional
    public ParticipationRequestDto cancelRequestInTransaction(Long userId, Long requestId) {
        Request request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));

        request.canceled();
        request = requestRepository.save(request);
        log.info("Отмена запроса на участие с id={} пользователя с id={}", requestId, userId);
        return requestMapper.toDto(request);
    }

    private void checkUserExists(Long userId) {
        try {
            userClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        } catch (FeignException e) {
            log.warn("Сервис пользователей недоступен, операция может быть выполнена некорректно");
        }
    }

    private void validateAllRequestsExist(List<Long> requestedIds, List<Request> foundRequests) {
        List<Long> foundIds = foundRequests.stream()
                .map(Request::getId)
                .toList();

        List<Long> missingIds = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new NotFoundException("Запрос(ы) с id=" + missingIds + " не найден(ы)");
        }
    }

    private void validateRequestsState(List<Request> requests, Long eventId) {
        for (Request req : requests) {
            if (req.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Можно изменять только запросы в статусе PENDING");
            }

            if (!req.getEventId().equals(eventId)) {
                throw new ConflictException("Запрос с id=" + req.getId() + " не относится к событию с id=" + eventId);
            }
        }
    }
}
