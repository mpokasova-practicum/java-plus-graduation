package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.model.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "status", expression = "java(ru.practicum.enums.CommentStatus.PENDING)")
    Comment toComment(NewCommentDto newCommentDto, Long userId, Long eventId);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "eventId", source = "eventId")
    CommentDto toDto(Comment comment);

}
