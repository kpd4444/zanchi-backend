package com.zanchi.zanchi_backend.domain.clip.repository;

import com.zanchi.zanchi_backend.domain.clip.ClipCommentMention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClipCommentMentionRepository extends JpaRepository<ClipCommentMention, Long> {
    void deleteByCommentId(Long commentId);
    List<ClipCommentMention> findByCommentId(Long commentId);
}
