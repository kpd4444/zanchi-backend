package com.zanchi.zanchi_backend.domain.clip.repository;

import com.zanchi.zanchi_backend.domain.clip.ClipComment;
import com.zanchi.zanchi_backend.domain.clip.dto.ReplyCountItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClipCommentRepository extends JpaRepository<ClipComment, Long> {

    @EntityGraph(attributePaths = {"author"})
    Page<ClipComment> findByClipIdOrderByIdAsc(Long clipId, Pageable pageable);

    long countByClipId(Long clipId);

    @EntityGraph(attributePaths = {"author"})
    Page<ClipComment> findByClipIdAndParentIsNullOrderByIdAsc(Long clipId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<ClipComment> findByParentIdOrderByIdAsc(Long parentId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    List<ClipComment> findByClipIdAndParentIsNull(Long clipId);

    // 부모 댓글 ID 리스트에 대한 대댓글 수를 한 번에 가져오기
    @Query("""
           select c.parent.id as parentId, count(c.id) as cnt
           from ClipComment c
           where c.parent.id in :parentIds
           group by c.parent.id
           """)
    List<ReplyCountItem> countRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    // (보조) 단건 카운트가 필요할 때
    long countByParentId(Long parentId);
}
