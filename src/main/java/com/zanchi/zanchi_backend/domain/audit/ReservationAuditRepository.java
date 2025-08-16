package com.zanchi.zanchi_backend.domain.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationAuditRepository extends JpaRepository<ReservationAudit, Long> { }
