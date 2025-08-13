package com.zanchi.zanchi_backend.domain.clip.repository;

import com.zanchi.zanchi_backend.domain.clip.ClipLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ClipLikeRepository extends JpaRepository<ClipLike,Long> {
    //boolean existsByClipIdAndMemberId(Long clipId, Long memberId);
    //Optional<ClipLike> findByClipIdAndMemberId(Long clipId, Long memberId);
    //long countByClipId(Long clipId);

    boolean existsByClipIdAndMemberId(Long clipId, Long memberId);
    void deleteByClipIdAndMemberId(Long clipId, Long memberId);
    long countByClipId(Long clipId);
    @Query("""
      select l from ClipLike l
      join fetch l.clip c
      where l.member.id = :memberId
      order by l.id desc
    """)
    Page<ClipLike> findByMemberIdOrderByIdDesc(Long memberId, Pageable pageable);
}
