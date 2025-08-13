package com.zanchi.zanchi_backend.domain.clip.tag.repository;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.tag.ClipTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClipTagRepository extends JpaRepository<ClipTag, Long> {
    List<ClipTag> findByClip(Clip clip);
    void deleteByClip(Clip clip);
}
