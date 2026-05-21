package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserRequest;
import ru.practicum.service.UserService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/admin/users")
@Validated
public class UserAdminController {
    private final UserService userService;

    @GetMapping
    public List<UserDto> getUsers(@RequestParam(required = false) List<Long> ids,
                                  @RequestParam(defaultValue = "0") int from,
                                  @RequestParam(defaultValue = "10") int size
    ) {
        return userService.getUsers(new UserRequest(ids,from,size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody NewUserRequest newUserRequest) {
        return userService.createUser(newUserRequest);
    }

    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}
