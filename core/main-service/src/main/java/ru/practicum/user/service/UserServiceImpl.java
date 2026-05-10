package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserRequest;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public List<UserDto> getUsers(UserRequest request) {

        Pageable pageable = PageRequest.of(request.getFrom(), request.getSize());

        Page<User> userPage = (request.getIds() == null || request.getIds().isEmpty())
                ? userRepository.findAll(pageable)
                : userRepository.findByIdIn(request.getIds(), pageable);

        return userPage.map(UserMapper::toUserDto).getContent();
    }

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest newUserRequest) {
        userRepository.findByEmail(newUserRequest.getEmail()).ifPresent(user -> {
            throw new ConflictException("Пользователь с таким email уже существует");
        });
        User user = UserMapper.toNewUser(newUserRequest);
        User savedUser = userRepository.save(user);
        return UserMapper.toUserDto(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с Id " + userId + " не найден"));

        userRepository.delete(user);
    }
}
