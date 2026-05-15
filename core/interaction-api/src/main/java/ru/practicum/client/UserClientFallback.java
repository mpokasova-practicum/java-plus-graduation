package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.user.UserShortDto;

@Component
@Slf4j
public class UserClientFallback implements UserClient {

    @Override
    public UserShortDto getUser(Long userId) {
        log.warn("Сервис пользователей недоступен");
        return new UserShortDto(userId, "Пользователь-" + userId);
    }
}
