package com.zanchi.zanchi_backend.domain.clip.repository;

import com.zanchi.zanchi_backend.domain.clip.ClipSave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClipSaveRepository extends JpaRepository<ClipSave, Long> {
    boolean existsByMember_IdAndClip_Id(Long memberId, Long clipId);
    void deleteByMember_IdAndClip_Id(Long memberId, Long clipId);
    Page<ClipSave> findByMember_IdOrderByIdDesc(Long memberId, Pageable pageable);

    // ★ 추가: 현재 페이지의 clipIds만 IN 조회
    @Query("""
        select s.clip.id
        from ClipSave s
        where s.member.id = :memberId
          and s.clip.id in :clipIds
    """)
    List<Long> findSavedClipIds(@Param("memberId") Long memberId,
                                @Param("clipIds") List<Long> clipIds);
}
