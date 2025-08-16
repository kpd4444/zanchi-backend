package com.zanchi.zanchi_backend.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeNameRequest(
        @NotBlank @Size(min = 1, max = 30)
        String name
) {}