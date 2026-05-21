package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.EventSimilarity;

import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    Optional<EventSimilarity> findByEventAAndEventB(Long eventA, Long eventB);

    @Query("SELECT s FROM EventSimilarity s WHERE s.eventA = :eventId OR s.eventB = :eventId")
    List<EventSimilarity> findAllByEventId(@Param("eventId") Long eventId);

    @Query("""
            SELECT s FROM EventSimilarity s 
            WHERE (s.eventA IN :recentEvents AND s.eventB NOT IN :interactedEvents)
               OR (s.eventB IN :recentEvents AND s.eventA NOT IN :interactedEvents)
            ORDER BY s.score DESC
            """)
    List<EventSimilarity> findNewSimilarEvents(
            @Param("recentEvents") List<Long> recentEvents,
            @Param("interactedEvents") List<Long> interactedEvents,
            Pageable pageable);

    @Query("""
            SELECT s FROM EventSimilarity s
            WHERE (s.eventA = :eventId OR s.eventB = :eventId)
            AND NOT EXISTS (
                SELECT 1 FROM UserAction ua
                WHERE ua.userId = :userId
                AND ua.eventId = CASE 
                    WHEN s.eventA = :eventId THEN s.eventB 
                    ELSE s.eventA 
                END
            )
            ORDER BY s.score DESC
            """)
    List<EventSimilarity> findSimilarEventsForUser(
            @Param("eventId") Long eventId,
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("""
            SELECT s FROM EventSimilarity s
            WHERE (s.eventA = :candidateId AND s.eventB IN :userEvents)
               OR (s.eventB = :candidateId AND s.eventA IN :userEvents)
            ORDER BY s.score DESC
            """)
    List<EventSimilarity> findNeighboursAmongUserEvents(
            @Param("candidateId") Long candidateId,
            @Param("userEvents") List<Long> userEvents,
            Pageable pageable);
}
