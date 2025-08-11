package com.zanchi.zanchi_backend.domain.clip.dto;

import jakarta.validation.constraints.Size;

public record ClipUploadReq(@Size(max = 300) String caption) {}