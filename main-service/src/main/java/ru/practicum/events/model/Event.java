package ru.practicum.events.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.category.model.Category;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String annotation;
    private String description;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    private Boolean paid;

    @Column(name = "request_moderation")
    private Boolean requestModeration;

    @Column(name = "participant_limit")
    private int participantLimit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id")
    private User initiator;

    @Enumerated(EnumType.STRING)
    private EventState state;

    @Column(name = "published_at")
    private LocalDateTime publishedOn;

    @Column(name = "created_at")
    private LocalDateTime createdOn;
}
