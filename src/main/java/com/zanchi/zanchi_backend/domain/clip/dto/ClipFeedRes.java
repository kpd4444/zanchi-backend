package com.zanchi.zanchi_backend.domain.clip.dto;


import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.member.Member;

import java.time.LocalDateTime;

public record ClipFeedRes(
        Long id,
        String videoUrl,
        String caption,
        long viewCount,
        long likeCount,
        long commentCount,
        LocalDateTime createdAt,
        UploaderSummary uploader
) {
    public static ClipFeedRes of(Clip c){
        Member m = c.getUploader();
        return new ClipFeedRes(
                c.getId(),
                c.getVideoUrl(),
                c.getCaption(),
                c.getViewCount(),
                c.getLikeCount(),
                c.getCommentCount(),
                c.getCreatedAt(),
                m == null
                        ? new UploaderSummary(null, null, null)
                        : new UploaderSummary(
                        m.getId(),
                        m.getName(),     // Member의 name
                        m.getLoginId()   // Member의 loginId
                )
        );
    }

    public record UploaderSummary(Long id, String name, String loginId) {}
}
