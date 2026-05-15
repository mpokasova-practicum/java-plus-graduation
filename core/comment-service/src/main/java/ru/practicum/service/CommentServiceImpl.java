package ru.practicum.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.comment.AdminUpdateCommentStatusDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.enums.AdminUpdateCommentStatusAction;
import ru.practicum.enums.CommentStatus;
import ru.practicum.enums.EventState;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.repository.CommentRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final EventClient eventClient;
    private final UserClient userClient;
    private final CommentMapper commentMapper;

    @Transactional
    @Override
    public CommentDto createComment(long authorId, long eventId, NewCommentDto newCommentDto) {
        checkUserExists(authorId);

        EventFullDto event = eventClient.getEvent(eventId);

        if (authorId == event.getInitiator().getId()) {
            throw new ConflictException("Инициатор мероприятия не может оставлять комментарии к нему");
        }
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Мероприятие должно быть опубликовано");
        }
        Comment comment = commentMapper.toComment(newCommentDto, authorId, eventId);
        commentRepository.save(comment);
        return commentMapper.toDto(comment);
    }

    @Transactional
    @Override
    public CommentDto updateComment(long authorId, long commentId, NewCommentDto updateCommentDto) {
        Comment commentToUpdate = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий ID %s не найден", commentId)));
        if (authorId != commentToUpdate.getUserId()) {
            throw new ConflictException("Изменить комментарий может только его автор");
        }
        if (commentToUpdate.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Можно редактировать только комментарии в статусе PENDING");
        }

        commentToUpdate.setText(updateCommentDto.getText());
        commentToUpdate.setStatus(CommentStatus.PENDING);

        commentRepository.save(commentToUpdate);
        return commentMapper.toDto(commentToUpdate);
    }

    @Transactional
    @Override
    public void deleteComment(long authorId, long commentId) {
        Comment commentToDelete = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий ID %s не найден", commentId)));
        if (authorId != commentToDelete.getUserId()) {
            throw new ConflictException("Удалить комментарий может только его автор");
        }
        commentRepository.delete(commentToDelete);
    }

    @Transactional
    @Override
    public void adminDeleteComment(long commentId) {
        Comment commentToDelete = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий ID %s не найден", commentId)));
        commentRepository.delete(commentToDelete);
    }

    @Transactional
    @Override
    public CommentDto adminUpdateCommentStatus(Long commentId, AdminUpdateCommentStatusDto updateCommentStatusDto) {
        Comment commentToUpdateStatus = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий ID %s не найден", commentId)));
        if (!commentToUpdateStatus.getStatus().equals(CommentStatus.PENDING)) {
            throw new ConflictException("Невозможно отклонить комментарий");
        }
        if (updateCommentStatusDto.getAction().equals(AdminUpdateCommentStatusAction.PUBLISH_COMMENT)) {
            commentToUpdateStatus.setStatus(CommentStatus.PUBLISHED);
        }
        if (updateCommentStatusDto.getAction().equals(AdminUpdateCommentStatusAction.REJECT_COMMENT)) {
            commentToUpdateStatus.setStatus(CommentStatus.REJECTED);
        }
        commentRepository.save(commentToUpdateStatus);
        return commentMapper.toDto(commentToUpdateStatus);
    }

    @Override
    public List<CommentDto> adminPendigCommentList(List<Long> usersId) {
        if (usersId != null && !usersId.isEmpty()) {
            return commentRepository.findByUserIdInAndStatus(usersId, CommentStatus.PENDING)
                    .stream()
                    .map(commentMapper::toDto)
                    .toList();
        }
        return commentRepository.findAllByStatus(CommentStatus.PENDING)
                .stream()
                .map(commentMapper::toDto)
                .toList();
    }

    private void checkUserExists(Long userId) {
        try {
            userClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }
}
