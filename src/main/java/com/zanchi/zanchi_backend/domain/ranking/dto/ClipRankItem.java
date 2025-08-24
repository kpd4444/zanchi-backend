// com.zanchi.zanchi_backend.domain.ranking.dto.ClipRankItem
package com.zanchi.zanchi_backend.domain.ranking.dto;

import java.time.LocalDateTime;

public interface ClipRankItem {
    Long getClipId();
    Long getUploaderId();
    String getUploaderName();
    Long getLikeCount();
    String getCaption();
    String getVideoUrl();
    LocalDateTime getCreatedAt();
}
