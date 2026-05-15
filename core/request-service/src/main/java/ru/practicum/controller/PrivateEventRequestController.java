package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.RequestService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
public class PrivateEventRequestController {
    private final RequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> getUserEventRequests(@PathVariable Long userId,
                                                              @PathVariable Long eventId) {
        return requestService.getUserEventRequests(userId, eventId);
    }

    @PatchMapping
    public EventRequestStatusUpdateResult updateUserEventRequests(@PathVariable Long userId,
                                                                  @PathVariable Long eventId,
                                                                  @Valid @RequestBody EventRequestStatusUpdateRequest dto) {
        return requestService.updateUserEventRequests(userId, eventId, dto);
    }
}
