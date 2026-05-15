package ru.practicum.service;

import ru.practicum.dto.comment.AdminUpdateCommentStatusDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;

import java.util.List;

public interface CommentService {
    CommentDto createComment(long userId, long eventId, NewCommentDto newCommentDto);

    CommentDto updateComment(long userId, long commentId, NewCommentDto updateCommentDto);

    void deleteComment(long userId, long commentId);

    void adminDeleteComment(long commentId);

    CommentDto adminUpdateCommentStatus(Long commentId, AdminUpdateCommentStatusDto dto);

    List<CommentDto> adminPendigCommentList(List<Long> usersId);
}
