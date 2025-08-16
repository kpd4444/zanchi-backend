package com.zanchi.zanchi_backend.domain.clip.tag.dto;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 태그 검색 결과용 클립 응답 DTO
 * 최소 필드만 사용(확실한 것만). 필요 시 nickname/thumbnail 등 나중에 추가.
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class ClipByTagRes {
    private Long id;
    private String caption;
    private Long uploaderId;

    public static ClipByTagRes of(Clip c) {
        Long uploaderId = (c.getUploader() != null ? c.getUploader().getId() : null);
        return new ClipByTagRes(c.getId(), c.getCaption(), uploaderId);
    }
}
