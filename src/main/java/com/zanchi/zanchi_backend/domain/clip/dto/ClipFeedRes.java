package com.zanchi.zanchi_backend.domain.clip.dto;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ClipFeedRes(
        Long id,
        String videoUrl,
        String caption,
        long likeCount,
        long commentCount,
        long viewCount,
        String authorName,
        Long uploaderId,
        LocalDateTime createdAt
) {
    public static ClipFeedRes of(Clip c) {

        String author = c.getUploader() != null
                ? (c.getUploader().getName() != null ? c.getUploader().getName() : c.getUploader().getLoginId())
                : "작성자";
        return ClipFeedRes.builder()
                .id(c.getId())
                .videoUrl(c.getVideoUrl())
                .caption(c.getCaption())

                .likeCount(c.getLikes() != null ? c.getLikes().size() : 0)
                .commentCount(c.getComments() != null ? c.getComments().size() : 0)
                .viewCount(c.getViewCount())
                .authorName(author)
                .uploaderId(c.getUploader() != null ? c.getUploader().getId() : null)
                .createdAt(c.getCreatedAt())
                .build();
    }
}