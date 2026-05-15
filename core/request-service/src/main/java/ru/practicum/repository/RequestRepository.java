package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.enums.RequestStatus;
import ru.practicum.model.Request;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {
    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    List<Request> findByRequesterId(Long requesterId);

    List<Request> findByEventId(Long eventId);

    Optional<Request> findByIdAndRequesterId(Long id, Long requesterId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    List<Request> findByEventIdAndStatus(Long eventId, RequestStatus status);

    List<Request> findByIdIn(List<Long> ids);

    @Query("SELECT r.eventId, " +
            "COUNT(r) " +
            "FROM Request r " +
            "WHERE r.eventId IN :eventIds AND r.status = 'CONFIRMED' GROUP BY r.eventId")
    List<Object[]> countConfirmedByEventIds(@Param("eventIds") List<Long> eventIds);
}
