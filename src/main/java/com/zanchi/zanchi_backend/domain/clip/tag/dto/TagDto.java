package com.zanchi.zanchi_backend.domain.clip.tag.dto;

import com.zanchi.zanchi_backend.domain.clip.tag.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class TagDto {
    private String name;
    private String normalizedName;
    private long usageCount;

    public static TagDto of(Tag tag, long usageCount) {
        return new TagDto(tag.getName(), tag.getNormalizedName(), usageCount);
    }
}
