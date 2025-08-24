package com.zanchi.zanchi_backend.domain.clip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClipCreateReq {
    @NotBlank
    private String fileKey;   // "clips/<UUID>.mp4"
    private String caption;   // optional
    private Long showId;      // optional
}
