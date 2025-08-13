package com.zanchi.zanchi_backend.domain.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClipReportRepository extends JpaRepository<ClipReport, Long> {
    boolean existsByClip_IdAndReporter_Id(Long clipId, Long reporterId);

    Page<ClipReport> findByClip_Id(Long clipId, Pageable pageable);
    Page<ClipReport> findByReporter_Id(Long reporterId, Pageable pageable);
}
