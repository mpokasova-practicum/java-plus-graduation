package ru.practicum.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.user.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional findByEmail(String email);

    Page<User> findByIdIn(List<Long> ids, Pageable pageable);

    @Modifying
    @Query("DELETE FROM User u WHERE u.id = :id")
    int deleteByIdAndReturnCount(@Param("id") Long id);

    @Query("SELECT u.id FROM User u WHERE u.id IN :userIds")
    List<Long> findExistingUserIds(@Param("userIds") List<Long> userIds);
}
