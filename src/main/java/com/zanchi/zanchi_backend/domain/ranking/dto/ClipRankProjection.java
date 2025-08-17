package com.zanchi.zanchi_backend.domain.ranking.dto;

public interface ClipRankProjection {
    Long getClipId();
    Long getUploaderId();
    String getUploaderName();
    Long getLikeCount();
}