package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.enums.CommentStatus;
import ru.practicum.model.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByEventIdAndStatus(Long eventId, CommentStatus commentStatus);

    List<Comment> findAllByStatus(CommentStatus commentStatus);

    List<Comment> findByEventIdInAndStatus(List<Long> eventIds, CommentStatus commentStatus);

    List<Comment> findByUserIdInAndStatus(List<Long> usersId, CommentStatus commentStatus);
}