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
        String uploaderAvatarUrl, // 작성자의 아바타 URL
        Long uploaderId,
        LocalDateTime createdAt
) {
    public static ClipFeedRes of(Clip c) {
        var uploader = c.getUploader();

        String author = uploader != null
                ? (uploader.getName() != null ? uploader.getName() : uploader.getLoginId())
                : "작성자";

        String avatar = uploader != null ? uploader.getAvatarUrl() : null;

        return ClipFeedRes.builder()
                .id(c.getId())
                .videoUrl(c.getVideoUrl())
                .caption(c.getCaption())
                .likeCount(c.getLikes() != null ? c.getLikes().size() : 0)
                .commentCount(c.getComments() != null ? c.getComments().size() : 0)
                .viewCount(c.getViewCount())
                .authorName(author)
                .uploaderAvatarUrl(avatar)  // 아바타 URL 넣어주기
                .uploaderId(uploader != null ? uploader.getId() : null)
                .createdAt(c.getCreatedAt())
                .build();
    }
}
