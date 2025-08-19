package com.zanchi.zanchi_backend.domain.clip.repository;

import com.zanchi.zanchi_backend.domain.clip.ClipLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClipLikeRepository extends JpaRepository<ClipLike,Long> {

    boolean existsByClipIdAndMemberId(Long clipId, Long memberId);

    void deleteByClipIdAndMemberId(Long clipId, Long memberId);

    long countByClipId(Long clipId);

    @Query(
            value = """
        select l
        from ClipLike l
        join fetch l.clip c
        where l.member.id = :memberId
        order by l.id desc
      """,
            countQuery = """
        select count(l)
        from ClipLike l
        where l.member.id = :memberId
      """
    )
    Page<ClipLike> findByMemberId(Long memberId, Pageable pageable);
}
