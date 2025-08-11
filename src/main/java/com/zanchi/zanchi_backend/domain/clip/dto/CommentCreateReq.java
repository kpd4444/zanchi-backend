package com.zanchi.zanchi_backend.domain.clip.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateReq(Long parentId,@NotBlank String content) {}
