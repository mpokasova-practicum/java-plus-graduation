package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserRequest;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public List<UserDto> getUsers(UserRequest request) {

        Pageable pageable = PageRequest.of(request.getFrom(), request.getSize());

        Page<User> userPage = (request.getIds() == null || request.getIds().isEmpty())
                ? userRepository.findAll(pageable)
                : userRepository.findByIdIn(request.getIds(), pageable);

        return userPage.map(userMapper::toUserDto).getContent();
    }

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest newUserRequest) {
        userRepository.findByEmail(newUserRequest.getEmail()).ifPresent(user -> {
            throw new ConflictException("Пользователь с таким email уже существует");
        });
        User user = userMapper.toNewUser(newUserRequest);
        User savedUser = userRepository.save(user);
        return userMapper.toUserDto(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с Id " + userId + " не найден"));

        userRepository.delete(user);
    }

    @Override
    public UserShortDto getUserShortInfo(Long userId) {
        User user = getUserEntityById(userId);
        return userMapper.toUserShortDto(user);
    }

    private User getUserEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }
}
