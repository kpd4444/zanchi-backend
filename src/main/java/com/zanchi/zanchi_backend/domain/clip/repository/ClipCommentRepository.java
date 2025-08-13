package com.zanchi.zanchi_backend.domain.clip.repository;

import com.zanchi.zanchi_backend.domain.clip.ClipComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClipCommentRepository extends JpaRepository<ClipComment,Long> {
    Page<ClipComment> findByClipIdOrderByIdAsc(Long clipId, Pageable pageable);
    long countByClipId(Long clipId);

    List<ClipComment> findByClipIdAndParentIsNull(Long clipId);

    Page<ClipComment> findByParentIdOrderByIdAsc(Long parentId, Pageable pageable);




    Page<ClipComment> findByClipIdAndParentIsNullOrderByIdAsc(Long clipId, Pageable pageable);
}
