package com.zanchi.zanchi_backend.domain.ranking.dto;

public interface ClipRankView {
    Long getClipId();
    Long getUploaderId();
    String getUploaderName();
    String getUploaderAvatarUrl(); // ← 추가
    String getVideoUrl();          // ← 추가
    Long getLikeCount();
}