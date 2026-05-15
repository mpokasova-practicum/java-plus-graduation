package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.config.FeignRetryConfig;
import ru.practicum.dto.user.UserShortDto;

@FeignClient(
        name = "user-service",
        path = "/internal/users",
        configuration = FeignRetryConfig.class,
        fallback = UserClientFallback.class
)
public interface UserClient {

    @GetMapping("/{userId}")
    UserShortDto getUser(@PathVariable("userId") Long userId);
}
