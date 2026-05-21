package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {


    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    UserDto toUserDto(User user);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    User toUser(UserDto userDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    User toNewUser(NewUserRequest newUserRequestDto);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    UserShortDto toUserShortDto(User user);
}
