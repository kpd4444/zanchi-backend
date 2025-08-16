package com.zanchi.zanchi_backend.domain.show;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShowRepository extends JpaRepository<Show, Integer> {
    Page<Show> findAllByOrderByDateDesc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Show s where s.id = :id")
    Optional<Show> findByIdForUpdate(@Param("id") Integer id);
}