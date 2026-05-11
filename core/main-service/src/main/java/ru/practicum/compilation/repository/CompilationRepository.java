package ru.practicum.compilation.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.events.model.EventState;

import java.util.List;
import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    List<Compilation> findByPinned(Boolean pinned, Pageable pageable);

    boolean existsByTitle(String title);

    @Query("SELECT c FROM Compilation c LEFT JOIN FETCH c.events WHERE c.id = :id")
    Optional<Compilation> findByIdWithEvents(@Param("id") Long id);

    @Query("SELECT c FROM Compilation c JOIN c.events e WHERE c.id = :compId AND e.state = :eventState")
    Optional<Compilation> findByIdWithPublishedEvents(@Param("compId") Long compId,
                                                      @Param("eventState") EventState eventState);
}
