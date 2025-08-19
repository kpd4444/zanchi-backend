package com.zanchi.zanchi_backend.domain.clip.dto;

import com.zanchi.zanchi_backend.domain.clip.ClipComment;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ClipCommentRes(
        Long id,
        Long authorId,
        String authorName,
        String authorAvatarUrl,
        String content,
        LocalDateTime createdAt,
        Long parentId
) {
    public static ClipCommentRes of(ClipComment c) {
        var author = c.getAuthor();
        Long    aid   = (author != null ? author.getId()        : null);
        String  aname = (author != null
                ? (author.getName() != null ? author.getName() : author.getLoginId())
                : "익명");
        String  aava  = (author != null ? author.getAvatarUrl() : null);

        return ClipCommentRes.builder()
                .id(c.getId())
                .authorId(aid)
                .authorName(aname)
                .authorAvatarUrl(aava)
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .build();
    }
}
