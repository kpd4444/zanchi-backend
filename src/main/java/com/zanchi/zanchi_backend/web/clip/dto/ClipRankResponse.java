package com.zanchi.zanchi_backend.web.clip.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClipRankResponse {

    private Long clipId;
    private String uploaderName;
    private Long likeCount;

    // 생성자
    public ClipRankResponse(Long clipId, String uploaderName, Long likeCount) {
        this.clipId = clipId;
        this.uploaderName = uploaderName;
        this.likeCount = likeCount;
    }

    // Getter와 Setter
    public Long getClipId() {
        return clipId;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public Long getLikeCount() {
        return likeCount;
    }
}