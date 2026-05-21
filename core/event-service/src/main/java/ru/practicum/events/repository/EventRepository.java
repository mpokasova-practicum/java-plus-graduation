package ru.practicum.events.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.events.model.Event;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    List<Event> findAllByInitiatorIdOrderByCreatedOnDesc(Long initiatorId, Pageable pageable);

    Event findFirstByOrderByCreatedOnAsc();

    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.category WHERE e.id IN :eventIds")
    List<Event> findAllByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.category WHERE e.id = :eventId")
    Optional<Event> findByIdNew(@Param("eventId") Long eventId);

    boolean existsByCategoryId(@Param("categoryId") Long categoryId);
}


