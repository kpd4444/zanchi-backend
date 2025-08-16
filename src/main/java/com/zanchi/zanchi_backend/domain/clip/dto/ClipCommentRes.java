package com.zanchi.zanchi_backend.domain.clip.dto;

import com.zanchi.zanchi_backend.domain.clip.ClipComment;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ClipCommentRes(
        Long id,
        Long clipId,
        Long parentId,
        String content,
        String authorName,
        LocalDateTime createdAt
) {
    public static ClipCommentRes of(ClipComment cc){
        String author = cc.getAuthor()!=null
                ? (cc.getAuthor().getName()!=null ? cc.getAuthor().getName() : cc.getAuthor().getLoginId())
                : "익명";
        return ClipCommentRes.builder()
                .id(cc.getId())
                .clipId(cc.getClip()!=null ? cc.getClip().getId() : null)
                .parentId(cc.getParent()!=null ? cc.getParent().getId() : null)
                .content(cc.getContent())
                .authorName(author)
                .createdAt(cc.getCreatedAt())
                .build();
    }
}