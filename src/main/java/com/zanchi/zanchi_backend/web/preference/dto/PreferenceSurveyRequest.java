package com.zanchi.zanchi_backend.web.preference.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class PreferenceSurveyRequest {

    @NotEmpty
    private List<Long> tagIds; // 선택된 태그 id 목록
}
