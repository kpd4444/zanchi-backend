package com.zanchi.zanchi_backend.domain.clip.repository;

import com.zanchi.zanchi_backend.domain.clip.ClipLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClipLikeRepository extends JpaRepository<ClipLike,Long> {
    boolean existsByClipIdAndMemberId(Long clipId, Long memberId);
    Optional<ClipLike> findByClipIdAndMemberId(Long clipId, Long memberId);
    long countByClipId(Long clipId);
}
