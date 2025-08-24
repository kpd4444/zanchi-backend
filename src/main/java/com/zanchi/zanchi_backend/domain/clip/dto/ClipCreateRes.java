package com.zanchi.zanchi_backend.domain.clip.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClipCreateRes {
    private Long id;
    private String fileKey;
    private String streamUrl; // S3 직링크(또는 CloudFront 경로)
}
