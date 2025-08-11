package com.zanchi.zanchi_backend.domain.clip.repository;

import com.zanchi.zanchi_backend.domain.clip.ClipLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClipLikeRepository extends JpaRepository<ClipLike,Long> {
    //boolean existsByClipIdAndMemberId(Long clipId, Long memberId);
    //Optional<ClipLike> findByClipIdAndMemberId(Long clipId, Long memberId);
    //long countByClipId(Long clipId);

    boolean existsByClipIdAndMemberId(Long clipId, Long memberId);
    void deleteByClipIdAndMemberId(Long clipId, Long memberId);
    long countByClipId(Long clipId);
}