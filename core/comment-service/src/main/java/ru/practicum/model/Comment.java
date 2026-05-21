package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.enums.CommentStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(length = 1000)
    private String text;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    private LocalDateTime created = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private CommentStatus status = CommentStatus.PENDING;
}
