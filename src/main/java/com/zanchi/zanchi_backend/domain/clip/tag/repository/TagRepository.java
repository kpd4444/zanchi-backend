package com.zanchi.zanchi_backend.domain.clip.tag.repository;

import com.zanchi.zanchi_backend.domain.clip.tag.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.*;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByNormalizedName(String normalizedName);

    @Query("""
      select t as tag, count(ct.id) as usageCount
      from Tag t
      left join com.zanchi.zanchi_backend.domain.clip.tag.ClipTag ct on ct.tag = t
      group by t
      order by count(ct.id) desc
    """)
    List<Object[]> findAllWithUsageCount();
}
