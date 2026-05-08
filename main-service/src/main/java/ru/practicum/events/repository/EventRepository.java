package ru.practicum.events.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {
    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    Page<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    boolean existsByCategory_Id(Long catId);

    @Query("""
        SELECT e FROM Event e
        WHERE (COALESCE(:users) IS NULL OR e.initiator.id IN (:users))
        AND (COALESCE(:states) IS NULL OR e.state IN (:states))
        AND (COALESCE(:categories) IS NULL OR e.category.id IN (:categories))
        AND (CAST(:rangeStart AS localdatetime) IS NULL OR e.eventDate >= :rangeStart)
        AND (CAST(:rangeEnd AS localdatetime) IS NULL OR e.eventDate <= :rangeEnd)
        """)
    List<Event> findEventsByAdminFilters(
            @Param("users") List<Long> users,
            @Param("states") List<EventState> states,
            @Param("categories") List<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);
}


