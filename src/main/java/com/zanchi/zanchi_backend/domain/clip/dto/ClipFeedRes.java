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
        boolean likedByMe,
        boolean savedByMe   // ← 추가
) {
    // 기존 호출 호환용(비로그인 등): likedByMe=false, savedByMe=false
    public static ClipFeedRes of(Clip c) {
        return of(c, false, false);
    }

    // 기존 코드 호환: savedByMe는 기본 false
    public static ClipFeedRes of(Clip c, boolean likedByMe) {
        return of(c, likedByMe, false);
    }

    // 신규: likedByMe + savedByMe를 함께 세팅
    public static ClipFeedRes of(Clip c, boolean likedByMe, boolean savedByMe) {
        var uploader = c.getUploader();
        String author = uploader != null
                ? (uploader.getName() != null ? uploader.getName() : uploader.getLoginId())
                : "작성자";
        String avatar = uploader != null ? uploader.getAvatarUrl() : null;

        return ClipFeedRes.builder()
                .id(c.getId())
                .videoUrl(c.getVideoUrl())
                .caption(c.getCaption())
                .likeCount(c.getLikeCount())
                .commentCount(c.getCommentCount())
                .viewCount(c.getViewCount())
                .authorName(author)
                .uploaderAvatarUrl(avatar)
                .uploaderId(uploader != null ? uploader.getId() : null)
                .createdAt(c.getCreatedAt())
                .likedByMe(likedByMe)
                .savedByMe(savedByMe)
                .build();
    }
}
