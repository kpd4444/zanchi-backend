package com.zanchi.zanchi_backend.domain.clip.repository;

import com.zanchi.zanchi_backend.domain.clip.ClipSave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClipSaveRepository extends JpaRepository<ClipSave, Long> {
    boolean existsByMember_IdAndClip_Id(Long memberId, Long clipId);
    void deleteByMember_IdAndClip_Id(Long memberId, Long clipId);
    Page<ClipSave> findByMember_IdOrderByIdDesc(Long memberId, Pageable pageable);
    void deleteByMemberIdAndClipId(Long memberId, Long clipId);
}
