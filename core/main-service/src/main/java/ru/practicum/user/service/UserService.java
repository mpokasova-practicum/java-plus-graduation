package ru.practicum.user.service;

import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserRequest;

import java.util.List;

public interface UserService {
    List<UserDto> getUsers(UserRequest request);

    UserDto createUser(NewUserRequest newUserRequest);

    void deleteUser(Long userId);
}
