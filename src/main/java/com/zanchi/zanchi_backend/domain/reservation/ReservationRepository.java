package com.zanchi.zanchi_backend.domain.reservation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    Page<Reservation> findByMemberIdOrderByCreatedAtDesc(Integer memberId, Pageable pageable);
    Optional<Reservation> findByIdAndMemberId(Integer id, Integer memberId);
}