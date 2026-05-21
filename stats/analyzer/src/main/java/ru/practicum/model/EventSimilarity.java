package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_similarities", schema = "stats_analyzer")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class EventSimilarity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_a")
    private Long eventA;

    @Column(name = "event_b")
    private Long eventB;

    private Double score;
}
