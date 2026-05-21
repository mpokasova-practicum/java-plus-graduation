package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.service.UserService;

@Slf4j
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public UserShortDto getUser(@PathVariable("userId") Long userId) {
        log.debug("Internal: getUser userId={}", userId);
        return userService.getUserShortInfo(userId);
    }
}
