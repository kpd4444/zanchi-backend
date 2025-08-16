package com.zanchi.zanchi_backend.domain.clip.repository;

import com.zanchi.zanchi_backend.domain.clip.ClipMention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClipMentionRepository extends JpaRepository<ClipMention, Long> {
    List<ClipMention> findByClipId(Long clipId);
    void deleteByClipId(Long clipId);
}