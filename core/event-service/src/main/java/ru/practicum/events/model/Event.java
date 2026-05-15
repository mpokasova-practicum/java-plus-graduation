package ru.practicum.events.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import ru.practicum.category.model.Category;
import ru.practicum.enums.EventState;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String annotation;

    @Column(nullable = false, length = 7000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "initiator_id")
    private Long initiatorId;

    @Column(name = "created_on", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdOn;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "lat")),
            @AttributeOverride(name = "lon", column = @Column(name = "lon"))
    })
    private Location location;

    @Column(nullable = false)
    private Boolean paid = false;

    @Column(name = "participant_limit", nullable = false)
    private Integer participantLimit;

    @Column(name = "request_moderation", nullable = false)
    private Boolean requestModeration;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EventState state;

    @Column(name = "confirmed_requests", nullable = false)
    @Builder.Default
    private Integer confirmedRequests = 0;

    @Column(name = "views", nullable = false)
    @Builder.Default
    private Long views = 0L;

    public void pending() {
        this.state = EventState.PENDING;
    }

    public void canceled() {
        this.state = EventState.CANCELED;
    }

    public void publish() {
        this.state = EventState.PUBLISHED;
        this.publishedOn = LocalDateTime.now();
    }
}
