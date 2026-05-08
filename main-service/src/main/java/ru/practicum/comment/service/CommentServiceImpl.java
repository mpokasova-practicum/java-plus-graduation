package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.AdminUpdateCommentStatusDto;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.AdminUpdateCommentStatusAction;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.EventState;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;

    @Transactional
    @Override
    public CommentDto createComment(long authorId, long eventId, NewCommentDto newCommentDto) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с ID %s не найден", authorId)));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с ID %s не найдено", eventId)));
        if (authorId == event.getInitiator().getId()) {
            throw new ConflictException("Инициатор мероприятия не может оставлять комментарии к нему");
        }
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Мероприятие должно быть опубликовано");
        }
        if (requestRepository.findByRequesterIdAndEventIdAndStatus(authorId, eventId, RequestStatus.CONFIRMED).isEmpty()) {
            throw new ConflictException("Комментарии может оставлять только подтвержденный участник мероприятия");
        }
        Comment comment = CommentMapper.toComment(newCommentDto, author, event);
        commentRepository.save(comment);
        return CommentMapper.toDto(comment);
    }

    @Transactional
    @Override
    public CommentDto updateComment(long authorId, long commentId, NewCommentDto updateCommentDto) {
        Comment commentToUpdate = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий ID %s не найден", commentId)));
        if (authorId != commentToUpdate.getAuthor().getId()) {
            throw new ConflictException("Изменить комментарий может только его автор");
        }
        commentToUpdate.setText(updateCommentDto.getText());
        commentToUpdate.setStatus(CommentStatus.PENDING);

        commentRepository.save(commentToUpdate);
        return CommentMapper.toDto(commentToUpdate);
    }

    @Transactional
    @Override
    public void deleteComment(long authorId, long commentId) {
        Comment commentToDelete = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий ID %s не найден", commentId)));
        if (authorId != commentToDelete.getAuthor().getId()) {
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
        return CommentMapper.toDto(commentToUpdateStatus);
    }

    @Override
    public List<CommentDto> adminPendigCommentList(List<Long> usersId) {
        if (usersId != null && !usersId.isEmpty()) {
            return commentRepository.findByAuthor_IdInAndStatus(usersId, CommentStatus.PENDING)
                    .stream()
                    .map(CommentMapper::toDto)
                    .toList();
        }
        return commentRepository.findAllByStatus(CommentStatus.PENDING)
                .stream()
                .map(CommentMapper::toDto)
                .toList();
    }
}
