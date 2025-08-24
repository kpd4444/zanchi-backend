package com.zanchi.zanchi_backend.domain.clip.dto;

import java.time.LocalDateTime;

public record ClipSummary(
        Long id,
        Long authorId,
        String authorName,
        String authorAvatarUrl,
        String videoUrl,
        String caption,
        Long likeCount,
        Long commentCount,
        LocalDateTime createdAt
) {}
