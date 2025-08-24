package com.zanchi.zanchi_backend.domain.ranking.dto;

public interface ClipRankView {
    Long getClipId();
    Long getUploaderId();
    String getUploaderName();
    String getUploaderAvatarUrl();
    String getVideoUrl();
    Long getLikeCount();
    String getCaption();
}