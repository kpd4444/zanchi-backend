package com.zanchi.zanchi_backend.domain.clip.repository;

import com.zanchi.zanchi_backend.domain.clip.ClipComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface ClipCommentRepository extends JpaRepository<ClipComment, Long> {

    // 댓글 목록: 작성자(author)까지 함께 로딩
    @EntityGraph(attributePaths = {"author"})
    Page<ClipComment> findByClipIdOrderByIdAsc(Long clipId, Pageable pageable);

    long countByClipId(Long clipId);

    // (선택) 상위 댓글만: 작성자 함께 로딩
    @EntityGraph(attributePaths = {"author"})
    Page<ClipComment> findByClipIdAndParentIsNullOrderByIdAsc(Long clipId, Pageable pageable);

    // 대댓글 목록: 작성자(author)까지 함께 로딩
    @EntityGraph(attributePaths = {"author"})
    Page<ClipComment> findByParentIdOrderByIdAsc(Long parentId, Pageable pageable);

    // (필요 시) 상위 댓글들을 한 번에 로딩할 때 사용
    @EntityGraph(attributePaths = {"author"})
    List<ClipComment> findByClipIdAndParentIsNull(Long clipId);
}
