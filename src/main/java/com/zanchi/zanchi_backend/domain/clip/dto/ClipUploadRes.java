package com.zanchi.zanchi_backend.domain.clip.dto;

import com.zanchi.zanchi_backend.domain.clip.Clip;

public record ClipUploadRes(Long id, String videoUrl, String caption) {
    public static ClipUploadRes of(Clip c){ return new ClipUploadRes(c.getId(), c.getVideoUrl(), c.getCaption()); }
}