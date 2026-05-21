package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.UserAction;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);

    Set<Long> findEventIdsByUserId(Long userId);

    List<UserAction> findByEventIdIn(Set<Long> eventIds);

    @Query("SELECT a.eventId FROM UserAction a WHERE a.userId = :userId ORDER BY a.timestamp DESC")
    List<Long> findRecentEventIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT a FROM UserAction a WHERE a.userId = :userId")
    List<UserAction> findAllByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT a.eventId, SUM(a.userScore)
        FROM UserAction a
        WHERE a.eventId IN :eventIds
        GROUP BY a.eventId
        """)
    List<Object[]> sumScoresByEventIds(@Param("eventIds") List<Long> eventIds);
}
