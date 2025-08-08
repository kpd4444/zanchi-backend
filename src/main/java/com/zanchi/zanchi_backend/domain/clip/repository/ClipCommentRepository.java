package com.zanchi.zanchi_backend.domain.clip.repository;

import com.zanchi.zanchi_backend.domain.clip.ClipComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClipCommentRepository extends JpaRepository<ClipComment,Long> {
    Page<ClipComment> findByClipIdOrderByIdAsc(Long clipId, Pageable pageable);
    long countByClipId(Long clipId);
}
