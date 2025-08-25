package com.zanchi.zanchi_backend.reco; // 패키지는 프로젝트 규칙에 맞게 조정

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PersonalizeItemMapRepository extends JpaRepository<PersonalizeItemMap, String> {
    List<PersonalizeItemMap> findByItemIdIn(Collection<String> itemIds);
}
