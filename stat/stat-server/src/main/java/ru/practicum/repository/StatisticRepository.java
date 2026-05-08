package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.ViewStats;
import ru.practicum.model.Statistic;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticRepository extends JpaRepository<Statistic, Long> {
    @Query("""
            SELECT new ru.practicum.dto.ViewStats(s.app, s.uri, COUNT(DISTINCT s.ip))
            FROM Statistic AS s
            WHERE s.requestDate BETWEEN :start AND :end
            AND s.uri IN :uris
            GROUP BY s.app, s.uri
            ORDER BY COUNT(DISTINCT s.ip) DESC
            """)
    List<ViewStats> findUniqueIpAndUrisIn(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("uris") List<String> uris);

    @Query("""
            SELECT new ru.practicum.dto.ViewStats(s.app, s.uri, COUNT(DISTINCT s.ip))
            FROM Statistic AS s
            WHERE s.requestDate BETWEEN :start AND :end
            GROUP BY s.app, s.uri
            ORDER BY COUNT(DISTINCT s.ip) DESC
            """)
    List<ViewStats> findUniqueIp(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Query("""
            SELECT new ru.practicum.dto.ViewStats(s.app, s.uri, COUNT(s.ip))
            FROM Statistic AS s
            WHERE s.requestDate BETWEEN :start AND :end
            AND s.uri IN :uris
            GROUP BY s.app, s.uri
            ORDER BY COUNT(s.ip) DESC
            """)
    List<ViewStats> findNotUniqueIpAndUrisIn(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("uris") List<String> uris);

    @Query("""
            SELECT new ru.practicum.dto.ViewStats(s.app, s.uri, COUNT(s.ip))
            FROM Statistic AS s
            WHERE s.requestDate BETWEEN :start AND :end
            GROUP BY s.app, s.uri
            ORDER BY COUNT(s.ip) DESC
            """)
    List<ViewStats> findNotUniqueIp(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);
}
