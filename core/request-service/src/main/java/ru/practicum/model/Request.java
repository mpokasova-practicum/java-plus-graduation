package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import ru.practicum.enums.RequestStatus;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
@Entity
@Table(name = "requests")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdOn;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    public void confirmed() {
        this.status = RequestStatus.CONFIRMED;
    }

    public void rejected() {
        this.status = RequestStatus.REJECTED;
    }

    public void canceled() {
        this.status = RequestStatus.CANCELED;
    }
}
