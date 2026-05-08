package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.EventState;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.RequestConflictException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    public List<ParticipationRequestDto> getUserEventRequests(Long userId, Long eventId) {
        if (eventId == null || userId == null) {
            throw new ValidationException("Id должен быть указан");
        }
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(
                () -> new NotFoundException("Событие или пользователь с данным id не найдены, или событие недоступно к просмотру данным пользователем")
        );
        List<Request> requests = requestRepository.findAllByEventId(eventId);
        return requests.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateUserEventRequests(Long userId, Long eventId, EventRequestStatusUpdateRequest dto) {
        if (eventId == null || userId == null) {
            throw new ValidationException("Id должен быть указан");
        }

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(
                () -> new NotFoundException("Событие или пользователь с данным id не найдены, или событие недоступно к просмотру данным пользователем")
        );

        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            log.info("Если для события лимит заявок равен 0 или отключена пре-модерация заявок, то подтверждение заявок не требуется");
            return new EventRequestStatusUpdateResult(List.of(), List.of());
        }

        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new RequestConflictException("Уже достигнут лимит по заявкам на данное событие");
        }

        List<Request> requests = requestRepository.findAllByIdIn(dto.getRequestIds());

        if (!requests.stream()
                .map(Request::getStatus)
                .allMatch(RequestStatus.PENDING::equals)) {
            throw new RequestConflictException("Статус можно изменить только у заявок, находящихся в состоянии ожидания");
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        if (dto.getStatus().equals(RequestStatus.REJECTED)) {
            requests.forEach(request -> request.setStatus(RequestStatus.REJECTED));
            List<Request> updatedRequests = requestRepository.saveAll(requests);
            rejected = updatedRequests.stream()
                    .map(RequestMapper::toParticipationRequestDto)
                    .toList();
            return new EventRequestStatusUpdateResult(confirmed, rejected);
        }

        int limit = event.getParticipantLimit() == 0 ? Integer.MAX_VALUE : event.getParticipantLimit();
        int availableSlots = limit - confirmedRequests.intValue();

        for (int i = 0; i < requests.size(); i++) {
            Request request = requests.get(i);
            if (i < availableSlots) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(RequestMapper.toParticipationRequestDto(request));
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(RequestMapper.toParticipationRequestDto(request));
            }
        }

        List<Request> updatedRequests = requestRepository.saveAll(requests);

        confirmed.clear();
        rejected.clear();
        for (Request request : updatedRequests) {
            ParticipationRequestDto dtoResult = RequestMapper.toParticipationRequestDto(request);
            if (request.getStatus() == RequestStatus.CONFIRMED) {
                confirmed.add(dtoResult);
            } else if (request.getStatus() == RequestStatus.REJECTED) {
                rejected.add(dtoResult);
            }
        }

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByRequester(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с данным id не найден")
        );
        List<Request> requests = requestRepository.findAllByRequesterId(userId);
        return requests.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException("Событие с данным id не найдено")
        );
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с данным id не найден")
        );

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new RequestConflictException("Запрос на участие в данном событии уже создан");
        }

        if (userId.equals(event.getInitiator().getId())) {
            throw new RequestConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new RequestConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (event.getParticipantLimit() > 0) {
            Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

            if (confirmedRequests >= event.getParticipantLimit()) {
                throw new RequestConflictException("Уже достигнут лимит по заявкам на данное событие");
            }
        }

        Request request = new Request();
        request.setEvent(event);
        request.setRequester(user);
        request.setCreatedOn(LocalDateTime.now());

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        request = requestRepository.save(request);
        return RequestMapper.toParticipationRequestDto(request);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос не найден или недоступен данному пользователю"));
        request.setStatus(RequestStatus.CANCELED);
        request = requestRepository.save(request);
        return RequestMapper.toParticipationRequestDto(request);
    }
}
