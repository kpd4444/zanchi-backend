package com.zanchi.zanchi_backend.domain.preference.repository;

import com.zanchi.zanchi_backend.domain.preference.PreferenceTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreferenceTagRepository extends JpaRepository<PreferenceTag, Long> {
    Optional<PreferenceTag> findByCode(String code);
    List<PreferenceTag> findAllByIdIn(List<Long> ids);
}
