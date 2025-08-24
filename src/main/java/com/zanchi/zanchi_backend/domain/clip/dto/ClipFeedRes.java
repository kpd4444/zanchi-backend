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
        String uploaderAvatarUrl,
        Long uploaderId,
        LocalDateTime createdAt,
        boolean likedByMe                // ← 추가
) {
    // 기존 호출 호환용(비로그인 등): likedByMe=false
    public static ClipFeedRes of(Clip c) {
        return of(c, false);
    }

    // 로그인 사용자 기준 likedByMe 전달용
    public static ClipFeedRes of(Clip c, boolean likedByMe) {
        var uploader = c.getUploader();
        String author = uploader != null
                ? (uploader.getName() != null ? uploader.getName() : uploader.getLoginId())
                : "작성자";
        String avatar = uploader != null ? uploader.getAvatarUrl() : null;

        return ClipFeedRes.builder()
                .id(c.getId())
                .videoUrl(c.getVideoUrl())
                .caption(c.getCaption())
                // N+1 방지 겸 일관성: 집계필드 사용 권장
                .likeCount(c.getLikeCount())
                .commentCount(c.getCommentCount())
                .viewCount(c.getViewCount())
                .authorName(author)
                .uploaderAvatarUrl(avatar)
                .uploaderId(uploader != null ? uploader.getId() : null)
                .createdAt(c.getCreatedAt())
                .likedByMe(likedByMe)
                .build();
    }
}
